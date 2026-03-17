package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SubjectStatDTO {
    Long subjectId;
    String subjectName;
    Integer classCount;
    Integer sessionCount;
    Double totalHours;
}
