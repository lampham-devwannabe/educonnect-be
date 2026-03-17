package com.sep.educonnect.dto.log;

import java.time.Instant;
import java.util.List;

public record InteractionLog(
    String sessionId,
    String version,
    String userId,
    Integer tutorId,
    EventType eventType,
    Instant timestamp,
    Integer rank,
    String query,
    List<String> filters,
    Double value,
    String source
) {
}
