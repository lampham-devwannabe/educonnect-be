package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TeachingStatisticsDTO {
    String tutorId;
    String tutorName;

    // Thống kê tổng quan
    Integer totalClasses;
    Integer activeClasses;
    Integer completedClasses;
    Integer totalStudents;
    Double averageRating;

    // Thống kê thời gian dạy
    Integer totalSessionsCompleted;
    Integer totalSessionsUpcoming;
    Integer totalSessionsCancelled;
    Double totalTeachingHours;

    // Thống kê theo tháng
    List<MonthlyStatDTO> monthlyStats;

    // Thống kê theo môn học
    List<SubjectStatDTO> subjectStats;

    // Thời gian dạy gần đây
    LocalDateTime firstSessionDate;
    LocalDateTime lastSessionDate;
    LocalDateTime nextSessionDate;
}
