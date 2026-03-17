package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.educonnect.dto.admin.statistic.MonthlyStatDTO;
import com.sep.educonnect.dto.admin.statistic.SubjectStatDTO;
import com.sep.educonnect.dto.admin.statistic.TeachingStatisticsDTO;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassSessionRepository;
import com.sep.educonnect.repository.TutorClassRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import com.sep.educonnect.service.admin.TeachingStatisticsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TeachingStatisticsServiceTest {

    @Mock private TutorClassRepository tutorClassRepository;
    @Mock private ClassSessionRepository sessionRepository;
    @Mock private TutorProfileRepository tutorProfileRepository;

    @InjectMocks private TeachingStatisticsService teachingStatisticsService;

    @Test
    @DisplayName("Should return correct teaching statistics")
    void should_returnCorrectTeachingStatistics() {
        // Given
        String tutorId = "tutor-1";
        User user = User.builder().userId(tutorId).firstName("John").lastName("Doe").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(1L)
                        .user(user)
                        .rating(4.5)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(10L).subjectId(100L).name("Math Syllabus").build();

        Course course = Course.builder().id(20L).syllabus(syllabus).build();

        TutorClass tutorClass =
                TutorClass.builder()
                        .id(30L)
                        .course(course)
                        .currentStudents(5)
                        .enrollments(new ArrayList<>())
                        .build();

        // Add enrollments to tutorClass
        User student1 = User.builder().userId("student-1").build();
        ClassEnrollment enrollment = ClassEnrollment.builder().student(student1).build();
        tutorClass.getEnrollments().add(enrollment);

        ClassSession completedSession =
                ClassSession.builder()
                        .id(40L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(
                                LocalDateTime.now()
                                        .minusDays(1)
                                        .withHour(12)
                                        .withMinute(0)) // 2 hours
                        .build();

        ClassSession upcomingSession =
                ClassSession.builder()
                        .id(41L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().plusDays(1))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(upcomingSession));
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(1);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(completedSession, upcomingSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(tutorId, result.getTutorId());
        assertEquals("John Doe", result.getTutorName());
        assertEquals(1, result.getTotalClasses());
        assertEquals(1, result.getActiveClasses());
        assertEquals(0, result.getCompletedClasses());
        assertEquals(5, result.getTotalStudents());
        assertEquals(4.5, result.getAverageRating());
        assertEquals(1, result.getTotalSessionsCompleted());
        assertEquals(1, result.getTotalSessionsUpcoming());
        assertEquals(1, result.getTotalSessionsCancelled());
        assertEquals(2.0, result.getTotalTeachingHours()); // 2 hours

        // Verify Monthly Stats
        assertFalse(result.getMonthlyStats().isEmpty());
        assertEquals(1, result.getMonthlyStats().get(0).getSessionsCompleted());
        assertEquals(2.0, result.getMonthlyStats().get(0).getHoursCompleted());
        assertEquals(1, result.getMonthlyStats().get(0).getStudentsEnrolled());

        // Verify Subject Stats
        assertFalse(result.getSubjectStats().isEmpty());
        assertEquals(100L, result.getSubjectStats().get(0).getSubjectId());
        assertEquals("Math Syllabus", result.getSubjectStats().get(0).getSubjectName());
        assertEquals(1, result.getSubjectStats().get(0).getClassCount());
        assertEquals(1, result.getSubjectStats().get(0).getSessionCount());
        assertEquals(2.0, result.getSubjectStats().get(0).getTotalHours());
    }

    @Test
    @DisplayName("Should throw exception when tutor profile not found")
    void should_throwException_when_tutorProfileNotFound() {
        // Given
        String tutorId = "non-existent-tutor";
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            teachingStatisticsService.getTeachingStatistics(tutorId);
                        });

        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository)
                .findByUserUserIdAndSubmissionStatus(tutorId, ProfileStatus.APPROVED);
    }

    @Test
    @DisplayName("Should return Unknown Subject when subject name not found")
    void should_returnUnknownSubject_whenNotFound() {
        // Given
        String tutorId = "tutor-4";
        User user = User.builder().userId(tutorId).firstName("Alice").lastName("Williams").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(4L)
                        .user(user)
                        .rating(4.0)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        // Course and syllabus with null subject id or missing syllabus
        Course course =
                Course.builder()
                        .id(24L)
                        .syllabus(null) // No syllabus
                        .build();

        TutorClass tutorClass =
                TutorClass.builder()
                        .id(34L)
                        .course(course)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession completedSession =
                ClassSession.builder()
                        .id(45L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(5).withHour(13).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(5).withHour(14).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(completedSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        // Subject stats will be empty because syllabus is null, so no subject can be extracted
        assertTrue(result.getSubjectStats().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple subjects with same id")
    void should_handleMultipleSubjects_withSameId() {
        // Given
        String tutorId = "tutor-5";
        User user = User.builder().userId(tutorId).firstName("Charlie").lastName("Brown").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(5L)
                        .user(user)
                        .rating(4.7)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(14L).subjectId(104L).name("English Syllabus").build();

        Course course = Course.builder().id(25L).syllabus(syllabus).build();

        TutorClass tutorClass1 =
                TutorClass.builder()
                        .id(35L)
                        .course(course)
                        .currentStudents(5)
                        .enrollments(new ArrayList<>())
                        .build();

        TutorClass tutorClass2 =
                TutorClass.builder()
                        .id(36L)
                        .course(course)
                        .currentStudents(3)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession session1 =
                ClassSession.builder()
                        .id(46L)
                        .tutorClass(tutorClass1)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0))
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(47L)
                        .tutorClass(tutorClass2)
                        .startTime(LocalDateTime.now().minusDays(2).withHour(14).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(2).withHour(15).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId))
                .thenReturn(List.of(tutorClass1, tutorClass2));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass1, tutorClass2));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session1, session2));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session1, session2));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSubjectStats().size());
        assertEquals("English Syllabus", result.getSubjectStats().get(0).getSubjectName());
        assertEquals(104L, result.getSubjectStats().get(0).getSubjectId());
        assertEquals(2, result.getSubjectStats().get(0).getClassCount());
        assertEquals(2, result.getSubjectStats().get(0).getSessionCount());
    }

    @Test
    @DisplayName("Should prioritize subject name from classes over sessions")
    void should_prioritizeSubjectName_fromClassesOverSessions() {
        // Given
        String tutorId = "tutor-6";
        User user = User.builder().userId(tutorId).firstName("David").lastName("Lee").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(6L)
                        .user(user)
                        .rating(4.6)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabusInClass =
                Syllabus.builder()
                        .syllabusId(15L)
                        .subjectId(105L)
                        .name("History Syllabus from Class")
                        .build();

        Course courseInClass = Course.builder().id(26L).syllabus(syllabusInClass).build();

        TutorClass tutorClass =
                TutorClass.builder()
                        .id(37L)
                        .course(courseInClass)
                        .currentStudents(4)
                        .enrollments(new ArrayList<>())
                        .build();

        // Session has different syllabus name for same subject id
        Syllabus syllabusInSession =
                Syllabus.builder()
                        .syllabusId(16L)
                        .subjectId(105L)
                        .name("History Syllabus from Session")
                        .build();

        Course courseInSession = Course.builder().id(27L).syllabus(syllabusInSession).build();

        TutorClass tutorClassForSession =
                TutorClass.builder()
                        .id(38L)
                        .course(courseInSession)
                        .currentStudents(0)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession completedSession =
                ClassSession.builder()
                        .id(48L)
                        .tutorClass(tutorClassForSession)
                        .startTime(LocalDateTime.now().minusDays(3).withHour(9).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(3).withHour(10).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(completedSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSubjectStats().size());
        // Should use name from classes (first priority)
        assertEquals(
                "History Syllabus from Class", result.getSubjectStats().get(0).getSubjectName());
        assertEquals(105L, result.getSubjectStats().get(0).getSubjectId());
    }

    @Test
    @DisplayName("Should handle empty classes and sessions lists")
    void should_handleEmpty_classesAndSessions() {
        // Given
        String tutorId = "tutor-14";
        User user = User.builder().userId(tutorId).firstName("Leo").lastName("White").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(14L)
                        .user(user)
                        .rating(4.0)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(Collections.emptyList());
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(Collections.emptyList());

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSubjectStats().isEmpty());
        assertEquals(0, result.getTotalClasses());
        assertEquals(0, result.getTotalSessionsCompleted());
    }

    @Test
    @DisplayName("Should sort subjects by total hours descending")
    void should_sortSubjects_byTotalHoursDescending() {
        // Given
        String tutorId = "tutor-15";
        User user = User.builder().userId(tutorId).firstName("Mia").lastName("Harris").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(15L)
                        .user(user)
                        .rating(4.8)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        // Subject 110 - 1 hour
        Syllabus syllabus1 =
                Syllabus.builder().syllabusId(21L).subjectId(110L).name("Subject A").build();
        Course course1 = Course.builder().id(34L).syllabus(syllabus1).build();
        TutorClass class1 =
                TutorClass.builder()
                        .id(47L)
                        .course(course1)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();
        ClassSession session1 =
                ClassSession.builder()
                        .id(54L)
                        .tutorClass(class1)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0))
                        .build();

        // Subject 111 - 3 hours
        Syllabus syllabus2 =
                Syllabus.builder().syllabusId(22L).subjectId(111L).name("Subject B").build();
        Course course2 = Course.builder().id(35L).syllabus(syllabus2).build();
        TutorClass class2 =
                TutorClass.builder()
                        .id(48L)
                        .course(course2)
                        .currentStudents(2)
                        .enrollments(new ArrayList<>())
                        .build();
        ClassSession session2 =
                ClassSession.builder()
                        .id(55L)
                        .tutorClass(class2)
                        .startTime(LocalDateTime.now().minusDays(2).withHour(9).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(2).withHour(12).withMinute(0))
                        .build();

        // Subject 112 - 2 hours
        Syllabus syllabus3 =
                Syllabus.builder().syllabusId(23L).subjectId(112L).name("Subject C").build();
        Course course3 = Course.builder().id(36L).syllabus(syllabus3).build();
        TutorClass class3 =
                TutorClass.builder()
                        .id(49L)
                        .course(course3)
                        .currentStudents(3)
                        .enrollments(new ArrayList<>())
                        .build();
        ClassSession session3 =
                ClassSession.builder()
                        .id(56L)
                        .tutorClass(class3)
                        .startTime(LocalDateTime.now().minusDays(3).withHour(14).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(3).withHour(16).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId))
                .thenReturn(List.of(class1, class2, class3));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(class1, class2, class3));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session1, session2, session3));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session1, session2, session3));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getSubjectStats().size());
        // Should be sorted by totalHours descending: B (3h), C (2h), A (1h)
        assertEquals(111L, result.getSubjectStats().get(0).getSubjectId());
        assertEquals("Subject B", result.getSubjectStats().get(0).getSubjectName());
        assertEquals(3.0, result.getSubjectStats().get(0).getTotalHours());

        assertEquals(112L, result.getSubjectStats().get(1).getSubjectId());
        assertEquals("Subject C", result.getSubjectStats().get(1).getSubjectName());
        assertEquals(2.0, result.getSubjectStats().get(1).getTotalHours());

        assertEquals(110L, result.getSubjectStats().get(2).getSubjectId());
        assertEquals("Subject A", result.getSubjectStats().get(2).getSubjectName());
        assertEquals(1.0, result.getSubjectStats().get(2).getTotalHours());
    }

    @Test
    @DisplayName("Should handle session with null endTime in monthly stats")
    void should_handleSession_withNullEndTimeInMonthlyStats() {
        // Given
        String tutorId = "tutor-17";
        User user = User.builder().userId(tutorId).firstName("Oscar").lastName("Lewis").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(17L)
                        .user(user)
                        .rating(4.4)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(25L).subjectId(114L).name("Geometry").build();
        Course course = Course.builder().id(38L).syllabus(syllabus).build();
        TutorClass tutorClass =
                TutorClass.builder()
                        .id(51L)
                        .course(course)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession sessionWithNullEndTime =
                ClassSession.builder()
                        .id(59L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(3).withHour(14).withMinute(0))
                        .endTime(null) // Null endTime
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(sessionWithNullEndTime));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(sessionWithNullEndTime));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertFalse(result.getMonthlyStats().isEmpty());
        assertEquals(1, result.getMonthlyStats().get(0).getSessionsCompleted());
        assertEquals(0.0, result.getMonthlyStats().get(0).getHoursCompleted()); // No hours
    }

    @Test
    @DisplayName("Should sort monthly stats by year and month descending")
    void should_sortMonthlyStats_byYearAndMonthDescending() {
        // Given
        String tutorId = "tutor-21";
        User user = User.builder().userId(tutorId).firstName("Sam").lastName("Young").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(21L)
                        .user(user)
                        .rating(4.8)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(29L).subjectId(118L).name("Economics").build();
        Course course = Course.builder().id(42L).syllabus(syllabus).build();
        TutorClass tutorClass =
                TutorClass.builder()
                        .id(56L)
                        .course(course)
                        .currentStudents(2)
                        .enrollments(new ArrayList<>())
                        .build();

        // Sessions in different months and years
        ClassSession session2024 =
                ClassSession.builder()
                        .id(66L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.of(2024, 12, 15, 10, 0))
                        .endTime(LocalDateTime.of(2024, 12, 15, 11, 0))
                        .build();

        ClassSession session2025Jan =
                ClassSession.builder()
                        .id(67L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.of(2025, 1, 10, 14, 0))
                        .endTime(LocalDateTime.of(2025, 1, 10, 15, 0))
                        .build();

        ClassSession session2025Nov =
                ClassSession.builder()
                        .id(68L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.of(2025, 11, 5, 9, 0))
                        .endTime(LocalDateTime.of(2025, 11, 5, 10, 0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session2024, session2025Jan, session2025Nov));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session2024, session2025Jan, session2025Nov));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getMonthlyStats().size());

        // Should be sorted: 2025-11, 2025-01, 2024-12 (descending)
        assertEquals(2025, result.getMonthlyStats().get(0).getYear());
        assertEquals(11, result.getMonthlyStats().get(0).getMonth());

        assertEquals(2025, result.getMonthlyStats().get(1).getYear());
        assertEquals(1, result.getMonthlyStats().get(1).getMonth());

        assertEquals(2024, result.getMonthlyStats().get(2).getYear());
        assertEquals(12, result.getMonthlyStats().get(2).getMonth());
    }

    @Test
    @DisplayName("Should return null for firstSessionDate when allSessions is empty")
    void should_returnNullFirstSessionDate_whenAllSessionsEmpty() {
        // Given
        String tutorId = "tutor-25";
        User user = User.builder().userId(tutorId).firstName("Wendy").lastName("Scott").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(25L)
                        .user(user)
                        .rating(4.5)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(Collections.emptyList());
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(Collections.emptyList());

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertNull(result.getFirstSessionDate());
    }

    @Test
    @DisplayName("Should return null for lastSessionDate when completedSessions is empty")
    void should_returnNullLastSessionDate_whenCompletedSessionsEmpty() {
        // Given
        String tutorId = "tutor-26";
        User user = User.builder().userId(tutorId).firstName("Xander").lastName("Green").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(26L)
                        .user(user)
                        .rating(4.2)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(32L).subjectId(121L).name("Philosophy").build();
        Course course = Course.builder().id(45L).syllabus(syllabus).build();
        TutorClass tutorClass =
                TutorClass.builder()
                        .id(59L)
                        .course(course)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession upcomingSession =
                ClassSession.builder()
                        .id(73L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().plusDays(5).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().plusDays(5).withHour(11).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(upcomingSession));
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(upcomingSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertNull(result.getLastSessionDate());
        assertNotNull(result.getNextSessionDate());
        assertNotNull(result.getFirstSessionDate());
    }

    @Test
    @DisplayName("Should return null for nextSessionDate when upcomingSessions is empty")
    void should_returnNullNextSessionDate_whenUpcomingSessionsEmpty() {
        // Given
        String tutorId = "tutor-27";
        User user = User.builder().userId(tutorId).firstName("Yara").lastName("Baker").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(27L)
                        .user(user)
                        .rating(4.6)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(33L).subjectId(122L).name("Psychology").build();
        Course course = Course.builder().id(46L).syllabus(syllabus).build();
        TutorClass tutorClass =
                TutorClass.builder()
                        .id(60L)
                        .course(course)
                        .currentStudents(2)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession completedSession =
                ClassSession.builder()
                        .id(74L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(5).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(5).withHour(11).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(completedSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertNull(result.getNextSessionDate());
        assertNotNull(result.getLastSessionDate());
        assertNotNull(result.getFirstSessionDate());
    }

    @Test
    @DisplayName("Should handle all statistics fields together")
    void should_handleAllStatisticsFields_together() {
        // Given
        String tutorId = "tutor-34";
        User user = User.builder().userId(tutorId).firstName("Fiona").lastName("Turner").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(34L)
                        .user(user)
                        .rating(4.9)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        User student1 = User.builder().userId("s1").firstName("S1").lastName("One").build();
        ClassEnrollment e1 = ClassEnrollment.builder().student(student1).build();

        Syllabus syllabus =
                Syllabus.builder().syllabusId(37L).subjectId(126L).name("Archaeology").build();
        Course course = Course.builder().id(50L).syllabus(syllabus).build();
        TutorClass tutorClass =
                TutorClass.builder()
                        .id(66L)
                        .course(course)
                        .currentStudents(5)
                        .enrollments(new ArrayList<>(List.of(e1)))
                        .build();

        ClassSession completedSession =
                ClassSession.builder()
                        .id(79L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(10).withHour(9).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(10).withHour(11).withMinute(0))
                        .build();

        ClassSession upcomingSession =
                ClassSession.builder()
                        .id(80L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0))
                        .endTime(LocalDateTime.now().plusDays(3).withHour(15).withMinute(30))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(upcomingSession));
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(2);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(completedSession, upcomingSession));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals("Fiona Turner", result.getTutorName());
        assertEquals(tutorId, result.getTutorId());
        assertEquals(1, result.getTotalClasses());
        assertEquals(1, result.getActiveClasses());
        assertEquals(0, result.getCompletedClasses());
        assertEquals(5, result.getTotalStudents());
        assertEquals(4.9, result.getAverageRating());
        assertEquals(1, result.getTotalSessionsCompleted());
        assertEquals(1, result.getTotalSessionsUpcoming());
        assertEquals(2, result.getTotalSessionsCancelled());
        assertEquals(2.0, result.getTotalTeachingHours());
        assertNotNull(result.getFirstSessionDate());
        assertNotNull(result.getLastSessionDate());
        assertNotNull(result.getNextSessionDate());
        assertFalse(result.getMonthlyStats().isEmpty());
        assertFalse(result.getSubjectStats().isEmpty());
    }

    @Test
    @DisplayName("Should return Unknown Subject when class has null course in getSubjectName")
    void should_returnUnknownSubject_whenClassHasNullCourse() {
        // Given
        String tutorId = "tutor-35";
        User user = User.builder().userId(tutorId).firstName("George").lastName("Hill").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(35L)
                        .user(user)
                        .rating(4.3)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorClass classWithNullCourse =
                TutorClass.builder()
                        .id(67L)
                        .course(null) // Null course
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(81L)
                        .tutorClass(classWithNullCourse)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId))
                .thenReturn(List.of(classWithNullCourse));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(classWithNullCourse));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSubjectStats().isEmpty()); // No subject can be extracted
    }

    @Test
    @DisplayName("Should return Unknown Subject when class has null syllabus in getSubjectName")
    void should_returnUnknownSubject_whenClassHasNullSyllabus() {
        // Given
        String tutorId = "tutor-36";
        User user = User.builder().userId(tutorId).firstName("Helen").lastName("Adams").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(36L)
                        .user(user)
                        .rating(4.4)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        Course courseWithNullSyllabus = Course.builder().id(51L).syllabus(null).build();

        TutorClass tutorClass =
                TutorClass.builder()
                        .id(68L)
                        .course(courseWithNullSyllabus)
                        .currentStudents(2)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(82L)
                        .tutorClass(tutorClass)
                        .startTime(LocalDateTime.now().minusDays(2).withHour(14).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(2).withHour(15).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(tutorClass));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSubjectStats().isEmpty());
    }

    @Test
    @DisplayName("Should skip class with non-matching subjectId in getSubjectName")
    void should_skipClass_withNonMatchingSubjectId() {
        // Given
        String tutorId = "tutor-37";
        User user = User.builder().userId(tutorId).firstName("Ian").lastName("Evans").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(37L)
                        .user(user)
                        .rating(4.5)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        // Class with subject 200
        Syllabus syllabus1 =
                Syllabus.builder().syllabusId(38L).subjectId(200L).name("Subject 200").build();
        Course course1 = Course.builder().id(52L).syllabus(syllabus1).build();
        TutorClass class1 =
                TutorClass.builder()
                        .id(69L)
                        .course(course1)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        // Class with subject 201 (the one we're looking for)
        Syllabus syllabus2 =
                Syllabus.builder().syllabusId(39L).subjectId(201L).name("Subject 201").build();
        Course course2 = Course.builder().id(53L).syllabus(syllabus2).build();
        TutorClass class2 =
                TutorClass.builder()
                        .id(70L)
                        .course(course2)
                        .currentStudents(2)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(83L)
                        .tutorClass(class2)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId)).thenReturn(List.of(class1, class2));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(class1, class2));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getSubjectStats().size());
        List<String> subjectNames =
                result.getSubjectStats().stream().map(SubjectStatDTO::getSubjectName).toList();
        assertTrue(subjectNames.contains("Subject 200"));
        assertTrue(subjectNames.contains("Subject 201"));
    }

    @Test
    @DisplayName("Should handle session with null course when searching in sessions")
    void should_handleSession_withNullCourseInGetSubjectName() {
        // Given
        String tutorId = "tutor-39";
        User user = User.builder().userId(tutorId).firstName("Kevin").lastName("Reed").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(39L)
                        .user(user)
                        .rating(4.7)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorClass classWithNullCourse =
                TutorClass.builder()
                        .id(73L)
                        .course(null)
                        .currentStudents(1)
                        .enrollments(new ArrayList<>())
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(85L)
                        .tutorClass(classWithNullCourse)
                        .startTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0))
                        .endTime(LocalDateTime.now().minusDays(1).withHour(11).withMinute(0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId))
                .thenReturn(List.of(classWithNullCourse));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(classWithNullCourse));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSubjectStats().isEmpty());
    }

    @Test
    @DisplayName("Should find subject name in sessions loop when not in classes")
    void should_findSubjectName_inSessionsLoop() {
        // Given
        String tutorId = "tutor-40";
        User user = User.builder().userId(tutorId).firstName("Laura").lastName("Murphy").build();

        TutorProfile profile =
                TutorProfile.builder()
                        .id(40L)
                        .user(user)
                        .rating(4.8)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        // Class with subject 116 - will be in classes list but no match
        Syllabus syllabusInClass =
                Syllabus.builder()
                        .syllabusId(41L)
                        .subjectId(116L)
                        .name("Chemistry Syllabus")
                        .build();
        Course courseInClass = Course.builder().id(55L).syllabus(syllabusInClass).build();
        TutorClass classInList =
                TutorClass.builder()
                        .id(41L)
                        .tutor(user)
                        .course(courseInClass)
                        .currentStudents(2)
                        .enrollments(Collections.emptyList())
                        .build();

        // Session with subject 117 - should be found in sessions loop
        Syllabus syllabusInSession =
                Syllabus.builder()
                        .syllabusId(42L)
                        .subjectId(117L)
                        .name("Geography Syllabus")
                        .build();
        Course courseInSession = Course.builder().id(56L).syllabus(syllabusInSession).build();
        TutorClass classForSession =
                TutorClass.builder()
                        .id(42L)
                        .tutor(user)
                        .course(courseInSession)
                        .currentStudents(1)
                        .enrollments(Collections.emptyList())
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(41L)
                        .tutorClass(classForSession)
                        .startTime(LocalDateTime.of(2025, 11, 5, 14, 0))
                        .endTime(LocalDateTime.of(2025, 11, 5, 16, 0))
                        .build();

        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(tutorClassRepository.findByTutorUserId(tutorId))
                .thenReturn(List.of(classInList, classForSession));
        when(tutorClassRepository.findActiveClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(List.of(classInList));
        when(tutorClassRepository.findCompletedClassesByTutor(eq(tutorId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCompletedSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(List.of(session));
        when(sessionRepository.findUpcomingSessionsByTutor(eq(tutorId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.countCancelledSessionsByTutor(tutorId)).thenReturn(0);
        when(sessionRepository.findAllSessionsByTutorOrderByDate(tutorId))
                .thenReturn(List.of(session));

        // When
        TeachingStatisticsDTO result = teachingStatisticsService.getTeachingStatistics(tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getSubjectStats().size());

        // Find the subject stats for subject 117
        SubjectStatDTO subject117 =
                result.getSubjectStats().stream()
                        .filter(s -> s.getSubjectId().equals(117L))
                        .findFirst()
                        .orElse(null);

        assertNotNull(subject117);
        assertEquals("Geography Syllabus", subject117.getSubjectName());
        assertEquals(1, subject117.getClassCount()); // classForSession is now in the list
        assertEquals(1, subject117.getSessionCount());
    }
}
