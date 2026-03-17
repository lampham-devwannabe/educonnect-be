package com.sep.educonnect.controller.admin;

import com.sep.educonnect.dto.admin.statistic.*;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.service.admin.AdminDashboardService;
import com.sep.educonnect.service.admin.TeachingStatisticsService;
import com.sep.educonnect.service.admin.TutorMonthlyDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminStatisticController {
    final TeachingStatisticsService statisticsService;
    final AdminDashboardService dashboardService;
    final TutorMonthlyDashboardService tutorDashboardService;

    @GetMapping("/teaching-statistics/tutor/{tutorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
    public ApiResponse<TeachingStatisticsDTO> getTeachingStatistics(
            @PathVariable String tutorId) {
        TeachingStatisticsDTO statistics = statisticsService.getTeachingStatistics(tutorId);
        return ApiResponse.<TeachingStatisticsDTO>builder()
                .result(statistics)
                .build();
    }

    @GetMapping("/statistics")
    public ApiResponse<AdminDashboardStatsDTO> getDashboardStatistics() {
        AdminDashboardStatsDTO stats = dashboardService.getDashboardStatistics();
        return ApiResponse.<AdminDashboardStatsDTO>builder()
                .result(stats)
                .build();
    }

    @GetMapping("/statistics/summary")
    public ApiResponse<Map<String, Object>> getQuickSummary() {
        AdminDashboardStatsDTO stats = dashboardService.getDashboardStatistics();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", stats.getTotalUsers());
        summary.put("totalCourses", stats.getTotalCourses());
        summary.put("totalRevenue", stats.getTotalRevenue());
        summary.put("totalOrders", stats.getTotalOrders());
        summary.put("revenueThisMonth", stats.getRevenueThisMonth());
        summary.put("newUsersThisMonth", stats.getNewUsersThisMonth());

        return ApiResponse.<Map<String, Object>>builder()
                .result(summary)
                .build();
    }

    @GetMapping("/tutors/monthly-dashboard/{year}/{month}")
    public ApiResponse<TutorMonthlyDashboardDTO> getMonthlyTutorDashboard(
            @PathVariable Integer year,
            @PathVariable Integer month) {

        TutorMonthlyDashboardDTO dashboard = tutorDashboardService.getMonthlyDashboard(year, month);
        return ApiResponse.<TutorMonthlyDashboardDTO>builder()
                .result(dashboard)
                .build();
    }

    @GetMapping("/tutors/monthly-summaries/{year}/{month}")
    public ApiResponse<Page<TutorMonthlySummaryDTO>> getMonthlyTutorSummaries(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "teachingHours") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        if (!List.of("teachingHours", "sessionsCompleted", "tutorName").contains(sortBy)) {
            sortBy = "teachingHours";
        }

        Page<TutorMonthlySummaryDTO> summaries = tutorDashboardService.getMonthlySummaries(
                year, month, page, size, sortBy, sortDirection
        );
        return ApiResponse.<Page<TutorMonthlySummaryDTO>>builder()
                .result(summaries)
                .build();
    }

    @GetMapping("/tutors/monthly-detail/{id}/{year}/{month}")
    public ApiResponse<TutorMonthlyDetailDTO> getMonthlyTutorDetail(
            @PathVariable Long id,
            @PathVariable Integer year,
            @PathVariable Integer month) {

        TutorMonthlyDetailDTO detail = tutorDashboardService.getMonthlyDetail(id, year, month);
        return ApiResponse.<TutorMonthlyDetailDTO>builder()
                .result(detail)
                .build();
    }

}
