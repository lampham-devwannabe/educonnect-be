package com.sep.educonnect.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.service.I18nService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final I18nService i18nService;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        String exceptionMessage = authException.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("Token invalid") || exceptionMessage.contains("Invalid token")) {
                errorCode = ErrorCode.INVALID_TOKEN;
                log.warn("Invalid JWT token received from IP: {}", request.getRemoteAddr());
            } else if (exceptionMessage.contains("expired") || exceptionMessage.contains("ExpiredJwtException")) {
                errorCode = ErrorCode.TOKEN_EXPIRED;
                log.warn("Expired JWT token received from IP: {}", request.getRemoteAddr());
            } else if (exceptionMessage.contains("MalformedJwtException")) {
                errorCode = ErrorCode.INVALID_TOKEN;
                log.warn("Malformed JWT token received from IP: {}", request.getRemoteAddr());
            } else if (exceptionMessage.contains("SignatureException")) {
                errorCode = ErrorCode.INVALID_TOKEN;
                log.warn("Invalid JWT signature received from IP: {}", request.getRemoteAddr());
            }
        }

        log.error("Authentication failed for request: {} {} from IP: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        log.error("Authentication exception: {}", exceptionMessage);

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(i18nService.msg(errorCode.getMessageKey()))
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
