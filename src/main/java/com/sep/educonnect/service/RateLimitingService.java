// src/main/java/com/sep/educonnect/service/RateLimitingService.java
package com.sep.educonnect.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.capacity:3}")
    private int capacity;

    @Value("${rate-limit.refill-tokens:3}")
    private int refillTokens;

    @Value("${rate-limit.refill-duration:1}")
    private int refillDurationMinutes;

    public boolean tryConsume(String key) {
        Bucket bucket = getBucket(key);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }

        return consumed;
    }

    public boolean tryConsume(HttpServletRequest request) {
        String clientKey = getClientKey(request);
        return tryConsume(clientKey);
    }

    public long getAvailableTokens(String key) {
        Bucket bucket = getBucket(key);
        return bucket.getAvailableTokens();
    }

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity,
                    Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes)));

            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    private String getClientKey(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Try X-Real-IP (for nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public boolean tryConsumeWithCustomLimit(String key, int capacity, int refillTokens, int refillDurationMinutes) {
        Bucket customBucket = buckets.computeIfAbsent(key + "_custom", k -> {
            Bandwidth limit = Bandwidth.classic(capacity,
                    Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes)));

            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });

        return customBucket.tryConsume(1);
    }
}