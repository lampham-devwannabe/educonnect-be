package com.sep.educonnect.dto.progress;

import com.sep.educonnect.enums.LessonProgressStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLessonProgressRequest {

    @NotNull
    Long enrollmentId;

    @NotNull
    Long lessonId;

    @NotNull
    LessonProgressStatus status;

    String result;

    Boolean skipOptional;
}

