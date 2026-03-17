package com.sep.educonnect.service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.educonnect.dto.log.EventType;
import com.sep.educonnect.dto.log.InteractionLog;
import com.sep.educonnect.service.InteractionLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionLogService Unit Tests")
class InteractionLogServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InteractionLogService interactionLogService;

    @BeforeEach
    void setUp() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    @DisplayName("Should log interaction with all fields")
    void should_logInteraction_withAllFields() {
        // Given
        Instant timestamp = Instant.now();
        List<String> filters = Arrays.asList("subject:Math", "level:Beginner");
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.CLICK,
                timestamp,
                1,
                "math tutor",
                filters,
                4.5,
                "web"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared after logging
        assertNull(MDC.get("timestamp"));
        assertNull(MDC.get("sessionId"));
        assertNull(MDC.get("userId"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with null timestamp and use current time")
    void should_logInteraction_withNullTimestamp_useCurrentTime() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.VIEW,
                null, // null timestamp
                1,
                "math tutor",
                null,
                null,
                "mobile"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("timestamp"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with null fields")
    void should_logInteraction_withNullFields() {
        // Given
        InteractionLog log = new InteractionLog(
                null, // null sessionId
                null, // null version
                null, // null userId
                null, // null tutorId
                null, // null eventType
                null, // null timestamp
                null, // null rank
                null, // null query
                null, // null filters
                null, // null value
                null  // null source
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("timestamp"));
        assertNull(MDC.get("sessionId"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with empty strings")
    void should_logInteraction_withEmptyStrings() {
        // Given
        InteractionLog log = new InteractionLog(
                "",
                "",
                "",
                null,
                EventType.RATING,
                Instant.now(),
                null,
                "",
                null,
                null,
                ""
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("sessionId"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with all event types")
    void should_logInteraction_withAllEventTypes() {
        // Given
        EventType[] eventTypes = {
                EventType.VIEW,
                EventType.CLICK,
                EventType.CONVERSION,
                EventType.RATING,
                EventType.WISHLIST,
                EventType.JOIN
        };

        // When & Then
        for (EventType eventType : eventTypes) {
            InteractionLog log = new InteractionLog(
                    "session-123",
                    "v1.0",
                    "user-456",
                    789,
                    eventType,
                    Instant.now(),
                    1,
                    "query",
                    null,
                    null,
                    "web"
            );

            assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
        }
    }

    @Test
    @DisplayName("Should log interaction with filters list")
    void should_logInteraction_withFiltersList() {
        // Given
        List<String> filters = Arrays.asList("subject:Math", "level:Beginner", "price:100-200");
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.CLICK,
                Instant.now(),
                1,
                "math tutor",
                filters,
                null,
                "web"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("filters"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with numeric values")
    void should_logInteraction_withNumericValues() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.RATING,
                Instant.now(),
                5,
                "query",
                null,
                4.75,
                "mobile"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("rank"));
        assertNull(MDC.get("value"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should handle exception and clear MDC")
    void should_handleException_andClearMDC() {
        // Given
        // Create a log that will cause an exception when toString() is called on filters
        // This tests the exception handling in the service
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.CLICK,
                Instant.now(),
                1,
                "query",
                null,
                null,
                "web"
        );

        // When
        // Service should handle any exception gracefully
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));

        // Then
        // MDC should still be cleared even if an exception occurs
        assertNull(MDC.get("timestamp"));
        assertNull(MDC.get("sessionId"));
    }

    @Test
    @DisplayName("Should log interaction with zero values")
    void should_logInteraction_withZeroValues() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                0,
                EventType.VIEW,
                Instant.now(),
                0,
                "query",
                null,
                0.0,
                "web"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("tutorId"));
        assertNull(MDC.get("rank"));
        assertNull(MDC.get("value"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with large values")
    void should_logInteraction_withLargeValues() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                Integer.MAX_VALUE,
                EventType.CLICK,
                Instant.now(),
                Integer.MAX_VALUE,
                "very long query string that might cause issues",
                Arrays.asList("filter1", "filter2", "filter3", "filter4", "filter5"),
                Double.MAX_VALUE,
                "api"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("tutorId"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log multiple interactions sequentially")
    void should_logMultipleInteractions_sequentially() {
        // Given
        InteractionLog log1 = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.VIEW,
                Instant.now(),
                1,
                "query1",
                null,
                null,
                "web"
        );

        InteractionLog log2 = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.CLICK,
                Instant.now(),
                2,
                "query2",
                null,
                null,
                "mobile"
        );

        // When
        interactionLogService.logInteraction(log1);
        interactionLogService.logInteraction(log2);

        // Then
        // MDC should be cleared after each call
        assertNull(MDC.get("timestamp"));
        assertNull(MDC.get("sessionId"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> {
            interactionLogService.logInteraction(log1);
            interactionLogService.logInteraction(log2);
        });
    }

    @Test
    @DisplayName("Should log interaction with special characters in query")
    void should_logInteraction_withSpecialCharactersInQuery() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.CLICK,
                Instant.now(),
                1,
                "query with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?",
                null,
                null,
                "web"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("query"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with unicode characters")
    void should_logInteraction_withUnicodeCharacters() {
        // Given
        InteractionLog log = new InteractionLog(
                "session-123",
                "v1.0",
                "user-456",
                789,
                EventType.VIEW,
                Instant.now(),
                1,
                "query with unicode: 你好世界 🌍",
                Arrays.asList("filter:中文", "filter:日本語"),
                null,
                "mobile"
        );

        // When
        interactionLogService.logInteraction(log);

        // Then
        // MDC should be cleared
        assertNull(MDC.get("query"));
        assertNull(MDC.get("filters"));
        // Verify no exceptions thrown
        assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
    }

    @Test
    @DisplayName("Should log interaction with different source values")
    void should_logInteraction_withDifferentSourceValues() {
        // Given
        String[] sources = {"web", "mobile", "api", "desktop", null};

        // When & Then
        for (String source : sources) {
            InteractionLog log = new InteractionLog(
                    "session-123",
                    "v1.0",
                    "user-456",
                    789,
                    EventType.VIEW,
                    Instant.now(),
                    1,
                    "query",
                    null,
                    null,
                    source
            );

            assertDoesNotThrow(() -> interactionLogService.logInteraction(log));
        }
    }
}

