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
public class AdminDashboardStatsDTO {
    Long totalUsers;
    UserBreakdownDTO userBreakdown;
    Long newUsersThisMonth;
    Long activeUsersThisMonth;

    // Course Statistics
    Long totalCourses;
    CourseBreakdownDTO courseBreakdown;
    Long newCoursesThisMonth;

    // Revenue Statistics
    BigDecimal totalRevenue;
    BigDecimal revenueThisMonth;
    BigDecimal revenueThisYear;
    List<MonthlyRevenueDTO> monthlyRevenue; // 12 tháng gần nhất

    // Order Statistics
    Long totalOrders;
    Long successfulOrders;
    Long pendingOrders;
    Long failedOrders;
    BigDecimal averageOrderValue;

    // Content Statistics
    Long totalVideos;
    Long totalLessons;
    Long totalExams;
    ContentBreakdownDTO contentBreakdown;

    // Class Statistics
    Long totalClasses;
    Long activeClasses;
    Long completedClasses;

    // Engagement Statistics
    Long totalEnrollments;
    Double averageClassSize;
    Long totalSessions;
}
