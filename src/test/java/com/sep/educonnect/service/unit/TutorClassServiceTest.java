package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.attendance.AttendanceRequest;
import com.sep.educonnect.dto.attendance.AttendanceResponse;
import com.sep.educonnect.dto.attendance.SessionAttendanceDTO;
import com.sep.educonnect.dto.classenrollment.ClassStudentResponse;
import com.sep.educonnect.dto.classenrollment.StudentClassResponse;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.student.StudentScheduleDTO;
import com.sep.educonnect.dto.tutor.request.CreateTutorClassRequest;
import com.sep.educonnect.dto.tutor.request.WeeklySchedule;
import com.sep.educonnect.dto.tutor.response.TutorClassResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.TutorClassMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.NotificationService;
import com.sep.educonnect.service.ProgressService;
import com.sep.educonnect.service.TutorClassService;
import com.sep.educonnect.service.email.MailService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TutorClassServiceTest {

    @Mock private TutorClassRepository tutorClassRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private TutorAvailabilityRepository tutorAvailabilityRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorClassMapper tutorClassMapper;
    @Mock private MailService mailService;
    @Mock private SessionAttendanceRepository sessionAttendanceRepository;
    @Mock private NotificationService notificationService;
    @Mock private ScheduleChangeRepository scheduleChangeRepository;
    @Mock private ProgressService progressService;
    @Mock private BookingMemberRepository bookingMemberRepository;

    @InjectMocks private TutorClassService tutorClassService;

    // ==================== createTutorClass TESTS ====================

    @Test
    @DisplayName("Should create tutor class successfully")
    void should_createTutorClass_successfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        // Use a future Monday to ensure availability (next Monday from today)
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (8 - today.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7; // If today is Monday, use next Monday
        LocalDate startDate = today.plusDays(daysUntilMonday);

        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .description("Basic Math")
                .maxStudents(10)
                .startDate(startDate)
                .totalSessions(2)
                .weeklySchedules(List.of(
                        WeeklySchedule.builder()
                                .dayOfWeek(1) // Monday
                                .slotNumbers(List.of(1))
                                .build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2, 3))); // Available on Monday slots 1,2,3

        Course course = Course.builder().id(1L).name("Math").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(tutorClassRepository.save(any(TutorClass.class))).thenAnswer(i -> i.getArgument(0));
        when(classSessionRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        TutorClass result = tutorClassService.createTutorClass(request);

        // Then
        assertNotNull(result);
        assertEquals("Math Class", result.getTitle());
        assertEquals(10, result.getMaxStudents());
        assertEquals(startDate, result.getStartDate());
        verify(tutorClassRepository).save(any(TutorClass.class));
        verify(classSessionRepository).saveAll(anyList());
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should throw TUTOR_AVAILABILITY_NOT_SET when tutor has no availability")
    void should_throwTutorAvailabilityNotSet_when_noAvailability() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(LocalDate.now().plusDays(1))
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.TUTOR_AVAILABILITY_NOT_SET, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw COURSE_NOT_EXISTED when course does not exist")
    void should_throwCourseNotExisted_when_invalidCourseId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(999L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(LocalDate.now().plusDays(1))
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw INVALID_CLASS_SIZE when maxStudents is zero or negative")
    void should_throwInvalidClassSize_when_maxStudentsInvalid() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(0)
                .startDate(LocalDate.now().plusDays(1))
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.INVALID_CLASS_SIZE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw INVALID_NUMBER_OF_SESSION when totalSessions is zero or negative")
    void should_throwInvalidNumberOfSession_when_totalSessionsInvalid() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(LocalDate.now().plusDays(1))
                .totalSessions(0)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.INVALID_NUMBER_OF_SESSION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw INVALID_START_DATE when startDate is in the past")
    void should_throwInvalidStartDate_when_startDateInPast() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(LocalDate.now().minusDays(1))
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.INVALID_START_DATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw TUTOR_NOT_AVAILABLE when tutor does not work on scheduled day")
    void should_throwTutorNotAvailable_when_notWorkOnDay() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        // Use a future Monday to ensure it's not in the past
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (8 - today.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7; // If today is Monday, use next Monday
        LocalDate startDate = today.plusDays(daysUntilMonday);

        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(startDate)
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(1)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        // Tutor does not work on Monday
        availability.setSlotsByDay(1, new ArrayList<>()); // Empty slots

        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.TUTOR_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw TUTOR_NOT_AVAILABLE when tutor not available for requested slot")
    void should_throwTutorNotAvailable_when_slotNotAvailable() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        // Use a future Monday to ensure it's not in the past
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (8 - today.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7; // If today is Monday, use next Monday
        LocalDate startDate = today.plusDays(daysUntilMonday);

        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("Math Class")
                .maxStudents(10)
                .startDate(startDate)
                .totalSessions(1)
                .weeklySchedules(List.of(WeeklySchedule.builder().dayOfWeek(1).slotNumbers(List.of(3)).build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2))); // Available slots 1,2 but not 3

        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorClassService.createTutorClass(request));
        assertEquals(ErrorCode.TUTOR_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should create class with multiple sessions and update availability")
    void should_createClass_withMultipleSessions_andUpdateAvailability() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        // Use a future Monday to ensure it's not in the past
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (8 - today.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7; // If today is Monday, use next Monday
        LocalDate startDate = today.plusDays(daysUntilMonday);

        CreateTutorClassRequest request = CreateTutorClassRequest.builder()
                .courseId(1L)
                .title("English Class")
                .maxStudents(15)
                .startDate(startDate)
                .totalSessions(3)
                .weeklySchedules(List.of(
                        WeeklySchedule.builder()
                                .dayOfWeek(1) // Monday
                                .slotNumbers(List.of(1, 2))
                                .build()))
                .build();

        User tutor = User.builder().userId("tutor").build();
        TutorAvailability availability = TutorAvailability.builder().user(tutor).build();
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2, 3, 4))); // Available slots 1,2,3,4

        Course course = Course.builder().id(1L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor")).thenReturn(Optional.of(availability));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(tutorClassRepository.save(any(TutorClass.class))).thenAnswer(i -> i.getArgument(0));
        when(classSessionRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        TutorClass result = tutorClassService.createTutorClass(request);

        // Then
        assertNotNull(result);
        assertEquals("English Class", result.getTitle());
        verify(tutorClassRepository).save(any(TutorClass.class));
        verify(classSessionRepository).saveAll(anyList());
        verify(tutorAvailabilityRepository).save(availability);

        // Verify availability updated - slots 1 and 2 should be removed from Monday
        List<Integer> remainingSlots = availability.getSlotsByDay(1);
        assertFalse(remainingSlots.contains(1));
        assertFalse(remainingSlots.contains(2));
        assertTrue(remainingSlots.contains(3));
        assertTrue(remainingSlots.contains(4));
    }

    @Test
    @DisplayName("Should get tutor classes successfully")
    void should_getTutorClasses_successfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor").build();
        TutorClass tutorClass = TutorClass.builder().id(1L).sessions(new ArrayList<>()).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByTutorUserId("tutor")).thenReturn(List.of(tutorClass));
        when(tutorClassMapper.toTutorBasicDTO(any())).thenReturn(null);
        when(tutorClassMapper.toCourseDTO(any())).thenReturn(null);

        // When
        List<TutorClassResponse> result = tutorClassService.getTutorClasses(LocalDate.now());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get tutor class by class id successfully")
    void should_getTutorClassByClassId_when_validClassId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor").build();
        TutorClass tutorClass = TutorClass.builder().id(1L).title("Class 1").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(1L, "tutor"))
                .thenReturn(Optional.of(tutorClass));

        // When
        TutorClass result = tutorClassService.getTutorClassByClassId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Class 1", result.getTitle());
        verify(userRepository).findByUsername("tutor");
        verify(tutorClassRepository).findByIdAndTutorUserId(1L, "tutor");
    }

    @Test
    @DisplayName("Should throw CLASS_NOT_FOUND when class id does not exist")
    void should_throwClassNotFound_when_invalidClassId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(0L, "tutor")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getTutorClassByClassId(0L));

        verify(userRepository).findByUsername("tutor");
        verify(tutorClassRepository).findByIdAndTutorUserId(0L, "tutor");
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user is not found")
    void should_throwUserNotExisted_when_userNotFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getTutorClassByClassId(1L));

        verify(userRepository).findByUsername("tutor");
        verify(tutorClassRepository, never()).findByIdAndTutorUserId(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should get tutor class by id successfully")
    void should_getTutorClassById_when_validClassId() {
        // Given
        TutorClass tutorClass = TutorClass.builder().id(1L).title("Class 1").build();

        when(tutorClassRepository.findById(1L)).thenReturn(Optional.of(tutorClass));

        // When
        TutorClass result = tutorClassService.getTutorClassById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Class 1", result.getTitle());
        verify(tutorClassRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw CLASS_NOT_FOUND when class id does not exist for getTutorClassById")
    void should_throwClassNotFound_when_invalidClassIdForGetById() {
        // Given
        when(tutorClassRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getTutorClassById(0L));

        verify(tutorClassRepository).findById(0L);
    }

    @Test
    @DisplayName("Should invite students successfully")
    void should_inviteStudents_successfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor =
                User.builder()
                        .userId("tutor")
                        .email("tutor@test.com")
                        .firstName("T")
                        .lastName("T")
                        .build();
        Course course = Course.builder().name("Math Course").build();
        TutorClass tutorClass = TutorClass.builder().id(1L).title("Class").course(course).build();
        User student =
                User.builder()
                        .userId("student")
                        .email("student@test.com")
                        .firstName("S")
                        .lastName("S")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(1L, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(userRepository.findByIdAndNotDeleted("student")).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(
                        i -> {
                            ClassEnrollment e = i.getArgument(0);
                            e.setId(100L);
                            return e;
                        });
        when(classEnrollmentRepository.countStudentsInClass(1L)).thenReturn(1);

        // When
        tutorClassService.inviteStudents(1L, List.of("student"));

        // Then
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
        verify(progressService).createCourseProgress(100L);
        verify(mailService).send(any(), any(), any());
        verify(classEnrollmentRepository).countStudentsInClass(1L);
    }

    @Test
    @DisplayName("Should get students to invite successfully")
    void should_getStudentsToInvite_successfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 1L;
        Long courseId = 10L;

        User tutor = User.builder().userId("tutor").build();
        Course course = Course.builder().id(courseId).build();
        TutorClass tutorClass = TutorClass.builder().id(classId).course(course).build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@test.com")
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .firstName("Jane")
                        .lastName("Smith")
                        .email("jane@test.com")
                        .build();

        BookingMember member1 = BookingMember.builder().userId("student1").build();
        BookingMember member2 = BookingMember.builder().userId("student2").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId))
                .thenReturn(List.of(member1, member2));
        when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
                .thenReturn(Set.of());
        when(userRepository.findByIdAndNotDeleted("student1")).thenReturn(Optional.of(student1));
        when(userRepository.findByIdAndNotDeleted("student2")).thenReturn(Optional.of(student2));

        // When
        List<StudentGeneralResponse> result = tutorClassService.getStudentsToInvite(classId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        StudentGeneralResponse dto1 = result.getFirst();
        assertEquals("student1", dto1.getUserId());
        assertEquals("John Doe", dto1.getName());
        assertEquals("john@test.com", dto1.getEmail());

        StudentGeneralResponse dto2 = result.get(1);
        assertEquals("student2", dto2.getUserId());
        assertEquals("Jane Smith", dto2.getName());
        assertEquals("jane@test.com", dto2.getEmail());

        verify(userRepository).findByUsername("tutor");
        verify(tutorClassRepository).findByIdAndTutorUserId(classId, "tutor");
        verify(bookingMemberRepository).findPaidOrApprovedTrialByCourseId(courseId);
        verify(classEnrollmentRepository).findDistinctStudentUserIdsByCourseId(courseId);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found for getStudentsToInvite")
    void should_throwUserNotExisted_when_tutorNotFoundForGetStudentsToInvite() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        Long classId = 1L;

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getStudentsToInvite(classId));

        verify(userRepository).findByUsername("nonexistent");
        verify(tutorClassRepository, never()).findByIdAndTutorUserId(anyLong(), anyString());
        verify(bookingMemberRepository, never()).findPaidOrApprovedTrialByCourseId(anyLong());
    }

    @Test
    @DisplayName("Should throw CLASS_NOT_FOUND when class not found for getStudentsToInvite")
    void should_throwClassNotFound_when_classNotFoundForGetStudentsToInvite() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 999L;

        User tutor = User.builder().userId("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getStudentsToInvite(classId));

        verify(userRepository).findByUsername("tutor");
        verify(tutorClassRepository).findByIdAndTutorUserId(classId, "tutor");
        verify(bookingMemberRepository, never()).findPaidOrApprovedTrialByCourseId(anyLong());
    }

    @Test
    @DisplayName("Should filter out already enrolled students")
    void should_filterOutEnrolledStudents_forGetStudentsToInvite() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 1L;
        Long courseId = 10L;

        User tutor = User.builder().userId("tutor").build();
        Course course = Course.builder().id(courseId).build();
        TutorClass tutorClass = TutorClass.builder().id(classId).course(course).build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@test.com")
                        .build();

        BookingMember member1 = BookingMember.builder().userId("student1").build();
        BookingMember member2 = BookingMember.builder().userId("student2").build(); // Already enrolled

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId))
                .thenReturn(List.of(member1, member2));
        when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
                .thenReturn(Set.of("student2")); // student2 already enrolled
        when(userRepository.findByIdAndNotDeleted("student1")).thenReturn(Optional.of(student1));

        // When
        List<StudentGeneralResponse> result = tutorClassService.getStudentsToInvite(classId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("student1", result.get(0).getUserId());

        verify(bookingMemberRepository).findPaidOrApprovedTrialByCourseId(courseId);
        verify(classEnrollmentRepository).findDistinctStudentUserIdsByCourseId(courseId);
        verify(userRepository, times(1)).findByIdAndNotDeleted("student1");
        verify(userRepository, never()).findByIdAndNotDeleted("student2");
    }

    @Test
    @DisplayName("Should return empty list when no booking members found")
    void should_returnEmptyList_when_noBookingMembersFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 1L;
        Long courseId = 10L;

        User tutor = User.builder().userId("tutor").build();
        Course course = Course.builder().id(courseId).build();
        TutorClass tutorClass = TutorClass.builder().id(classId).course(course).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId)).thenReturn(List.of());
        when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
                .thenReturn(Set.of());

        // When
        List<StudentGeneralResponse> result = tutorClassService.getStudentsToInvite(classId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());

        verify(bookingMemberRepository).findPaidOrApprovedTrialByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return empty list when all booking members already enrolled")
    void should_returnEmptyList_when_allMembersAlreadyEnrolled() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 1L;
        Long courseId = 10L;

        User tutor = User.builder().userId("tutor").build();
        Course course = Course.builder().id(courseId).build();
        TutorClass tutorClass = TutorClass.builder().id(classId).course(course).build();

        BookingMember member1 = BookingMember.builder().userId("student1").build();
        BookingMember member2 = BookingMember.builder().userId("student2").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId))
                .thenReturn(List.of(member1, member2));
        when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
                .thenReturn(Set.of("student1", "student2")); // All already enrolled

        // When
        List<StudentGeneralResponse> result = tutorClassService.getStudentsToInvite(classId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());

        verify(bookingMemberRepository).findPaidOrApprovedTrialByCourseId(courseId);
        verify(classEnrollmentRepository).findDistinctStudentUserIdsByCourseId(courseId);
        verify(userRepository, never()).findByIdAndNotDeleted(anyString());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when student in booking member not found")
    void should_throwUserNotExisted_when_studentInBookingMemberNotFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        Long classId = 1L;
        Long courseId = 10L;

        User tutor = User.builder().userId("tutor").build();
        Course course = Course.builder().id(courseId).build();
        TutorClass tutorClass = TutorClass.builder().id(classId).course(course).build();

        BookingMember member1 =
                BookingMember.builder().userId("deletedStudent").build(); // Student deleted

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorClassRepository.findByIdAndTutorUserId(classId, "tutor"))
                .thenReturn(Optional.of(tutorClass));
        when(bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId))
                .thenReturn(List.of(member1));
        when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
                .thenReturn(Set.of());
        when(userRepository.findByIdAndNotDeleted("deletedStudent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.getStudentsToInvite(classId));

        verify(userRepository).findByIdAndNotDeleted("deletedStudent");
    }

    @Test
    @DisplayName("Should create attendance successfully")
    void should_createAttendance_successfully() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(1L);
        request.setSessionId(10L);
        request.setAttended(true);

        ClassEnrollment enrollment =
                ClassEnrollment.builder()
                        .id(1L)
                        .student(
                                User.builder()
                                        .userId("student")
                                        .firstName("S")
                                        .lastName("S")
                                        .build())
                        .build();
        ClassSession session = ClassSession.builder().id(10L).build();

        when(classEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.saveAll(anyList())).thenReturn(List.of());
        when(classSessionRepository.findAllById(anySet())).thenReturn(List.of(session));
        when(classEnrollmentRepository.findAllById(anySet())).thenReturn(List.of(enrollment));

        // When
        List<AttendanceResponse> result = tutorClassService.createAttendance(List.of(request));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationService)
                .createAndSendNotification(eq("student"), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ENROLLMENT_NOT_EXISTED when enrollment does not exist")
    void should_throwEnrollmentNotExisted_when_enrollmentNotFound() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(999L);
        request.setSessionId(10L);
        request.setAttended(true);

        when(classEnrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.createAttendance(List.of(request)));

        verify(classEnrollmentRepository).findById(999L);
        verify(sessionAttendanceRepository, never()).saveAll(anyList());
        verify(notificationService, never())
                .createAndSendNotification(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw SESSION_NOT_EXISTED when session does not exist")
    void should_throwSessionNotExisted_when_sessionNotFound() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(1L);
        request.setSessionId(999L);
        request.setAttended(true);

        ClassEnrollment enrollment =
                ClassEnrollment.builder()
                        .id(1L)
                        .student(User.builder().userId("student").build())
                        .build();

        when(classEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(classSessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.createAttendance(List.of(request)));

        verify(classEnrollmentRepository).findById(1L);
        verify(classSessionRepository).findById(999L);
        verify(sessionAttendanceRepository, never()).saveAll(anyList());
        verify(notificationService, never())
                .createAndSendNotification(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should create multiple attendances successfully")
    void should_createMultipleAttendances_successfully() {
        // Given
        AttendanceRequest request1 = new AttendanceRequest();
        request1.setEnrollmentId(1L);
        request1.setSessionId(10L);
        request1.setAttended(true);

        AttendanceRequest request2 = new AttendanceRequest();
        request2.setEnrollmentId(2L);
        request2.setSessionId(10L);
        request2.setAttended(false);

        ClassEnrollment enrollment1 =
                ClassEnrollment.builder()
                        .id(1L)
                        .student(User.builder().userId("student1").build())
                        .build();

        ClassEnrollment enrollment2 =
                ClassEnrollment.builder()
                        .id(2L)
                        .student(User.builder().userId("student2").build())
                        .build();

        ClassSession session = ClassSession.builder().id(10L).build();

        when(classEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment1));
        when(classEnrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment2));
        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.saveAll(anyList())).thenReturn(List.of());
        when(classSessionRepository.findAllById(anySet())).thenReturn(List.of(session));
        when(classEnrollmentRepository.findAllById(anySet()))
                .thenReturn(List.of(enrollment1, enrollment2));

        // When
        List<AttendanceResponse> result =
                tutorClassService.createAttendance(List.of(request1, request2));

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sessionAttendanceRepository).saveAll(anyList());
        verify(notificationService, times(2))
                .createAndSendNotification(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should update attendance successfully")
    void should_updateAttendance_successfully() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(1L);
        request.setSessionId(10L);
        request.setAttended(false);
        request.setNotes("Updated notes");

        User student = User.builder().userId("student1").firstName("John").lastName("Doe").build();

        ClassEnrollment enrollment = ClassEnrollment.builder().id(1L).student(student).build();

        TutorClass tutorClass = TutorClass.builder().title("Math Class").build();

        ClassSession session = ClassSession.builder().id(10L).tutorClass(tutorClass).build();

        SessionAttendance existingAttendance =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session)
                        .enrollment(enrollment)
                        .attended(true)
                        .notes("Old notes")
                        .build();

        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.findBySessionId(10L))
                .thenReturn(List.of(existingAttendance));
        when(sessionAttendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(existingAttendance));

        // When
        List<AttendanceResponse> result = tutorClassService.updateAttendance(List.of(request));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(false, existingAttendance.getAttended());
        assertEquals("Updated notes", existingAttendance.getNotes());
        verify(sessionAttendanceRepository).saveAll(anyList());
        verify(notificationService)
                .createAndSendNotification(
                        eq("student1"), contains("updated"), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw SESSION_NOT_EXISTED when session does not exist for update")
    void should_throwSessionNotExisted_when_sessionNotFoundForUpdate() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(1L);
        request.setSessionId(999L);
        request.setAttended(false);

        when(classSessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tutorClassService.updateAttendance(List.of(request)));

        verify(classSessionRepository).findById(999L);
        verify(sessionAttendanceRepository, never()).findBySessionId(anyLong());
        verify(sessionAttendanceRepository, never()).saveAll(anyList());
        verify(notificationService, never())
                .createAndSendNotification(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should update multiple attendances successfully")
    void should_updateMultipleAttendances_successfully() {
        // Given
        AttendanceRequest request1 = new AttendanceRequest();
        request1.setEnrollmentId(1L);
        request1.setSessionId(10L);
        request1.setAttended(true);

        AttendanceRequest request2 = new AttendanceRequest();
        request2.setEnrollmentId(2L);
        request2.setSessionId(10L);
        request2.setAttended(false);

        User student1 = User.builder().userId("student1").firstName("John").lastName("Doe").build();
        User student2 =
                User.builder().userId("student2").firstName("Jane").lastName("Smith").build();

        ClassEnrollment enrollment1 = ClassEnrollment.builder().id(1L).student(student1).build();
        ClassEnrollment enrollment2 = ClassEnrollment.builder().id(2L).student(student2).build();

        TutorClass tutorClass = TutorClass.builder().title("Math Class").build();
        ClassSession session = ClassSession.builder().id(10L).tutorClass(tutorClass).build();

        SessionAttendance attendance1 =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session)
                        .enrollment(enrollment1)
                        .attended(false)
                        .build();

        SessionAttendance attendance2 =
                SessionAttendance.builder()
                        .id(2L)
                        .session(session)
                        .enrollment(enrollment2)
                        .attended(true)
                        .build();

        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.findBySessionId(10L))
                .thenReturn(List.of(attendance1, attendance2));
        when(sessionAttendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(attendance1, attendance2));

        // When
        List<AttendanceResponse> result =
                tutorClassService.updateAttendance(List.of(request1, request2));

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(true, attendance1.getAttended());
        assertEquals(false, attendance2.getAttended());
        verify(sessionAttendanceRepository).saveAll(anyList());
        verify(notificationService, times(2))
                .createAndSendNotification(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle update when enrollment not found in existing attendances")
    void should_handleUpdate_when_enrollmentNotFoundInExisting() {
        // Given
        AttendanceRequest request1 = new AttendanceRequest();
        request1.setEnrollmentId(1L);
        request1.setSessionId(10L);
        request1.setAttended(true);

        AttendanceRequest request2 = new AttendanceRequest();
        request2.setEnrollmentId(999L); // This enrollment doesn't exist
        request2.setSessionId(10L);
        request2.setAttended(false);

        User student = User.builder().userId("student1").firstName("John").lastName("Doe").build();
        ClassEnrollment enrollment = ClassEnrollment.builder().id(1L).student(student).build();
        TutorClass tutorClass = TutorClass.builder().title("Math Class").build();
        ClassSession session = ClassSession.builder().id(10L).tutorClass(tutorClass).build();

        SessionAttendance existingAttendance =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session)
                        .enrollment(enrollment)
                        .attended(false)
                        .build();

        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.findBySessionId(10L))
                .thenReturn(List.of(existingAttendance));
        when(sessionAttendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(existingAttendance));

        // When
        List<AttendanceResponse> result =
                tutorClassService.updateAttendance(List.of(request1, request2));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(true, existingAttendance.getAttended());
        verify(sessionAttendanceRepository).saveAll(anyList());
        verify(notificationService, times(1))
                .createAndSendNotification(eq("student1"), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should update attendance with null tutorClass in session")
    void should_updateAttendance_when_tutorClassIsNull() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(1L);
        request.setSessionId(10L);
        request.setAttended(true);

        User student = User.builder().userId("student1").firstName("John").lastName("Doe").build();
        ClassEnrollment enrollment = ClassEnrollment.builder().id(1L).student(student).build();
        ClassSession session =
                ClassSession.builder().id(10L).tutorClass(null).build(); // null tutorClass

        SessionAttendance existingAttendance =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session)
                        .enrollment(enrollment)
                        .attended(false)
                        .build();

        when(classSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.findBySessionId(10L))
                .thenReturn(List.of(existingAttendance));
        when(sessionAttendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(existingAttendance));

        // When
        List<AttendanceResponse> result = tutorClassService.updateAttendance(List.of(request));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(true, existingAttendance.getAttended());
        verify(notificationService)
                .createAndSendNotification(
                        eq("student1"), contains("Session"), any(), any(), any());
    }

    @Test
    @DisplayName("Should get student schedule successfully without schedule changes")
    void should_getStudentSchedule_successfully_withoutChanges() {
        // Given
        String studentId = "student1";
        LocalDate fromDate = LocalDate.of(2025, 12, 1);
        LocalDate toDate = LocalDate.of(2025, 12, 31);

        User tutor = User.builder().firstName("John").lastName("Doe").build();
        Course course = Course.builder().name("Mathematics").build();
        TutorClass tutorClass =
                TutorClass.builder().id(1L).title("Math Class").tutor(tutor).course(course).build();

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 5))
                        .slotNumber(1)
                        .sessionNumber(1)
                        .topic("Algebra")
                        .meetingJoinUrl("http://meeting1")
                        .meetingPassword("pass1")
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(2L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 10))
                        .slotNumber(2)
                        .sessionNumber(2)
                        .topic("Geometry")
                        .meetingJoinUrl("http://meeting2")
                        .meetingPassword("pass2")
                        .build();

        when(classSessionRepository.findStudentSchedule(studentId, fromDate, toDate))
                .thenReturn(List.of(session1, session2));
        when(scheduleChangeRepository.findApprovedChangesBySessionIds(anyList()))
                .thenReturn(List.of());

        // When
        List<StudentScheduleDTO> result =
                tutorClassService.getStudentSchedule(studentId, fromDate, toDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        StudentScheduleDTO dto1 = result.getFirst();
        assertEquals(1L, dto1.getSessionId());
        assertEquals("Math Class", dto1.getClassName());
        assertEquals("Mathematics", dto1.getCourseName());
        assertEquals(LocalDate.of(2025, 12, 5), dto1.getSessionDate());
        assertEquals(1, dto1.getSlotNumber());
        assertEquals(false, dto1.getHasScheduleChange());

        StudentScheduleDTO dto2 = result.get(1);
        assertEquals(2L, dto2.getSessionId());
        assertEquals(LocalDate.of(2025, 12, 10), dto2.getSessionDate());
        assertEquals(2, dto2.getSlotNumber());
        assertEquals(false, dto2.getHasScheduleChange());

        verify(classSessionRepository).findStudentSchedule(studentId, fromDate, toDate);
        verify(scheduleChangeRepository).findApprovedChangesBySessionIds(anyList());
    }

    @Test
    @DisplayName("Should get student schedule successfully with schedule changes")
    void should_getStudentSchedule_successfully_withScheduleChanges() {
        // Given
        String studentId = "student1";
        LocalDate fromDate = LocalDate.of(2025, 12, 1);
        LocalDate toDate = LocalDate.of(2025, 12, 31);

        User tutor = User.builder().firstName("John").lastName("Doe").build();
        Course course = Course.builder().name("Mathematics").build();
        TutorClass tutorClass =
                TutorClass.builder().id(1L).title("Math Class").tutor(tutor).course(course).build();

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 5))
                        .slotNumber(1)
                        .sessionNumber(1)
                        .topic("Algebra")
                        .meetingJoinUrl("http://meeting1")
                        .meetingPassword("pass1")
                        .build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(1L)
                        .session(session1)
                        .oldDate(LocalDate.of(2025, 12, 5))
                        .newDate(LocalDate.of(2025, 12, 6))
                        .newSLot(3)
                        .status("APPROVED")
                        .build();

        when(classSessionRepository.findStudentSchedule(studentId, fromDate, toDate))
                .thenReturn(List.of(session1));
        when(scheduleChangeRepository.findApprovedChangesBySessionIds(anyList()))
                .thenReturn(List.of(scheduleChange));

        // When
        List<StudentScheduleDTO> result =
                tutorClassService.getStudentSchedule(studentId, fromDate, toDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        StudentScheduleDTO dto = result.getFirst();
        assertEquals(1L, dto.getSessionId());
        assertEquals(true, dto.getHasScheduleChange());
        assertEquals(LocalDate.of(2025, 12, 5), dto.getOriginalDate());
        assertEquals(1, dto.getOriginalSlot());
        assertEquals(LocalDate.of(2025, 12, 6), dto.getNewDate());
        assertEquals(3, dto.getNewSlot());
        // Should display new date and slot
        assertEquals(LocalDate.of(2025, 12, 6), dto.getSessionDate());
        assertEquals(3, dto.getSlotNumber());

        verify(classSessionRepository).findStudentSchedule(studentId, fromDate, toDate);
        verify(scheduleChangeRepository).findApprovedChangesBySessionIds(anyList());
    }

    @Test
    @DisplayName("Should return empty list when no sessions found in date range")
    void should_returnEmptyList_when_noSessionsInDateRange() {
        // Given
        String studentId = "student1";
        LocalDate fromDate = LocalDate.of(2025, 12, 1);
        LocalDate toDate = LocalDate.of(2025, 12, 31);

        when(classSessionRepository.findStudentSchedule(studentId, fromDate, toDate))
                .thenReturn(List.of());

        // When
        List<StudentScheduleDTO> result =
                tutorClassService.getStudentSchedule(studentId, fromDate, toDate);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());

        verify(classSessionRepository).findStudentSchedule(studentId, fromDate, toDate);
        verify(scheduleChangeRepository, never()).findApprovedChangesBySessionIds(anyList());
    }

    @Test
    @DisplayName("Should get student attendance in class successfully")
    void should_getStudentAttendanceInClass_successfully() {
        // Given
        MockHelper.mockSecurityContext("student1");
        Long classId = 1L;

        User student = User.builder().userId("student1").firstName("John").lastName("Doe").build();

        ClassEnrollment enrollment = ClassEnrollment.builder().id(1L).student(student).build();

        TutorClass tutorClass = TutorClass.builder().id(classId).title("Math Class").build();

        ClassSession session1 =
                ClassSession.builder()
                        .id(10L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 5))
                        .sessionNumber(1)
                        .topic("Algebra")
                        .startTime(null)
                        .endTime(null)
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(11L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 10))
                        .sessionNumber(2)
                        .topic("Geometry")
                        .startTime(null)
                        .endTime(null)
                        .build();

        SessionAttendance attendance1 =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session1)
                        .enrollment(enrollment)
                        .attended(true)
                        .notes("Present")
                        .build();

        SessionAttendance attendance2 =
                SessionAttendance.builder()
                        .id(2L)
                        .session(session2)
                        .enrollment(enrollment)
                        .attended(false)
                        .notes("Absent")
                        .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(sessionAttendanceRepository.findByStudentAndClass("student1", classId))
                .thenReturn(List.of(attendance1, attendance2));

        // When
        List<SessionAttendanceDTO> result = tutorClassService.getStudentAttendanceInClass(classId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        SessionAttendanceDTO dto1 = result.getFirst();
        assertEquals(1L, dto1.getId());
        assertEquals(10L, dto1.getSessionId());
        assertEquals(true, dto1.getAttended());
        assertEquals("Present", dto1.getNotes());
        assertEquals("Algebra", dto1.getTopic());
        assertEquals(classId, dto1.getClassId());
        assertEquals("Math Class", dto1.getClassName());

        SessionAttendanceDTO dto2 = result.get(1);
        assertEquals(2L, dto2.getId());
        assertEquals(11L, dto2.getSessionId());
        assertEquals(false, dto2.getAttended());
        assertEquals("Absent", dto2.getNotes());

        verify(userRepository).findByUsername("student1");
        verify(sessionAttendanceRepository).findByStudentAndClass("student1", classId);
    }

    @Test
    @DisplayName("Should get student attendance with complete session details")
    void should_getStudentAttendance_withCompleteSessionDetails() {
        // Given
        MockHelper.mockSecurityContext("student1");
        Long classId = 1L;

        User student = User.builder().userId("student1").firstName("John").lastName("Doe").build();

        ClassEnrollment enrollment = ClassEnrollment.builder().id(1L).student(student).build();

        TutorClass tutorClass = TutorClass.builder().id(classId).title("Math Class").build();

        ClassSession session =
                ClassSession.builder()
                        .id(10L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.of(2025, 12, 5))
                        .sessionNumber(1)
                        .topic("Algebra")
                        .startTime(null)
                        .endTime(null)
                        .build();

        SessionAttendance attendance =
                SessionAttendance.builder()
                        .id(1L)
                        .session(session)
                        .enrollment(enrollment)
                        .attended(true)
                        .notes("Present and participated")
                        .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(sessionAttendanceRepository.findByStudentAndClass("student1", classId))
                .thenReturn(List.of(attendance));

        // When
        List<SessionAttendanceDTO> result = tutorClassService.getStudentAttendanceInClass(classId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        SessionAttendanceDTO dto = result.getFirst();
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getSessionId());
        assertEquals(1L, dto.getEnrollmentId());
        assertEquals("student1", dto.getStudentId());
        assertEquals("JohnDoe", dto.getStudentName());
        assertEquals(LocalDate.of(2025, 12, 5), dto.getSessionDate());
        assertEquals(1, dto.getSessionNumber());
        assertEquals("Algebra", dto.getTopic());
        assertEquals(true, dto.getAttended());
        assertEquals("Present and participated", dto.getNotes());
        assertEquals(classId, dto.getClassId());
        assertEquals("Math Class", dto.getClassName());

        verify(userRepository).findByUsername("student1");
        verify(sessionAttendanceRepository).findByStudentAndClass("student1", classId);
    }

    @Test
    @DisplayName("Should get class students successfully")
    void should_getClassStudents_successfully() {
        // Given
        Long classId = 1L;

        TutorClass tutorClass =
                TutorClass.builder().id(classId).title("Math Class").maxStudents(10).build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .avatar("avatar1.jpg")
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .firstName("Jane")
                        .lastName("Smith")
                        .email("jane@example.com")
                        .avatar("avatar2.jpg")
                        .build();

        ClassEnrollment enrollment1 =
                ClassEnrollment.builder()
                        .id(1L)
                        .tutorClass(tutorClass)
                        .student(student1)
                        .enrolledAt(LocalDateTime.of(2025, 12, 1, 10, 0))
                        .notes("Good student")
                        .hasJoined(true)
                        .build();

        ClassEnrollment enrollment2 =
                ClassEnrollment.builder()
                        .id(2L)
                        .tutorClass(tutorClass)
                        .student(student2)
                        .enrolledAt(LocalDateTime.of(2025, 12, 2, 11, 0))
                        .notes("Excellent performance")
                        .hasJoined(false)
                        .build();

        when(tutorClassRepository.findById(classId)).thenReturn(Optional.of(tutorClass));
        when(classEnrollmentRepository.findByTutorClassIdOrderByEnrolledAtAsc(classId))
                .thenReturn(List.of(enrollment1, enrollment2));

        // When
        List<ClassStudentResponse> result = tutorClassService.getClassStudents(classId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first student
        ClassStudentResponse response1 = result.getFirst();
        assertEquals(1L, response1.getEnrollmentId());
        assertEquals("student1", response1.getStudent().getUserId());
        assertEquals("John", response1.getStudent().getFirstName());
        assertEquals("Doe", response1.getStudent().getLastName());
        assertEquals("john@example.com", response1.getStudent().getEmail());
        assertEquals("avatar1.jpg", response1.getStudent().getAvatar());
        assertEquals(LocalDateTime.of(2025, 12, 1, 10, 0), response1.getEnrolledAt());
        assertEquals("Good student", response1.getNotes());
        assertEquals(true, response1.getHasJoined());

        // Verify second student
        ClassStudentResponse response2 = result.get(1);
        assertEquals(2L, response2.getEnrollmentId());
        assertEquals("student2", response2.getStudent().getUserId());
        assertEquals("Jane", response2.getStudent().getFirstName());
        assertEquals("Smith", response2.getStudent().getLastName());
        assertEquals("jane@example.com", response2.getStudent().getEmail());
        assertEquals("avatar2.jpg", response2.getStudent().getAvatar());
        assertEquals(LocalDateTime.of(2025, 12, 2, 11, 0), response2.getEnrolledAt());
        assertEquals("Excellent performance", response2.getNotes());
        assertEquals(false, response2.getHasJoined());

        verify(tutorClassRepository).findById(classId);
        verify(classEnrollmentRepository).findByTutorClassIdOrderByEnrolledAtAsc(classId);
    }

    @Test
    @DisplayName("Should throw CLASS_NOT_FOUND when class does not exist")
    void should_throwClassNotFound_when_classDoesNotExist() {
        // Given
        Long classId = 999L;

        when(tutorClassRepository.findById(classId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorClassService.getClassStudents(classId));

        assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
        verify(tutorClassRepository).findById(classId);
        verify(classEnrollmentRepository, never())
                .findByTutorClassIdOrderByEnrolledAtAsc(anyLong());
    }

    @Test
    @DisplayName("Should return empty list when class has no enrolled students")
    void should_returnEmptyList_when_classHasNoStudents() {
        // Given
        Long classId = 1L;

        TutorClass tutorClass =
                TutorClass.builder().id(classId).title("Empty Class").maxStudents(10).build();

        when(tutorClassRepository.findById(classId)).thenReturn(Optional.of(tutorClass));
        when(classEnrollmentRepository.findByTutorClassIdOrderByEnrolledAtAsc(classId))
                .thenReturn(List.of());

        // When
        List<ClassStudentResponse> result = tutorClassService.getClassStudents(classId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());

        verify(tutorClassRepository).findById(classId);
        verify(classEnrollmentRepository).findByTutorClassIdOrderByEnrolledAtAsc(classId);
    }

    @Test
    @DisplayName("Should get student classes successfully")
    void should_getStudentClasses_successfully() {
        // Given
        MockHelper.mockSecurityContext("student1");

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        User tutor1 =
                User.builder()
                        .userId("tutor1")
                        .firstName("Jane")
                        .lastName("Smith")
                        .avatar("tutor1.jpg")
                        .build();

        User tutor2 =
                User.builder()
                        .userId("tutor2")
                        .firstName("Bob")
                        .lastName("Wilson")
                        .avatar("tutor2.jpg")
                        .build();

        Course course1 = Course.builder().id(1L).name("Mathematics").build();

        Course course2 = Course.builder().id(2L).name("Physics").build();

        TutorClass class1 =
                TutorClass.builder()
                        .id(1L)
                        .title("Math Class 101")
                        .tutor(tutor1)
                        .course(course1)
                        .startDate(LocalDate.of(2025, 12, 1))
                        .endDate(LocalDate.of(2026, 1, 31))
                        .currentStudents(5)
                        .maxStudents(10)
                        .build();

        TutorClass class2 =
                TutorClass.builder()
                        .id(2L)
                        .title("Physics Lab")
                        .tutor(tutor2)
                        .course(course2)
                        .startDate(LocalDate.of(2025, 12, 15))
                        .endDate(LocalDate.of(2026, 2, 28))
                        .currentStudents(8)
                        .maxStudents(15)
                        .build();

        ClassEnrollment enrollment1 =
                ClassEnrollment.builder()
                        .id(1L)
                        .student(student)
                        .tutorClass(class1)
                        .enrolledAt(LocalDateTime.of(2025, 11, 20, 10, 0))
                        .hasJoined(true)
                        .build();

        ClassEnrollment enrollment2 =
                ClassEnrollment.builder()
                        .id(2L)
                        .student(student)
                        .tutorClass(class2)
                        .enrolledAt(LocalDateTime.of(2025, 11, 25, 14, 30))
                        .hasJoined(false)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student1"))
                .thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc("student1"))
                .thenReturn(List.of(enrollment2, enrollment1)); // Ordered by enrolledAt desc

        // When
        List<StudentClassResponse> result = tutorClassService.getStudentClasses(null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first class (most recent enrollment)
        StudentClassResponse response1 = result.getFirst();
        assertEquals(2L, response1.getEnrollmentId());
        assertEquals(LocalDateTime.of(2025, 11, 25, 14, 30), response1.getEnrolledAt());
        assertEquals(false, response1.getHasJoined());
        assertEquals(2L, response1.getClassId());
        assertEquals("Physics Lab", response1.getClassTitle());
        assertEquals(LocalDate.of(2025, 12, 15), response1.getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), response1.getEndDate());
        assertEquals(8, response1.getCurrentStudents());
        assertEquals(15, response1.getMaxStudents());
        assertEquals("tutor2", response1.getTutorId());
        assertEquals("Bob Wilson", response1.getTutorName());
        assertEquals("tutor2.jpg", response1.getTutorAvatar());
        assertEquals(2L, response1.getCourseId());
        assertEquals("Physics", response1.getCourseName());

        // Verify second class
        StudentClassResponse response2 = result.get(1);
        assertEquals(1L, response2.getEnrollmentId());
        assertEquals(LocalDateTime.of(2025, 11, 20, 10, 0), response2.getEnrolledAt());
        assertEquals(true, response2.getHasJoined());
        assertEquals(1L, response2.getClassId());
        assertEquals("Math Class 101", response2.getClassTitle());
        assertEquals(LocalDate.of(2025, 12, 1), response2.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 31), response2.getEndDate());
        assertEquals(5, response2.getCurrentStudents());
        assertEquals(10, response2.getMaxStudents());
        assertEquals("tutor1", response2.getTutorId());
        assertEquals("Jane Smith", response2.getTutorName());
        assertEquals("tutor1.jpg", response2.getTutorAvatar());
        assertEquals(1L, response2.getCourseId());
        assertEquals("Mathematics", response2.getCourseName());

        verify(userRepository).findByUsernameAndNotDeleted("student1");
        verify(classEnrollmentRepository).findByStudentUserIdOrderByEnrolledAtDesc("student1");
    }
}
