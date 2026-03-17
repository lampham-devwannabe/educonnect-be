package com.sep.educonnect.service;

import com.sep.educonnect.dto.log.InteractionLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionLogService {
    private static final Logger interactionLogger = LoggerFactory.getLogger("com.sep.educonnect.interaction");

    /**
     * Logs an interaction event using MDC for structured logging.
     * All fields from InteractionLog are added to MDC and logged as JSON.
     *
     * @param interactionLog The interaction log data to record
     */
    public void logInteraction(InteractionLog interactionLog) {
        try {
            // Set MDC values for structured logging
            // Use timestamp from input, fallback to current time if not provided
            String timestampValue = interactionLog.timestamp() != null
                    ? interactionLog.timestamp().toString()
                    : Instant.now().toString();
            MDC.put("timestamp", timestampValue);
            MDC.put("sessionId", interactionLog.sessionId() != null ? interactionLog.sessionId() : "");
            MDC.put("version", "v2.1");
            MDC.put("userId", interactionLog.userId() != null ? interactionLog.userId() : "");
            MDC.put("tutorId", interactionLog.tutorId() != null ? String.valueOf(interactionLog.tutorId()) : "");
            MDC.put("eventType", interactionLog.eventType() != null ? interactionLog.eventType().getEvent() : "");
            MDC.put("rank", interactionLog.rank() != null ? String.valueOf(interactionLog.rank()) : "");
            MDC.put("query", interactionLog.query() != null ? interactionLog.query() : "");
            MDC.put("filters", interactionLog.filters() != null ? interactionLog.filters().toString() : "[]");
            MDC.put("value", interactionLog.value() != null ? String.valueOf(interactionLog.value()) : "");
            MDC.put("source", interactionLog.source() != null ? interactionLog.source() : "");

            // Log the interaction
            interactionLogger.info("Interaction logged");

            // Clear MDC to avoid memory leaks
            MDC.clear();
        } catch (Exception e) {
            log.error("Error logging interaction", e);
            // Ensure MDC is cleared even on error
            MDC.clear();
        }
    }
}

