package com.sep.educonnect.dto.log;

import java.time.Instant;
import java.util.List;

public record SearchLog(
    String sessionId,
    String version,
    String userId,
    Instant timestamp,
    Integer page,
    Integer size,
    String query,
    List<String> filters,
    List<SearchResult> results
) {
    public record SearchResult(
        Integer tutorId,
        Integer rank,
        Float osScore,
        Float score
    ) {}
}
