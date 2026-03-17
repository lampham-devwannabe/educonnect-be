package com.sep.educonnect.dto.progress;

import com.sep.educonnect.enums.LessonProgressStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgressResponse {
    Long lessonId;
    String lessonTitle;
    LessonProgressStatus status;
    Boolean optionalLesson;
    String result;
    LocalDateTime lastAccessedAt;
    LocalDateTime completedAt;
}

