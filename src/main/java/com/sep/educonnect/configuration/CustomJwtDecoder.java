package com.sep.educonnect.configuration;

import com.sep.educonnect.dto.auth.request.IntrospectRequest;
import com.sep.educonnect.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Slf4j
@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        // Validate token format trước
        if (token == null || token.trim().isEmpty()) {
            log.warn("JWT token is null or empty");
            throw new JwtException("Token is null or empty");
        }

        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 3) {
            log.warn("Invalid JWT token format - expected 3 parts, got {}", tokenParts.length);
            throw new JwtException("Invalid token format");
        }

        for (int i = 0; i < tokenParts.length; i++) {
            if (tokenParts[i] == null || tokenParts[i].trim().isEmpty()) {
                log.warn("JWT token part {} is empty", i);
                throw new JwtException("Invalid token format");
            }
        }

        try {
            log.debug("Validating JWT token with authentication service");
            var response = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build());

            if (!response.isValid()) {
                log.warn("JWT token validation failed - token is invalid");
                throw new JwtException("Token invalid");
            }

            log.debug("JWT token validation successful");
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage());
            throw new JwtException("Token validation failed");
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            try {
                log.debug("Initializing NimbusJwtDecoder");
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
                log.debug("NimbusJwtDecoder initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize NimbusJwtDecoder: {}", e.getMessage());
                throw new JwtException("JWT decoder initialization failed");
            }
        }

        try {
            return nimbusJwtDecoder.decode(token);
        } catch (Exception e) {
            log.error("Failed to decode JWT token: {}", e.getMessage());
            throw new JwtException("Token decoding failed: " + e.getMessage());
        }
    }
}
