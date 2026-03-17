package com.sep.educonnect.service;

import com.sep.educonnect.dto.auth.request.ForgotPasswordRequest;
import com.sep.educonnect.dto.auth.request.ResetPasswordRequest;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.entity.PasswordResetToken;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.PasswordResetTokenRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.email.MailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetService {

    PasswordResetTokenRepository passwordResetTokenRepository;
    UserRepository userRepository;
    MailService mailService;
    PasswordEncoder passwordEncoder;
    I18nService i18nService;

    private static final int TOKEN_EXPIRY_MINUTES = 15;
    private static final String RESET_PASSWORD_URL = "https://educonnect.dev/reset-password?token=";

    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        String email = request.getEmail();
        LocalDateTime now = LocalDateTime.now();

        Optional<User> userOpt = userRepository.findByEmailAndNotDeleted(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findActiveTokensByUserId(user.getUserId(), now);

        if (!activeTokens.isEmpty()) {
            PasswordResetToken existingToken = activeTokens.getFirst();
            sendResetEmail(user, existingToken.getToken());
            log.info("Resent password reset email for user: {}", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plusMinutes(TOKEN_EXPIRY_MINUTES);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder().token(token).email(email).expiresAt(expiresAt).user(user).build();

        passwordResetTokenRepository.save(passwordResetToken);

        sendResetEmail(user, token);
        log.info("Password reset email sent for user: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        LocalDateTime now = LocalDateTime.now();

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByTokenAndIsUsedFalse(token);

        if (tokenOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        PasswordResetToken passwordResetToken = tokenOpt.get();

        if (passwordResetToken.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = passwordResetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        passwordResetToken.setIsUsed(true);
        passwordResetToken.setUsedAt(now);
        passwordResetTokenRepository.save(passwordResetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    private void sendResetEmail(User user, String token) {
        String resetUrl = RESET_PASSWORD_URL + token;

        Email email = Email.builder().subject(i18nService.msg("email.subject.reset.password")).to(List.of(Mailer.builder().email(user.getEmail()).build())).build();

        Map<String, Object> variables = Map.of("userName", user.getFirstName() + " " + user.getLastName(), "resetUrl", resetUrl, "expiryMinutes", TOKEN_EXPIRY_MINUTES);

        mailService.send(email, "password-reset", variables);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<PasswordResetToken> expiredTokens = passwordResetTokenRepository.findExpiredTokens(now);

        if (!expiredTokens.isEmpty()) {
            passwordResetTokenRepository.deleteAll(expiredTokens);
            log.info("Cleaned up {} expired password reset tokens", expiredTokens.size());
        }
    }
}