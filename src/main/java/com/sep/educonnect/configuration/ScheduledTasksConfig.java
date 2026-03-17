package com.sep.educonnect.configuration;

import com.sep.educonnect.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {

    private final PasswordResetService passwordResetService;

    // Clean up expired tokens every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");
        passwordResetService.cleanupExpiredTokens();
        log.info("Completed cleanup of expired password reset tokens");
    }
}
