package com.sep.educonnect.service;

import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.entity.EmailVerificationToken;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.EmailVerificationTokenRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.email.MailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class EmailVerificationService {

    static final int TOKEN_EXPIRY_MINUTES = 120;
    static final int RESEND_COOLDOWN_MINUTES = 5;
    static final String VERIFY_EMAIL_PATH = "/verify-email?token=";
    static final String LOGIN_PATH = "/login";

    EmailVerificationTokenRepository emailVerificationTokenRepository;
    UserRepository userRepository;
    MailService mailService;
    I18nService i18nService;

    @Value("${app.frontend-url:https://educonnect.dev}")
    @NonFinal
    String frontendBaseUrl;

    @Transactional
    public void scheduleVerificationEmail(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.debug("User {} already verified, skip scheduling email", user.getEmail());
            return;
        }

        EmailVerificationToken token = findActiveToken(user)
                .orElseGet(() -> createNewToken(user));
        sendVerificationEmail(user, token);
    }

    @Transactional
    public void resendVerificationEmail(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        Optional<EmailVerificationToken> activeTokenOpt = findActiveToken(user);
        EmailVerificationToken token;

        if (activeTokenOpt.isPresent()) {
            token = activeTokenOpt.get();
            enforceCooldown(token);
        } else {
            token = createNewToken(user);
        }

        token.setLastSentAt(LocalDateTime.now());
        token.setResendCount(
                Optional.ofNullable(token.getResendCount()).orElse(0) + 1);
        emailVerificationTokenRepository.save(token);

        sendVerificationEmail(user, token);
    }

    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        if (Boolean.TRUE.equals(token.getIsUsed())) {
            throw new AppException(ErrorCode.VERIFICATION_TOKEN_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();

        if (token.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        User user = token.getUser();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            markTokenAsUsed(token, now);
            log.info("Verification link reused after completion for user {}", user.getEmail());
            return;
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        userRepository.save(user);

        markTokenAsUsed(token, now);

        sendWelcomeEmail(user);
        log.info("User {} verified email successfully", user.getEmail());
    }

    private void markTokenAsUsed(EmailVerificationToken token, LocalDateTime now) {
        token.setIsUsed(true);
        token.setUsedAt(now);
        emailVerificationTokenRepository.save(token);
    }

    private Optional<EmailVerificationToken> findActiveToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository
                .findActiveTokensByUserId(user.getUserId(), now);

        if (activeTokens.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(activeTokens.getFirst());
    }

    private EmailVerificationToken createNewToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(now.plusMinutes(TOKEN_EXPIRY_MINUTES))
                .lastSentAt(now)
                .build();

        return emailVerificationTokenRepository.save(token);
    }

    private void enforceCooldown(EmailVerificationToken token) {
        LocalDateTime now = LocalDateTime.now();
        if (token.getLastSentAt() != null
                && token.getLastSentAt().isAfter(now.minusMinutes(RESEND_COOLDOWN_MINUTES))) {
            throw new AppException(ErrorCode.VERIFICATION_TOO_SOON);
        }
    }

    private void sendVerificationEmail(User user, EmailVerificationToken token) {
        String verificationUrl = frontendBaseUrl + VERIFY_EMAIL_PATH + token.getToken();

        Email email = Email.builder()
                .subject(i18nService.msg("email.subject.verify.account"))
                .to(List.of(Mailer.builder().email(user.getEmail()).build()))
                .build();

        Map<String, Object> variables = Map.of(
                "userName", resolveDisplayName(user),
                "verificationUrl", verificationUrl,
                "expiryHours", TOKEN_EXPIRY_MINUTES / 60);

        mailService.send(email, TemplateMail.ACCOUNT_VERIFICATION, variables);
        log.info("Sent verification email to {}", user.getEmail());
    }

    private void sendWelcomeEmail(User user) {
        Email email = Email.builder()
                .subject(i18nService.msg("email.subject.welcome"))
                .to(List.of(Mailer.builder().email(user.getEmail()).build()))
                .build();

        Map<String, Object> variables = Map.of(
                "name", resolveDisplayName(user),
                "username", Optional.ofNullable(user.getEmail()).orElse(""),
                "password", "********",
                "web_login_url", frontendBaseUrl + LOGIN_PATH,
                "support_phone_number", "+84 000 000 000",
                "email_sign", "Đội ngũ EduConnect",
                "falo_info", "EduConnect");

        mailService.send(email, TemplateMail.ACCOUNT_CREATED, variables);
    }

    private String resolveDisplayName(User user) {
        if (user.getFirstName() != null || user.getLastName() != null) {
            return (Optional.ofNullable(user.getFirstName()).orElse("") + " "
                    + Optional.ofNullable(user.getLastName()).orElse("")).trim();
        }
        return Optional.ofNullable(user.getUsername()).orElse(user.getEmail());
    }
}

