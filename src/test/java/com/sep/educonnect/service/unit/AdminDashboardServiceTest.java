package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.admin.statistic.AdminDashboardStatsDTO;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.PaymentStatus;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.admin.AdminDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private VideoLessonRepository videoLessonRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private TutorClassRepository tutorClassRepository;
    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("Should return correct dashboard statistics")
    void should_returnCorrectDashboardStatistics() {
        // Mock User Stats
        when(userRepository.countTotalUsers()).thenReturn(100L);
        when(userRepository.countNewUsersSince(any(LocalDateTime.class))).thenReturn(10L);
        when(userRepository.countActiveUsersSince(any(LocalDateTime.class))).thenReturn(50L);
        when(userRepository.countUsersByRole("STUDENT")).thenReturn(80L);
        when(userRepository.countUsersByRole("TUTOR")).thenReturn(15L);
        when(userRepository.countUsersByRole("ADMIN")).thenReturn(5L);
        when(userRepository.countVerifiedUsers()).thenReturn(90L);

        // Mock Course Stats
        when(courseRepository.countTotalCourses()).thenReturn(20L);
        when(courseRepository.countNewCoursesSince(any(LocalDateTime.class))).thenReturn(2L);
        when(courseRepository.countCoursesByStatus(CourseStatus.ONGOING)).thenReturn(10L);
        when(courseRepository.countCoursesByStatus(CourseStatus.PLANNED)).thenReturn(5L);
        when(courseRepository.countCoursesByStatus(CourseStatus.INACTIVE)).thenReturn(5L);
        when(courseRepository.countCoursesByComboType(true)).thenReturn(3L);
        when(courseRepository.countCoursesByComboType(false)).thenReturn(17L);

        // Mock Revenue Stats
        when(transactionRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.calculateRevenueSince(any(LocalDateTime.class))).thenReturn(new BigDecimal("1000"));
        when(transactionRepository.getMonthlyRevenue(any(LocalDateTime.class))).thenReturn(List.of());

        // Mock Order Stats
        when(transactionRepository.count()).thenReturn(50L);
        when(transactionRepository.countTransactionsByStatus(PaymentStatus.PAID)).thenReturn(40L);
        when(transactionRepository.countTransactionsByStatus(PaymentStatus.PENDING)).thenReturn(5L);
        when(transactionRepository.countTransactionsByStatus(PaymentStatus.FAILED)).thenReturn(5L);
        when(transactionRepository.calculateAverageOrderValue()).thenReturn(new BigDecimal("200"));

        // Mock Content Stats
        when(videoLessonRepository.countTotalVideos()).thenReturn(100L);
        when(lessonRepository.countTotalLessons()).thenReturn(50L);
        when(examRepository.countTotalExams()).thenReturn(30L);
        when(videoLessonRepository.countVideosByStatus(VideoStatus.READY)).thenReturn(90L);
        when(videoLessonRepository.countVideosByStatus(VideoStatus.PROCESSING)).thenReturn(5L);
        when(videoLessonRepository.countVideosByStatus(VideoStatus.FAILED)).thenReturn(5L);
        when(lessonRepository.countLessonsByStatus("PUBLISHED")).thenReturn(45L);
        when(lessonRepository.countLessonsByStatus("DRAFT")).thenReturn(5L);
        when(examRepository.countExamsByStatus("ACTIVE")).thenReturn(25L);

        // Mock Class Stats
        when(tutorClassRepository.countTotalClasses()).thenReturn(15L);
        when(tutorClassRepository.countActiveClasses(any(LocalDate.class))).thenReturn(10L);
        when(tutorClassRepository.countCompletedClasses(any(LocalDate.class))).thenReturn(5L);

        // Mock Engagement Stats
        when(classEnrollmentRepository.countTotalEnrollments()).thenReturn(200L);
        when(tutorClassRepository.calculateAverageClassSize()).thenReturn(15.5);
        when(classSessionRepository.countTotalSessions()).thenReturn(300L);

        // Execute
        AdminDashboardStatsDTO result = adminDashboardService.getDashboardStatistics();

        // Verify
        assertNotNull(result);

        // Verify User Stats
        assertEquals(100L, result.getTotalUsers());
        assertEquals(10L, result.getNewUsersThisMonth());
        assertEquals(50L, result.getActiveUsersThisMonth());
        assertEquals(80L, result.getUserBreakdown().getStudents());
        assertEquals(15L, result.getUserBreakdown().getTutors());
        assertEquals(5L, result.getUserBreakdown().getAdmins());
        assertEquals(90L, result.getUserBreakdown().getVerifiedUsers());
        assertEquals(10L, result.getUserBreakdown().getUnverifiedUsers());

        // Verify Course Stats
        assertEquals(20L, result.getTotalCourses());
        assertEquals(2L, result.getNewCoursesThisMonth());
        assertEquals(10L, result.getCourseBreakdown().getOngoingCourses());
        assertEquals(5L, result.getCourseBreakdown().getPlannedCourses());
        assertEquals(5L, result.getCourseBreakdown().getInactiveCourses());
        assertEquals(3L, result.getCourseBreakdown().getComboCourses());
        assertEquals(17L, result.getCourseBreakdown().getSingleCourses());

        // Verify Revenue Stats
        assertEquals(new BigDecimal("10000"), result.getTotalRevenue());
        assertEquals(new BigDecimal("1000"), result.getRevenueThisMonth());

        // Verify Order Stats
        assertEquals(50L, result.getTotalOrders());
        assertEquals(40L, result.getSuccessfulOrders());
        assertEquals(5L, result.getPendingOrders());
        assertEquals(5L, result.getFailedOrders());
        assertEquals(new BigDecimal("200"), result.getAverageOrderValue());

        // Verify Content Stats
        assertEquals(100L, result.getTotalVideos());
        assertEquals(50L, result.getTotalLessons());
        assertEquals(30L, result.getTotalExams());
        assertEquals(90L, result.getContentBreakdown().getReadyVideos());
        assertEquals(5L, result.getContentBreakdown().getProcessingVideos());
        assertEquals(5L, result.getContentBreakdown().getFailedVideos());
        assertEquals(45L, result.getContentBreakdown().getPublishedLessons());
        assertEquals(5L, result.getContentBreakdown().getDraftLessons());
        assertEquals(25L, result.getContentBreakdown().getActiveExams());

        // Verify Class Stats
        assertEquals(15L, result.getTotalClasses());
        assertEquals(10L, result.getActiveClasses());
        assertEquals(5L, result.getCompletedClasses());

        // Verify Engagement Stats
        assertEquals(200L, result.getTotalEnrollments());
        assertEquals(15.5, result.getAverageClassSize());
        assertEquals(300L, result.getTotalSessions());
    }
}
