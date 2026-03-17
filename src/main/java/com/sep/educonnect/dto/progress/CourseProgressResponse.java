package com.sep.educonnect.dto.progress;

import com.sep.educonnect.enums.CourseProgressStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseProgressResponse {
    Long courseProgressId;
    Long enrollmentId;
    CourseProgressStatus status;
    Integer totalLessons;
    Integer completedLessons;
    Integer progressPercentage;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    List<LessonProgressResponse> lessons;
}

