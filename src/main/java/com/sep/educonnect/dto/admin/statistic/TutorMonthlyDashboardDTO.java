package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TutorMonthlyDashboardDTO {
    Integer year;
    Integer month;

    // Overall monthly statistics
    Integer totalActiveTutors;
    Double totalTeachingHours;
    Integer totalSessionsCompleted;

    // Top 3 performers for each category
    List<TopTutorDTO> topTutorsByHours;
    List<TopTutorDTO> topTutorsBySessions;
    List<TopTutorDTO> topTutorsByStudents;

    // Monthly trends (compared to previous month)
    Double hoursGrowthPercentage;
    Double sessionsGrowthPercentage;
    Double tutorsGrowthPercentage;
}