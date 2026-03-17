package com.sep.educonnect.dto.search.request;

import java.util.List;

public record SearchTutorParams(String sessionId, String userId, String query, String subject, List<String> level,
                                Double rating, Integer lowestPrice, Integer highestPrice, List<AvailabilityRange> availabilities,
                                List<String> styles, Integer page, Integer size) {
    public record AvailabilityRange(String day, Integer start, Integer end) {
    }

    public boolean hasTextQuery() {
        return query() != null && !query().isBlank();
    }
}
