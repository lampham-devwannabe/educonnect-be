package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TopTutorDTO {
    String tutorId;
    String tutorName;
    Double teachingHours;
    Integer sessionsCompleted;
    Integer studentsTaught;
}