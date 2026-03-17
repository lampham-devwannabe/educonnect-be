package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TutorMonthlyDetailDTO {
    // Basic info
    Long id;
    String tutorId;
    String tutorName;
    String tutorEmail;

    // Monthly performance metrics
    Double teachingHours;
    Integer sessionsCompleted;
    Integer onlineCourseStudents;    // students enrolled in tutor's online courses this month
    Integer selfPacedCourseStudents; // students enrolled in tutor's self-paced courses this month
    BigDecimal monthlyRevenue;       // total revenue (online + self-paced)
    BigDecimal onlineRevenue;        // revenue from online courses only
    BigDecimal selfPacedRevenue;     // revenue from self-paced courses only
    BigDecimal averageHourlyRate;

    // Subject breakdown
    List<SubjectMonthlyStatsDTO> subjectBreakdown;

    // Class performance
    List<ClassMonthlyStatsDTO> classStats;

    // Growth indicators
    Double hoursChangeFromLastMonth;
    Double revenueChangeFromLastMonth;

    // Session details (all sessions taught in the month)
    List<SessionDetailDTO> sessions;
}