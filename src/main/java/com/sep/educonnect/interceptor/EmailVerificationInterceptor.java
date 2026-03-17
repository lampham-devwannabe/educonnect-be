package com.sep.educonnect.interceptor;

import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailVerificationInterceptor implements HandlerInterceptor {

    static final Set<String> EXACT_WHITELIST = Set.of(
            "/api/auth/token",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/users/my-info",
            "/api/payments/return",
            "/api/wishlist/{courseId}/exists",
            "/api/students/schedule/{tutorId}"
//            "/api/course-reviews",
//            "/api/course-reviews/course/{courseId}"
    );

    static final List<String> PREFIX_WHITELIST = List.of(
            "/api/auth/verification",
            "/search/*",
            "/api/course-reviews/course/{courseId}/**",
            "/api/top/**",
            "api/ratings/tutor/**"
    );

    UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // Check whitelist FIRST, before checking authentication
        // This ensures verification endpoints are always accessible
        if (isWhitelisted(path)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return true;
        }

        // Try to read isVerified from JWT claim first (performance optimization)
        Boolean isVerified = extractIsVerifiedFromJwt(principal);

        // If not found in JWT, fallback to database query (should be rare)
        if (isVerified == null) {
            log.debug("isVerified claim not found in JWT, querying database for user: {}", authentication.getName());
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
            isVerified = user.getEmailVerified();
        }

        if (Boolean.TRUE.equals(isVerified)) {
            return true;
        }

        log.debug("Blocked access to {} for unverified user {}", path, authentication.getName());
        throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    /**
     * Extract isVerified claim from JWT token to avoid database query.
     * Returns null if claim is not found or principal is not a Jwt object.
     */
    private Boolean extractIsVerifiedFromJwt(Object principal) {
        if (principal instanceof Jwt jwt) {
            Object isVerifiedClaim = jwt.getClaim("isVerified");
            if (isVerifiedClaim instanceof Boolean) {
                return (Boolean) isVerifiedClaim;
            }
            // Handle case where claim might be stored as String
            if (isVerifiedClaim instanceof String) {
                return Boolean.parseBoolean((String) isVerifiedClaim);
            }
        }
        return null;
    }

    private boolean isWhitelisted(String path) {
        if (EXACT_WHITELIST.contains(path)) {
            return true;
        }

        return PREFIX_WHITELIST.stream().anyMatch(path::startsWith);
    }
}

