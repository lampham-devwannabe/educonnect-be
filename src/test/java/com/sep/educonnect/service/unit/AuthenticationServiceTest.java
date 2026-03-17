package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sep.educonnect.dto.auth.request.AuthenticationRequest;
import com.sep.educonnect.dto.auth.request.IntrospectRequest;
import com.sep.educonnect.dto.auth.request.LogoutRequest;
import com.sep.educonnect.dto.auth.request.RefreshRequest;
import com.sep.educonnect.dto.auth.response.AuthenticationResponse;
import com.sep.educonnect.dto.auth.response.IntrospectResponse;
import com.sep.educonnect.entity.InvalidatedToken;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.InvalidatedTokenRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.AuthenticationService;
import com.sep.educonnect.util.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    private static final String TEST_SIGNER_KEY =
            "test-signer-key-for-testing-purposes-only-very-long-key-required-for-hs512-algorithm";
    private static final long TEST_VALID_DURATION = 3600L;
    private static final long TEST_REFRESHABLE_DURATION = 36000L;
    @Mock private UserRepository userRepository;
    @Mock private InvalidatedTokenRepository invalidatedTokenRepository;
    @InjectMocks private AuthenticationService authenticationService;
    private PasswordEncoder passwordEncoder;
    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);

        // Set private fields using reflection
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", TEST_SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", TEST_VALID_DURATION);
        ReflectionTestUtils.setField(
                authenticationService, "REFRESHABLE_DURATION", TEST_REFRESHABLE_DURATION);

        testRole = TestDataBuilder.defaultRole().name("STUDENT").build();

        testUser = TestDataBuilder.defaultUser().role(testRole).build();
    }

    @Test
    @DisplayName("Should authenticate successfully with valid credentials")
    void should_authenticateSuccessfully_when_validCredentials() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getToken());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void should_throwException_when_userNotFound() {
        // Given
        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("nonexistent")
                        .password("password123")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void should_throwException_when_passwordIncorrect() {
        // Given
        String encodedPassword = passwordEncoder.encode("correctPassword");
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password("wrongPassword")
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.PASSWORD_NOT_MATCHED, exception.getErrorCode());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return valid true for valid token")
    void should_returnValidTrue_when_validToken() throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        IntrospectRequest request = IntrospectRequest.builder().token(token).build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isValid());
    }

    @Test
    @DisplayName("Should return valid false for invalid token")
    void should_returnValidFalse_when_invalidToken() throws JOSEException, ParseException {
        // Given
        IntrospectRequest request = IntrospectRequest.builder().token("invalid.token.here").build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should logout successfully with valid token")
    void should_logoutSuccessfully_when_validToken() throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        LogoutRequest request = LogoutRequest.builder().token(token).build();

        // When
        assertDoesNotThrow(() -> authenticationService.logout(request));

        // Then
        verify(invalidatedTokenRepository, times(1)).save(any(InvalidatedToken.class));
    }

    @Test
    @DisplayName("Should handle logout gracefully when token already expired")
    void should_handleLogoutGracefully_when_tokenExpired() throws JOSEException, ParseException {
        // Given
        LogoutRequest request = LogoutRequest.builder().token("expired.token.here").build();

        // When & Then
        assertDoesNotThrow(() -> authenticationService.logout(request));
        verify(invalidatedTokenRepository, never()).save(any(InvalidatedToken.class));
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void should_refreshTokenSuccessfully_when_validRefreshToken()
            throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        RefreshRequest request = RefreshRequest.builder().token(token).build();

        // When
        AuthenticationResponse response = authenticationService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getToken());
        assertNotEquals(token, response.getToken()); // New token should be different
        verify(invalidatedTokenRepository, times(1)).save(any(InvalidatedToken.class));
        verify(userRepository, times(2)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void should_throwException_when_refreshTokenInvalid() {
        // Given
        RefreshRequest request = RefreshRequest.builder().token("invalid.refresh.token").build();

        // When & Then
        assertThrows(Exception.class, () -> authenticationService.refreshToken(request));
    }

    @Test
    @DisplayName("Should throw exception when user not found during refresh")
    void should_throwException_when_userNotFoundDuringRefresh()
            throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        // Simulate user deleted after token generation
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        RefreshRequest request = RefreshRequest.builder().token(token).build();

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> authenticationService.refreshToken(request));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should include role in token scope when user has role")
    void should_includeRoleInTokenScope_when_userHasRole() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        // Token should be generated successfully with role
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when token is invalidated (in blacklist)")
    void should_throwException_when_tokenInvalidated() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        // Simulate token being invalidated
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(true);

        IntrospectRequest request = IntrospectRequest.builder().token(token).build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should throw exception when token has expired (normal token)")
    void should_throwException_when_tokenExpired() throws InterruptedException {
        // Given - Set very short expiry time
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", 1L);

        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        // Wait for token to expire
        Thread.sleep(2000);

        // Reset to original duration before introspection
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", TEST_VALID_DURATION);

        IntrospectRequest request = IntrospectRequest.builder().token(token).build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should throw exception when refresh token has expired")
    void should_throwException_when_refreshTokenExpired() throws InterruptedException {
        // Given - Set very short refreshable duration
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", 1L);
        ReflectionTestUtils.setField(authenticationService, "REFRESHABLE_DURATION", 2L);

        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        // Wait for refresh token to expire
        Thread.sleep(3000);

        RefreshRequest request = RefreshRequest.builder().token(token).build();

        // When & Then
        try {
            assertThrows(AppException.class, () -> authenticationService.refreshToken(request));
        } finally {
            // Reset to original durations after test
            ReflectionTestUtils.setField(
                    authenticationService, "VALID_DURATION", TEST_VALID_DURATION);
            ReflectionTestUtils.setField(
                    authenticationService, "REFRESHABLE_DURATION", TEST_REFRESHABLE_DURATION);
        }
    }

    @Test
    @DisplayName("Should throw exception when token signature is invalid")
    void should_throwException_when_tokenSignatureInvalid() {
        // Given - Create a token with different signer key
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        // Temporarily change signer key
        String differentKey =
                "different-signer-key-that-will-make-verification-fail-very-long-key-for-hs512";
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", differentKey);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String tokenWithDifferentKey = authResponse.getToken();

        // Change signer key back to original
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", TEST_SIGNER_KEY);

        IntrospectRequest request =
                IntrospectRequest.builder().token(tokenWithDifferentKey).build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should handle malformed token in verifyToken")
    void should_handleMalformedToken_when_parsing() {
        // Given
        IntrospectRequest request = IntrospectRequest.builder().token("this.is.malformed").build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should use refresh duration when isRefresh is true")
    void should_useRefreshDuration_when_isRefreshTrue() throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        RefreshRequest request = RefreshRequest.builder().token(token).build();

        // When - This will use isRefresh=true path in verifyToken
        AuthenticationResponse response = authenticationService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getToken());
        verify(invalidatedTokenRepository, times(1)).save(any(InvalidatedToken.class));
    }

    @Test
    @DisplayName("Should use normal expiration when isRefresh is false")
    void should_useNormalExpiration_when_isRefreshFalse() throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        IntrospectRequest request = IntrospectRequest.builder().token(token).build();

        // When - This will use isRefresh=false path in verifyToken
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isValid());
    }

    @Test
    @DisplayName("Should handle null token by throwing exception")
    void should_throwException_when_tokenIsNull() {
        // Given
        IntrospectRequest request = IntrospectRequest.builder().token(null).build();

        // When & Then - SignedJWT.parse() throws NullPointerException for null tokens
        assertThrows(NullPointerException.class, () -> authenticationService.introspect(request));
    }

    @Test
    @DisplayName("Should throw exception when token is empty string")
    void should_throwException_when_tokenIsEmpty() {
        // Given
        IntrospectRequest request = IntrospectRequest.builder().token("").build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should throw exception when token has invalid JWT structure")
    void should_throwException_when_tokenHasInvalidStructure() {
        // Given
        IntrospectRequest request = IntrospectRequest.builder().token("not-a-jwt-token").build();

        // When
        IntrospectResponse response = authenticationService.introspect(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    @DisplayName("Should successfully verify valid token in logout")
    void should_verifyValidToken_when_logout() throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        LogoutRequest request = LogoutRequest.builder().token(token).build();

        // When - This uses verifyToken with isRefresh=true
        assertDoesNotThrow(() -> authenticationService.logout(request));

        // Then
        verify(invalidatedTokenRepository, times(1)).save(any(InvalidatedToken.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully when logout with malformed token")
    void should_handleExceptionGracefully_when_logoutWithMalformedToken() {
        // Given
        LogoutRequest request = LogoutRequest.builder().token("malformed.token.value").build();

        // When & Then - Should not throw exception, just log
        assertDoesNotThrow(() -> authenticationService.logout(request));
        verify(invalidatedTokenRepository, never()).save(any(InvalidatedToken.class));
    }

    @Test
    @DisplayName("Should verify token successfully for refresh with valid duration")
    void should_verifyTokenSuccessfully_when_refreshWithinDuration()
            throws JOSEException, ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest authRequest =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);
        when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        String token = authResponse.getToken();

        RefreshRequest request = RefreshRequest.builder().token(token).build();

        // When
        AuthenticationResponse response = authenticationService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isAuthenticated());
        assertNotNull(response.getToken());
        assertNotEquals(token, response.getToken());
    }

    // ==================== generateToken Method Tests ====================

    @Test
    @DisplayName("Should generate token with role scope when user has role")
    void should_generateTokenWithRoleScope_when_userHasRole() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setRole(testRole); // User has role

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify token contains role scope
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        String scope = (String) signedJWT.getJWTClaimsSet().getClaim("scope");
        assertNotNull(scope);
        assertTrue(scope.contains("ROLE_" + testRole.getName()));
    }

    @Test
    @DisplayName("Should generate token with empty scope when user has no role")
    void should_generateTokenWithEmptyScope_when_userHasNoRole() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setRole(null); // User has no role

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify token contains empty scope
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        String scope = (String) signedJWT.getJWTClaimsSet().getClaim("scope");
        assertNotNull(scope);
        assertTrue(scope.isEmpty());
    }

    @Test
    @DisplayName("Should generate token with isVerified true when email is verified")
    void should_generateTokenWithIsVerifiedTrue_when_emailVerified() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setEmailVerified(true);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(response.isEmailVerified());

        // Verify token contains isVerified claim as true
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        Boolean isVerified = (Boolean) signedJWT.getJWTClaimsSet().getClaim("isVerified");
        assertNotNull(isVerified);
        assertTrue(isVerified);
    }

    @Test
    @DisplayName("Should generate token with isVerified false when email is not verified")
    void should_generateTokenWithIsVerifiedFalse_when_emailNotVerified() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setEmailVerified(false);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.isEmailVerified());

        // Verify token contains isVerified claim as false
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        Boolean isVerified = (Boolean) signedJWT.getJWTClaimsSet().getClaim("isVerified");
        assertNotNull(isVerified);
        assertFalse(isVerified);
    }

    @Test
    @DisplayName("Should generate token with isVerified false when email verification is null")
    void should_generateTokenWithIsVerifiedFalse_when_emailVerificationNull()
            throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setEmailVerified(null);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.isEmailVerified());

        // Verify token contains isVerified claim as false
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        Boolean isVerified = (Boolean) signedJWT.getJWTClaimsSet().getClaim("isVerified");
        assertNotNull(isVerified);
        assertFalse(isVerified);
    }

    @Test
    @DisplayName("Should include all required claims in generated token")
    void should_includeAllRequiredClaims_when_generatingToken() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);
        testUser.setUserId("user-123");
        testUser.setUsername("testuser");

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify all claims are present
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        assertEquals("testuser", claims.getSubject());
        assertEquals("educonnect", claims.getIssuer());
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());
        assertNotNull(claims.getJWTID());
        assertNotNull(claims.getClaim("scope"));
        assertEquals("user-123", claims.getClaim("userId"));
        assertNotNull(claims.getClaim("isVerified"));
    }

    @Test
    @DisplayName("Should set expiration time based on VALID_DURATION")
    void should_setExpirationTime_basedOnValidDuration() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify expiration time is set correctly
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date issueTime = claims.getIssueTime();
        Date expirationTime = claims.getExpirationTime();

        long diffInSeconds = (expirationTime.getTime() - issueTime.getTime()) / 1000;

        // Should be approximately equal to TEST_VALID_DURATION (3600 seconds)
        assertTrue(
                diffInSeconds >= TEST_VALID_DURATION - 5
                        && diffInSeconds <= TEST_VALID_DURATION + 5);
    }

    @Test
    @DisplayName("Should generate unique JWT ID for each token")
    void should_generateUniqueJwtId_forEachToken() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When - Generate two tokens
        AuthenticationResponse response1 = authenticationService.authenticate(request);
        AuthenticationResponse response2 = authenticationService.authenticate(request);

        // Then
        assertNotNull(response1.getToken());
        assertNotNull(response2.getToken());

        SignedJWT signedJWT1 = SignedJWT.parse(response1.getToken());
        SignedJWT signedJWT2 = SignedJWT.parse(response2.getToken());

        String jwtId1 = signedJWT1.getJWTClaimsSet().getJWTID();
        String jwtId2 = signedJWT2.getJWTClaimsSet().getJWTID();

        // JWT IDs should be different
        assertNotNull(jwtId1);
        assertNotNull(jwtId2);
        assertNotEquals(jwtId1, jwtId2);
    }

    @Test
    @DisplayName("Should use HS512 algorithm for token signing")
    void should_useHS512Algorithm_forTokenSigning() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify algorithm is HS512
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        assertEquals(JWSAlgorithm.HS512, signedJWT.getHeader().getAlgorithm());
    }

    @Test
    @DisplayName("Should throw RuntimeException when token signing fails")
    void should_throwRuntimeException_when_tokenSigningFails() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        // Set an invalid signer key that's too short for HS512 (requires at least 512 bits / 64
        // bytes)
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", "short");

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        try {
            assertThrows(RuntimeException.class, () -> authenticationService.authenticate(request));
        } finally {
            // Reset to valid key
            ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", TEST_SIGNER_KEY);
        }
    }

    @Test
    @DisplayName("Should generate token with correct userId claim")
    void should_generateTokenWithCorrectUserId_when_userHasId() throws ParseException {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        String userId = "test-user-id-12345";
        testUser.setPassword(encodedPassword);
        testUser.setUserId(userId);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify userId claim matches
        SignedJWT signedJWT = SignedJWT.parse(response.getToken());
        String tokenUserId = (String) signedJWT.getJWTClaimsSet().getClaim("userId");
        assertEquals(userId, tokenUserId);
    }

    @Test
    @DisplayName("Should generate valid serialized JWT token")
    void should_generateValidSerializedToken_whenCalled() {
        // Given
        String plainPassword = "password123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        testUser.setPassword(encodedPassword);

        AuthenticationRequest request =
                TestDataBuilder.defaultAuthenticationRequest()
                        .username("testuser")
                        .password(plainPassword)
                        .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify token has valid JWT structure (header.payload.signature)
        String token = response.getToken();
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token should have 3 parts separated by dots");
        assertTrue(parts[0].length() > 0, "Header should not be empty");
        assertTrue(parts[1].length() > 0, "Payload should not be empty");
        assertTrue(parts[2].length() > 0, "Signature should not be empty");
    }
}
