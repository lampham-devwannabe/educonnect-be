package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TutorMonthlySummaryDTO {
    Integer id;
    String tutorId;
    String tutorName;
    Double teachingHours;
    Integer sessionsCompleted;
    BigDecimal monthlyRevenue; // calculated based on tutor experience and course type
}