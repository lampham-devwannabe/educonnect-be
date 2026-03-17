package com.sep.educonnect.service.admin;

import com.sep.educonnect.dto.admin.statistic.AdminDashboardStatsDTO;
import com.sep.educonnect.dto.admin.statistic.ContentBreakdownDTO;
import com.sep.educonnect.dto.admin.statistic.CourseBreakdownDTO;
import com.sep.educonnect.dto.admin.statistic.UserBreakdownDTO;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.PaymentStatus;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminDashboardService {
    final UserRepository userRepository;
    final CourseRepository courseRepository;
    final TransactionRepository transactionRepository;
    final VideoLessonRepository videoLessonRepository;
    final LessonRepository lessonRepository;
    final ExamRepository examRepository;
    final TutorClassRepository tutorClassRepository;
    final ClassEnrollmentRepository classEnrollmentRepository;
    final ClassSessionRepository classSessionRepository;

    public AdminDashboardStatsDTO getDashboardStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime last12Months = now.minusMonths(12);
        LocalDate today = LocalDate.now();

        return AdminDashboardStatsDTO.builder()
                // User Statistics
                .totalUsers(userRepository.countTotalUsers())
                .userBreakdown(getUserBreakdown())
                .newUsersThisMonth(userRepository.countNewUsersSince(startOfMonth))
                .activeUsersThisMonth(userRepository.countActiveUsersSince(startOfMonth))

                // Course Statistics
                .totalCourses(courseRepository.countTotalCourses())
                .courseBreakdown(getCourseBreakdown())
                .newCoursesThisMonth(courseRepository.countNewCoursesSince(startOfMonth))

                // Revenue Statistics
                .totalRevenue(transactionRepository.calculateTotalRevenue())
                .revenueThisMonth(transactionRepository.calculateRevenueSince(startOfMonth))
                .revenueThisYear(transactionRepository.calculateRevenueSince(startOfYear))
                .monthlyRevenue(transactionRepository.getMonthlyRevenue(last12Months))

                // Order Statistics
                .totalOrders(transactionRepository.count())
                .successfulOrders(transactionRepository.countTransactionsByStatus(PaymentStatus.PAID))
                .pendingOrders(transactionRepository.countTransactionsByStatus(PaymentStatus.PENDING))
                .failedOrders(transactionRepository.countTransactionsByStatus(PaymentStatus.FAILED))
                .averageOrderValue(transactionRepository.calculateAverageOrderValue())

                // Content Statistics
                .totalVideos(videoLessonRepository.countTotalVideos())
                .totalLessons(lessonRepository.countTotalLessons())
                .totalExams(examRepository.countTotalExams())
                .contentBreakdown(getContentBreakdown())

                // Class Statistics
                .totalClasses(tutorClassRepository.countTotalClasses())
                .activeClasses(tutorClassRepository.countActiveClasses(today))
                .completedClasses(tutorClassRepository.countCompletedClasses(today))

                // Engagement Statistics
                .totalEnrollments(classEnrollmentRepository.countTotalEnrollments())
                .averageClassSize(tutorClassRepository.calculateAverageClassSize())
                .totalSessions(classSessionRepository.countTotalSessions())

                .build();
    }

    private UserBreakdownDTO getUserBreakdown() {
        return UserBreakdownDTO.builder()
                .students(userRepository.countUsersByRole("STUDENT"))
                .tutors(userRepository.countUsersByRole("TUTOR"))
                .admins(userRepository.countUsersByRole("ADMIN"))
                .verifiedUsers(userRepository.countVerifiedUsers())
                .unverifiedUsers(userRepository.countTotalUsers() - userRepository.countVerifiedUsers())
                .build();
    }

    private CourseBreakdownDTO getCourseBreakdown() {
        return CourseBreakdownDTO.builder()
                .ongoingCourses(courseRepository.countCoursesByStatus(CourseStatus.ONGOING))
                .plannedCourses(courseRepository.countCoursesByStatus(CourseStatus.PLANNED))
                .inactiveCourses(courseRepository.countCoursesByStatus(CourseStatus.INACTIVE))
                .comboCourses(courseRepository.countCoursesByComboType(true))
                .singleCourses(courseRepository.countCoursesByComboType(false))
                .build();
    }

    private ContentBreakdownDTO getContentBreakdown() {
        return ContentBreakdownDTO.builder()
                .readyVideos(videoLessonRepository.countVideosByStatus(VideoStatus.READY))
                .processingVideos(videoLessonRepository.countVideosByStatus(VideoStatus.PROCESSING))
                .failedVideos(videoLessonRepository.countVideosByStatus(VideoStatus.FAILED))
                .publishedLessons(lessonRepository.countLessonsByStatus("PUBLISHED"))
                .draftLessons(lessonRepository.countLessonsByStatus("DRAFT"))
                .activeExams(examRepository.countExamsByStatus("ACTIVE"))
                .build();
    }
}
