package com.sep.educonnect.dto.course.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyCourseResponse {
    Long id;
    Long classId;
    String name;
    String pictureUrl;
    Integer progressPercentage;
    Integer completedLessons;
    Integer totalLessons;
    LatestLesson latestLesson;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LatestLesson {
        Long lessonId;
        String title;
    }
}
