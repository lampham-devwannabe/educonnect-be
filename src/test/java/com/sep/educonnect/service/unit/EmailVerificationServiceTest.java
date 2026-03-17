package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.entity.EmailVerificationToken;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.EmailVerificationTokenRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.EmailVerificationService;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.email.MailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock private UserRepository userRepository;

    @Mock private MailService mailService;

    @Mock private I18nService i18nService;

    @InjectMocks private EmailVerificationService emailVerificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user =
                User.builder()
                        .userId("user-1")
                        .email("student@example.com")
                        .username("student")
                        .emailVerified(false)
                        .build();

        ReflectionTestUtils.setField(
                emailVerificationService, "frontendBaseUrl", "http://localhost:5173");
        lenient().when(i18nService.msg(anyString())).thenReturn("subject");
    }

    @Test
    @DisplayName("scheduleVerificationEmail should create token and send mail when no active token")
    void should_scheduleVerificationEmail_whenNoActiveToken() {
        EmailVerificationToken savedToken =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now())
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenReturn(savedToken);

        emailVerificationService.scheduleVerificationEmail(user);

        verify(emailVerificationTokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(mailService).send(any(Email.class), eq(TemplateMail.ACCOUNT_VERIFICATION), anyMap());
    }

    @Test
    @DisplayName("scheduleVerificationEmail should reuse active token when exists")
    void should_reuseActiveToken_whenExists() {
        // Given
        EmailVerificationToken activeToken =
                EmailVerificationToken.builder()
                        .token("existing-token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .lastSentAt(LocalDateTime.now().minusMinutes(10))
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of(activeToken));

        // When
        emailVerificationService.scheduleVerificationEmail(user);

        // Then - Should not create new token, just use existing one
        verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
        verify(mailService).send(any(Email.class), eq(TemplateMail.ACCOUNT_VERIFICATION), anyMap());
    }

    @Test
    @DisplayName("scheduleVerificationEmail should send email with correct verification URL")
    void should_sendEmailWithCorrectVerificationUrl() {
        // Given
        EmailVerificationToken savedToken =
                EmailVerificationToken.builder()
                        .token("unique-token-123")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now())
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenReturn(savedToken);

        // When
        emailVerificationService.scheduleVerificationEmail(user);

        // Then
        verify(mailService)
                .send(
                        argThat(
                                email ->
                                        email.getTo()
                                                .get(0)
                                                .getEmail()
                                                .equals("student@example.com")),
                        eq(TemplateMail.ACCOUNT_VERIFICATION),
                        argThat(
                                variables -> {
                                    String url = (String) variables.get("verificationUrl");
                                    return url != null && url.contains("unique-token-123");
                                }));
    }

    @Test
    @DisplayName("scheduleVerificationEmail should handle multiple active tokens by using first")
    void should_useFirstToken_whenMultipleActiveTokensExist() {
        // Given
        EmailVerificationToken token1 =
                EmailVerificationToken.builder()
                        .token("token-1")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now().minusMinutes(10))
                        .build();

        EmailVerificationToken token2 =
                EmailVerificationToken.builder()
                        .token("token-2")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .lastSentAt(LocalDateTime.now().minusMinutes(5))
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of(token1, token2));

        // When
        emailVerificationService.scheduleVerificationEmail(user);

        // Then - Should use first token and not create new one
        verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
        verify(mailService)
                .send(
                        any(Email.class),
                        eq(TemplateMail.ACCOUNT_VERIFICATION),
                        argThat(
                                variables -> {
                                    String url = (String) variables.get("verificationUrl");
                                    return url != null && url.contains("token-1");
                                }));
    }

    @Test
    @DisplayName("scheduleVerificationEmail should send email to correct recipient")
    void should_sendEmailToCorrectRecipient() {
        // Given
        user.setEmail("test@example.com");

        EmailVerificationToken savedToken =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now())
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenReturn(savedToken);

        // When
        emailVerificationService.scheduleVerificationEmail(user);

        // Then
        verify(mailService)
                .send(
                        argThat(
                                email -> {
                                    List<Mailer> recipients = email.getTo();
                                    return recipients != null
                                            && recipients.size() == 1
                                            && "test@example.com"
                                                    .equals(recipients.get(0).getEmail());
                                }),
                        eq(TemplateMail.ACCOUNT_VERIFICATION),
                        anyMap());
    }

    @Test
    @DisplayName("scheduleVerificationEmail should use i18n for email subject")
    void should_useI18nForEmailSubject() {
        // Given
        when(i18nService.msg("email.subject.verify.account")).thenReturn("Verify Your Account");

        EmailVerificationToken savedToken =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now())
                        .build();

        when(emailVerificationTokenRepository.findActiveTokensByUserId(
                        eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenReturn(savedToken);

        // When
        emailVerificationService.scheduleVerificationEmail(user);

        // Then
        verify(i18nService).msg("email.subject.verify.account");
        verify(mailService)
                .send(
                        argThat(email -> "Verify Your Account".equals(email.getSubject())),
                        eq(TemplateMail.ACCOUNT_VERIFICATION),
                        anyMap());
    }

    @Test
    @DisplayName("verifyEmail should mark user verified and send welcome email")
    void should_verifyEmailSuccessfully() {
        EmailVerificationToken token =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .lastSentAt(LocalDateTime.now().minusMinutes(10))
                        .build();

        when(emailVerificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenReturn(token);

        emailVerificationService.verifyEmail("token");

        assertTrue(Boolean.TRUE.equals(user.getEmailVerified()));
        assertNotNull(user.getEmailVerifiedAt());
        verify(userRepository).save(user);
        verify(emailVerificationTokenRepository, atLeastOnce()).save(token);
        verify(mailService).send(any(Email.class), eq(TemplateMail.ACCOUNT_CREATED), anyMap());
    }

    @Test
    @DisplayName("verifyEmail should throw exception when token not found")
    void should_throwException_whenTokenNotFound() {
        when(emailVerificationTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> emailVerificationService.verifyEmail("invalid-token"));

        assertEquals(ErrorCode.VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
        verify(mailService, never()).send(any(), anyString(), anyMap());
    }

    @Test
    @DisplayName("verifyEmail should throw exception when token already used")
    void should_throwException_whenTokenAlreadyUsed() {
        EmailVerificationToken token =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusHours(2))
                        .isUsed(true)
                        .build();

        when(emailVerificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        AppException exception =
                assertThrows(
                        AppException.class, () -> emailVerificationService.verifyEmail("token"));

        assertEquals(ErrorCode.VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
        verify(mailService, never()).send(any(), anyString(), anyMap());
    }

    @Test
    @DisplayName("verifyEmail should throw exception when token expired")
    void should_throwException_whenTokenExpired() {
        EmailVerificationToken token =
                EmailVerificationToken.builder()
                        .token("token")
                        .user(user)
                        .expiresAt(LocalDateTime.now().minusHours(1))
                        .isUsed(false)
                        .build();

        when(emailVerificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        AppException exception =
                assertThrows(
                        AppException.class, () -> emailVerificationService.verifyEmail("token"));

        assertEquals(ErrorCode.VERIFICATION_TOKEN_EXPIRED, exception.getErrorCode());
        verify(mailService, never()).send(any(), anyString(), anyMap());
    }


}
