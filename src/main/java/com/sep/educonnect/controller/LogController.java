package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.log.InteractionLog;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.InteractionLogService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LogController {
    InteractionLogService interactionLogService;
    I18nService i18nService;

    /**
     * Endpoint to receive and log interaction events.
     *
     * @param interactionLog The interaction log data to record
     * @return ApiResponse indicating success or failure
     */
    @PostMapping("/interaction")
    public ApiResponse<Void> logInteraction(@RequestBody InteractionLog interactionLog) {
        try {
            // Ensure timestamp is set if not provided
            InteractionLog logToRecord =
                    interactionLog.timestamp() == null
                            ? new InteractionLog(
                                    interactionLog.sessionId(),
                                    "v2.1",
                                    interactionLog.userId(),
                                    interactionLog.tutorId(),
                                    interactionLog.eventType(),
                                    Instant.now(),
                                    interactionLog.rank(),
                                    interactionLog.query(),
                                    interactionLog.filters(),
                                    interactionLog.value(),
                                    interactionLog.source())
                            : interactionLog;

            // Log the interaction
            interactionLogService.logInteraction(logToRecord);

            return ApiResponse.<Void>builder()
                    .code(1000)
                    .message(i18nService.msg("msg.interaction.success"))
                    .build();
        } catch (Exception e) {
            log.error("Error logging interaction", e);
            return ApiResponse.<Void>builder()
                    .code(5000)
                    .message(i18nService.msg("msg.interaction.fail"))
                    .build();
        }
    }
}
