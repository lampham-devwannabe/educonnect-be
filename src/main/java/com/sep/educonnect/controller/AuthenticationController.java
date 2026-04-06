package com.sep.educonnect.controller;

import com.nimbusds.jose.JOSEException;
import com.sep.educonnect.dto.auth.request.*;
import com.sep.educonnect.dto.auth.response.AuthenticationResponse;
import com.sep.educonnect.dto.auth.response.IntrospectResponse;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.text.ParseException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
  AuthenticationService authenticationService;
  PasswordResetService passwordResetService;
  RateLimitingService rateLimitingService;
  I18nService i18nService;
  EmailVerificationService emailVerificationService;

  @PostMapping("/token")
  ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
    var result = authenticationService.authenticate(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @PostMapping("/introspect")
  ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
    var result = authenticationService.introspect(request);
    return ApiResponse.<IntrospectResponse>builder().result(result).build();
  }

  @PostMapping("/refresh")
  ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request)
      throws ParseException, JOSEException {
    var result = authenticationService.refreshToken(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @PostMapping("/logout")
  ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
    authenticationService.logout(request);
    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @RequestBody @Valid ForgotPasswordRequest request, HttpServletRequest httpRequest) {

    if (!rateLimitingService.tryConsumeWithCustomLimit(getClientKey(httpRequest), 1, 1, 1)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(
              ApiResponse.<String>builder()
                  .code(429)
                  .message(i18nService.msg("msg.many.requests"))
                  .build());
    }

    passwordResetService.sendPasswordResetEmail(request);

    return ResponseEntity.ok(
        ApiResponse.<String>builder()
            .code(1000)
            .message(i18nService.msg("success.password.reset.email"))
            .result(i18nService.msg("success.password.reset.email"))
            .build());
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<String>> resetPassword(
      @RequestBody @Valid ResetPasswordRequest request) {

    passwordResetService.resetPassword(request);

    return ResponseEntity.ok(
        ApiResponse.<String>builder()
            .code(1000)
            .message(i18nService.msg("success.password.reset"))
            .result(i18nService.msg("success.password.reset"))
            .build());
  }

  @PostMapping("/verification")
  public ApiResponse<String> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
    emailVerificationService.verifyEmail(request.getToken());
    return ApiResponse.<String>builder().result(i18nService.msg("success.email.verified")).build();
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/verification/resend")
  public ApiResponse<String> resendVerificationEmail(Authentication authentication) {
    emailVerificationService.resendVerificationEmail(authentication.getName());
    return ApiResponse.<String>builder()
        .result(i18nService.msg("success.email.verification.sent"))
        .build();
  }

  private String getClientKey(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}
