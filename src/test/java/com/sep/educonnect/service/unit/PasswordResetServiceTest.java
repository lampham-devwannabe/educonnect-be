package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.auth.request.ForgotPasswordRequest;
import com.sep.educonnect.dto.auth.request.ResetPasswordRequest;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.entity.PasswordResetToken;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.PasswordResetTokenRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.PasswordResetService;
import com.sep.educonnect.service.email.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user-1")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    @DisplayName("Should skip sending email when user not found")
    void should_skipWhenUserNotFound() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("missing@example.com")
                .build();
        when(userRepository.findByEmailAndNotDeleted("missing@example.com")).thenReturn(Optional.empty());

        // When
        passwordResetService.sendPasswordResetEmail(request);

        // Then
        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).send(any(Email.class), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should resend existing active token")
    void should_resendExistingToken() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        PasswordResetToken existingToken = PasswordResetToken.builder()
                .token("existing-token")
                .user(user)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(i18nService.msg(anyString())).thenReturn("Reset Password");
        when(userRepository.findByEmailAndNotDeleted("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findActiveTokensByUserId(eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of(existingToken));

        // When
        passwordResetService.sendPasswordResetEmail(request);

        // Then
        verify(mailService).send(any(Email.class), eq("password-reset"), anyMap());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create new token and send email")
    void should_createNewTokenAndSendEmail() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        when(i18nService.msg(anyString())).thenReturn("Reset Password");
        when(userRepository.findByEmailAndNotDeleted("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findActiveTokensByUserId(eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        passwordResetService.sendPasswordResetEmail(request);

        // Then
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).send(any(Email.class), eq("password-reset"), anyMap());
    }

    @Test
    @DisplayName("Should reset password when token valid")
    void should_resetPassword_when_tokenValid() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token-123")
                .newPassword("newpass")
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .token("token-123")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .user(user)
                .build();

        when(passwordResetTokenRepository.findByTokenAndIsUsedFalse("token-123"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");

        // When
        passwordResetService.resetPassword(request);

        // Then
        assertEquals("encoded", user.getPassword());
        assertTrue(token.getIsUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    @DisplayName("Should throw when token invalid")
    void should_throwWhenTokenInvalid() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid")
                .newPassword("pass")
                .build();

        when(passwordResetTokenRepository.findByTokenAndIsUsedFalse("invalid"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when token expired")
    void should_throwWhenTokenExpired() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token")
                .newPassword("pass")
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .token("token")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(user)
                .build();

        when(passwordResetTokenRepository.findByTokenAndIsUsedFalse("token"))
                .thenReturn(Optional.of(token));

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void should_cleanupExpiredTokens() {
        // Given
        PasswordResetToken token1 = PasswordResetToken.builder().token("1").build();
        PasswordResetToken token2 = PasswordResetToken.builder().token("2").build();
        when(passwordResetTokenRepository.findExpiredTokens(any(LocalDateTime.class)))
                .thenReturn(List.of(token1, token2));

        // When
        passwordResetService.cleanupExpiredTokens();

        // Then
        verify(passwordResetTokenRepository).deleteAll(List.of(token1, token2));
    }
}
