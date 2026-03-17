package com.sep.educonnect.dto.search.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TutorResponse(
    Integer id,
    String name,
    String headline,
    String avatarUrl,
    List<String> teachingStyle,
    double rating,
    int studentsCount,
    int numberReviews,
    int totalLessons,
    Float score
) {
    // Convenience method for creating a copy with different score
    public TutorResponse withScore(float newScore) {
        return new TutorResponse(id, name, headline, avatarUrl, teachingStyle,
                rating, studentsCount, numberReviews, totalLessons, newScore);
    }
}
