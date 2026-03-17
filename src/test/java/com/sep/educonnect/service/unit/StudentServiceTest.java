package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.course.response.MyCourseResponse;
import com.sep.educonnect.dto.student.CheckInviteRequest;
import com.sep.educonnect.dto.student.StudentBookingListItemResponse;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.StudentService;
import com.sep.educonnect.service.ProgressService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private BookingMemberRepository bookingMemberRepository;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock private StudentLikesRepository studentLikesRepository;

    @Mock private TutorAvailabilityRepository tutorAvailabilityRepository;

    @Mock private TutorAvailabilityExceptionRepository tutorAvailabilityExceptionRepository;

    @Mock private TutorProfileRepository tutorProfileRepository;

    @Mock private ClassSessionRepository sessionRepo;

    @Mock private ScheduleChangeRepository scheduleChangeRepository;

    @Mock private CourseProgressRepository courseProgressRepository;

    @Mock private LessonProgressRepository lessonProgressRepository;

    @Mock private LessonRepository lessonRepository;

    @Mock private ModuleRepository moduleRepository;

    @Mock private CourseMapper courseMapper;

    @Mock private ProgressService progressService;

    @InjectMocks private StudentService studentService;

    @BeforeEach
    void setUp() {
        // Security context will be set up in each test as needed
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should get students for invitation successfully")
    void should_getStudentsForInvitation_successfully() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser =
                User.builder()
                        .userId("tutor1")
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .firstName("Student")
                        .lastName("One")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .firstName("Student")
                        .lastName("Two")
                        .email("student2@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student3 =
                User.builder()
                        .userId("tutor1") // Same as current user - should be filtered
                        .username("student3")
                        .firstName("Student")
                        .lastName("Three")
                        .email("student3@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        List<User> allStudents = List.of(student1, student2, student3);

        when(userRepository.findByUsername(currentUsername))
                .thenReturn(java.util.Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // student3 should be filtered out

        StudentGeneralResponse response1 = result.get(0);
        assertEquals("student1", response1.getUserId());
        assertEquals("Student One", response1.getName());
        assertEquals("student1@example.com", response1.getEmail());

        StudentGeneralResponse response2 = result.get(1);
        assertEquals("student2", response2.getUserId());
        assertEquals("Student Two", response2.getName());
        assertEquals("student2@example.com", response2.getEmail());

        verify(userRepository).findByUsername(currentUsername);
        verify(userRepository).findByRole_NameAndIsDeletedFalse("STUDENT");
    }

    @Test
    @DisplayName("Should return empty list when no students found")
    void should_returnEmptyList_when_noStudentsFound() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        when(userRepository.findByUsername(currentUsername))
                .thenReturn(java.util.Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(List.of());

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername(currentUsername);
        verify(userRepository).findByRole_NameAndIsDeletedFalse("STUDENT");
    }

    @Test
    @DisplayName("Should filter out current user from students list")
    void should_filterOutCurrentUser_fromStudentsList() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("tutor1") // Same as current user
                        .username("student1")
                        .firstName("Student")
                        .lastName("One")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        when(userRepository.findByUsername(currentUsername))
                .thenReturn(java.util.Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT"))
                .thenReturn(List.of(student));

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Current user should be filtered out
    }

    @Test
    @DisplayName("Should throw exception when current user not found")
    void should_throwException_when_currentUserNotFound_forGetStudentsForInvitation() {
        // Given
        String currentUsername = "nonexistent";
        MockHelper.mockSecurityContext(currentUsername);

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> studentService.getStudentsForInvitation());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository).findByUsername(currentUsername);
        verify(userRepository, never()).findByRole_NameAndIsDeletedFalse(anyString());
    }

    @Test
    @DisplayName("Should handle null first name and last name when getting students")
    void should_handleNullNames_when_gettingStudentsForInvitation() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .firstName(null)
                        .lastName("One")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .firstName("Student")
                        .lastName(null)
                        .email("student2@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student3 =
                User.builder()
                        .userId("student3")
                        .username("student3")
                        .firstName(null)
                        .lastName(null)
                        .email("student3@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        List<User> allStudents = List.of(student1, student2, student3);

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("null One", result.get(0).getName());
        assertEquals("Student null", result.get(1).getName());
        assertEquals("null null", result.get(2).getName());
    }

    @Test
    @DisplayName("Should handle empty first name and last name when getting students")
    void should_handleEmptyNames_when_gettingStudentsForInvitation() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .firstName("")
                        .lastName("One")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .firstName("Student")
                        .lastName("")
                        .email("student2@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        List<User> allStudents = List.of(student1, student2);

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(" One", result.get(0).getName());
        assertEquals("Student ", result.get(1).getName());
    }

    @Test
    @DisplayName("Should return all students when current user is not in student list")
    void should_returnAllStudents_when_currentUserNotInStudentList() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        List<User> allStudents = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            allStudents.add(
                    User.builder()
                            .userId("student" + i)
                            .username("student" + i)
                            .firstName("Student")
                            .lastName(String.valueOf(i))
                            .email("student" + i + "@example.com")
                            .role(studentRole)
                            .isDeleted(false)
                            .build());
        }

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(userRepository).findByUsername(currentUsername);
        verify(userRepository).findByRole_NameAndIsDeletedFalse("STUDENT");
    }

    @Test
    @DisplayName("Should handle multiple students with same userId as current user")
    void should_filterMultipleStudentsWithSameUserId() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("tutor1") // Same as current user
                        .username("student1")
                        .firstName("Student")
                        .lastName("One")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .firstName("Student")
                        .lastName("Two")
                        .email("student2@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student3 =
                User.builder()
                        .userId("tutor1") // Same as current user
                        .username("student3")
                        .firstName("Student")
                        .lastName("Three")
                        .email("student3@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        List<User> allStudents = List.of(student1, student2, student3);

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only student2 should be included
        assertEquals("student2", result.get(0).getUserId());
    }

    @Test
    @DisplayName("Should handle students with special characters in names")
    void should_handleSpecialCharactersInNames() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User currentUser = User.builder().userId("tutor1").username("tutor1").build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .firstName("Nguyễn Văn")
                        .lastName("Anh")
                        .email("student1@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .firstName("O'Connor")
                        .lastName("Smith-Jones")
                        .email("student2@example.com")
                        .role(studentRole)
                        .isDeleted(false)
                        .build();

        List<User> allStudents = List.of(student1, student2);

        when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRole_NameAndIsDeletedFalse("STUDENT")).thenReturn(allStudents);

        // When
        List<StudentGeneralResponse> result = studentService.getStudentsForInvitation();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Nguyễn Văn Anh", result.get(0).getName());
        assertEquals("O'Connor Smith-Jones", result.get(1).getName());
    }

    @Test
    @DisplayName("Should get student bookings successfully")
    void should_getStudentBookings_successfully() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student =
                User.builder()
                        .userId("student1")
                        .username(currentUsername)
                        .firstName("Student")
                        .lastName("One")
                        .build();

        Course course = Course.builder().id(1L).name("Math Course").build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.APPROVED)
                        .registrationType(RegistrationType.REGULAR)
                        .groupType(GroupType.GROUP)
                        .totalAmount(BigDecimal.valueOf(100))
                        .scheduleDescription("Weekly on Monday")
                        .course(course)
                        .createdAt(LocalDateTime.now())
                        .build();

        BookingMember bookingMember1 =
                BookingMember.builder()
                        .id(1L)
                        .userId("student1")
                        .role("OWNER")
                        .status(BookingMemberStatus.APPROVED)
                        .booking(booking)
                        .build();

        BookingMember bookingMember2 =
                BookingMember.builder()
                        .id(2L)
                        .userId("student2")
                        .role("MEMBER")
                        .status(BookingMemberStatus.WAITING)
                        .booking(booking)
                        .build();

        Set<BookingMember> allMembers = new HashSet<>();
        allMembers.add(bookingMember1);
        allMembers.add(bookingMember2);
        booking.setBookingMembers(allMembers);

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(bookingMemberRepository.searchByUser(
                        any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookingMember1)));

        // When
        Page<StudentBookingListItemResponse> result =
                studentService.getStudentBookings(0, 10, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        StudentBookingListItemResponse response = result.getContent().get(0);
        assertEquals(1L, response.getId());
        assertEquals(BookingStatus.APPROVED, response.getBookingStatus());
        assertEquals(BookingMemberStatus.APPROVED, response.getMyStatus());
        assertEquals(RegistrationType.REGULAR, response.getRegistrationType());
        assertEquals(GroupType.GROUP, response.getGroupType());
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
        assertEquals("Math Course", response.getCourseName());
        assertEquals("Weekly on Monday", response.getScheduleDescription());
        assertEquals(2, response.getCurrentMemberCount());
        assertTrue(response.getIsPaymentRequired()); // APPROVED + OWNER
        assertFalse(response.getHasPaid()); // Status is APPROVED, not PAID

        verify(userRepository).findByUsernameAndNotDeleted(currentUsername);
        verify(bookingMemberRepository)
                .searchByUser(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no bookings found")
    void should_returnEmptyList_when_noBookingsFound() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(bookingMemberRepository.searchByUser(
                        any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        Page<StudentBookingListItemResponse> result =
                studentService.getStudentBookings(0, 10, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should check can invite student successfully")
    void should_checkCanInviteStudent_successfully() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username(currentUsername)
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student1@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student1@example.com")).thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(false);

        // When
        String result = studentService.checkCanInviteStudent(request);

        // Then
        assertEquals("student1", result);
        verify(userRepository).findByUsernameAndNotDeleted(currentUsername);
        verify(userRepository).findByEmail("student1@example.com");
        verify(bookingMemberRepository).existsByUserIdAndBooking_Course_Id("student1", 1L);
    }

    @Test
    @DisplayName("Should throw exception when inviting non-existent user")
    void should_throwException_when_invitingNonExistentUser() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor = User.builder().userId("tutor1").username(currentUsername).build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("nonexistent@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> studentService.checkCanInviteStudent(request));
    }

    @Test
    @DisplayName("Should throw exception when student already enrolled")
    void should_throwException_when_studentAlreadyEnrolled() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor = User.builder().userId("tutor1").username(currentUsername).build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student1@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student1@example.com")).thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> studentService.checkCanInviteStudent(request));
        assertEquals(ErrorCode.STUDENT_ALREADY_ENROLLED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when current user not found")
    void should_throwException_when_currentUserNotFound_forCheckCanInvite() {
        // Given
        String currentUsername = "nonexistent";
        MockHelper.mockSecurityContext(currentUsername);

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student1@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> studentService.checkCanInviteStudent(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository).findByUsernameAndNotDeleted(currentUsername);
        verify(userRepository, never()).findByEmail(anyString());
        verify(bookingMemberRepository, never())
                .existsByUserIdAndBooking_Course_Id(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should check can invite student with different courseId")
    void should_checkCanInviteStudent_withDifferentCourseId() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username(currentUsername)
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student1@example.com").courseId(999L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student1@example.com")).thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 999L))
                .thenReturn(false);

        // When
        String result = studentService.checkCanInviteStudent(request);

        // Then
        assertEquals("student1", result);
        verify(bookingMemberRepository).existsByUserIdAndBooking_Course_Id("student1", 999L);
    }

    @Test
    @DisplayName("Should handle email with different case sensitivity")
    void should_handleEmailCaseSensitivity() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username(currentUsername)
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("STUDENT1@EXAMPLE.COM").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("STUDENT1@EXAMPLE.COM")).thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(false);

        // When
        String result = studentService.checkCanInviteStudent(request);

        // Then
        assertEquals("student1", result);
        verify(userRepository).findByEmail("STUDENT1@EXAMPLE.COM");
    }

    @Test
    @DisplayName("Should throw exception when student enrolled in same course multiple times check")
    void should_throwException_when_studentEnrolledMultipleTimes() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor = User.builder().userId("tutor1").username(currentUsername).build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student1@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student1@example.com")).thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(true); // Already enrolled

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> studentService.checkCanInviteStudent(request));
        assertEquals(ErrorCode.STUDENT_ALREADY_ENROLLED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should successfully check multiple different students for invitation")
    void should_checkMultipleDifferentStudents() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username(currentUsername)
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student1 =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student1@example.com")
                        .role(studentRole)
                        .build();

        User student2 =
                User.builder()
                        .userId("student2")
                        .username("student2")
                        .email("student2@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request1 =
                CheckInviteRequest.builder().email("student1@example.com").courseId(1L).build();

        CheckInviteRequest request2 =
                CheckInviteRequest.builder().email("student2@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student1@example.com")).thenReturn(Optional.of(student1));
        when(userRepository.findByEmail("student2@example.com")).thenReturn(Optional.of(student2));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(false);
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student2", 1L))
                .thenReturn(false);

        // When
        String result1 = studentService.checkCanInviteStudent(request1);
        String result2 = studentService.checkCanInviteStudent(request2);

        // Then
        assertEquals("student1", result1);
        assertEquals("student2", result2);
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void should_handleSpecialCharactersInEmail() {
        // Given
        String currentUsername = "tutor1";
        MockHelper.mockSecurityContext(currentUsername);

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username(currentUsername)
                        .email("tutor1@example.com")
                        .build();

        Role studentRole = Role.builder().name("STUDENT").build();

        User student =
                User.builder()
                        .userId("student1")
                        .username("student1")
                        .email("student+test@example.com")
                        .role(studentRole)
                        .build();

        CheckInviteRequest request =
                CheckInviteRequest.builder().email("student+test@example.com").courseId(1L).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("student+test@example.com"))
                .thenReturn(Optional.of(student));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("student1", 1L))
                .thenReturn(false);

        // When
        String result = studentService.checkCanInviteStudent(request);

        // Then
        assertEquals("student1", result);
        verify(userRepository).findByEmail("student+test@example.com");
    }

    @Test
    @DisplayName("Should toggle like tutor profile successfully - like")
    void should_toggleLikeTutorProfile_successfully_like() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();

        User tutor =
                User.builder()
                        .userId("tutor1")
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByIdAndStatus(1L, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student1", "tutor1"))
                .thenReturn(true);
        when(studentLikesRepository.findByStudentIdAndTutor_Id("student1", 1L))
                .thenReturn(Optional.empty());

        // When
        studentService.toggleLikeTutorProfile(1L);

        // Then
        verify(userRepository).findByUsernameAndNotDeleted(currentUsername);
        verify(tutorProfileRepository).findByIdAndStatus(1L, ProfileStatus.APPROVED);
        verify(classEnrollmentRepository).existsByStudentUserIdAndTutorUserId("student1", "tutor1");
        verify(studentLikesRepository).findByStudentIdAndTutor_Id("student1", 1L);
        verify(studentLikesRepository).save(any(StudentLikes.class));
        verify(studentLikesRepository, never())
                .deleteByStudentIdAndTutor_Id(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should throw exception when student not enrolled with tutor")
    void should_throwException_when_studentNotEnrolledWithTutor() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();

        User tutor = User.builder().userId("tutor1").username("tutor1").build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByIdAndStatus(1L, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student1", "tutor1"))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> studentService.toggleLikeTutorProfile(1L));
        assertEquals(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR, exception.getErrorCode());
        verify(tutorProfileRepository).findByIdAndStatus(1L, ProfileStatus.APPROVED);
        verify(classEnrollmentRepository).existsByStudentUserIdAndTutorUserId("student1", "tutor1");
        verify(studentLikesRepository, never()).findByStudentIdAndTutor_Id(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should throw exception when tutor profile not found")
    void should_throwException_when_tutorProfileNotFound() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByIdAndStatus(1L, ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> studentService.toggleLikeTutorProfile(1L));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository).findByIdAndStatus(1L, ProfileStatus.APPROVED);
        verify(classEnrollmentRepository, never())
                .existsByStudentUserIdAndTutorUserId(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when current user not found for toggle like")
    void should_throwException_when_currentUserNotFound_forToggleLike() {
        // Given
        String currentUsername = "nonexistent";
        MockHelper.mockSecurityContext(currentUsername);

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> studentService.toggleLikeTutorProfile(1L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository).findByUsernameAndNotDeleted(currentUsername);
        verify(tutorProfileRepository, never()).findByIdAndStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Should like different tutor profiles independently")
    void should_likeDifferentTutorProfiles_independently() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();
        User tutor1 = User.builder().userId("tutor1").username("tutor1").build();
        User tutor2 = User.builder().userId("tutor2").username("tutor2").build();
        TutorProfile tutorProfile1 =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor1)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();
        TutorProfile tutorProfile2 =
                TutorProfile.builder()
                        .id(2L)
                        .user(tutor2)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByIdAndStatus(1L, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile1));
        when(tutorProfileRepository.findByIdAndStatus(2L, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile2));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student1", "tutor1"))
                .thenReturn(true);
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student1", "tutor2"))
                .thenReturn(true);
        when(studentLikesRepository.findByStudentIdAndTutor_Id("student1", 1L))
                .thenReturn(Optional.empty());
        when(studentLikesRepository.findByStudentIdAndTutor_Id("student1", 2L))
                .thenReturn(Optional.empty());

        // When
        studentService.toggleLikeTutorProfile(1L);
        studentService.toggleLikeTutorProfile(2L);

        // Then
        verify(studentLikesRepository, times(2)).save(any(StudentLikes.class));
    }

    @Test
    @DisplayName("Should handle like for tutor profile with large ID")
    void should_handleLike_forLargeTutorProfileId() {
        // Given
        String currentUsername = "student1";
        MockHelper.mockSecurityContext(currentUsername);

        User student = User.builder().userId("student1").username(currentUsername).build();
        User tutor = User.builder().userId("tutor1").username("tutor1").build();
        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(999999L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted(currentUsername))
                .thenReturn(Optional.of(student));
        when(tutorProfileRepository.findByIdAndStatus(999999L, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student1", "tutor1"))
                .thenReturn(true);
        when(studentLikesRepository.findByStudentIdAndTutor_Id("student1", 999999L))
                .thenReturn(Optional.empty());

        // When
        studentService.toggleLikeTutorProfile(999999L);

        // Then
        verify(tutorProfileRepository).findByIdAndStatus(999999L, ProfileStatus.APPROVED);
        verify(studentLikesRepository).save(any(StudentLikes.class));
    }

    @Test
    @DisplayName("Should get weekly schedule successfully")
    void should_getWeeklySchedule_successfully() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        assertEquals(tutorId, result.getUserId());
        assertEquals("Tutor One", result.getTutorName());
        assertFalse(result.getDays().isEmpty());

        // Verify Monday (index 1 in days list if starting from Sunday, but logic
        // depends on startDate)
        // startDate is Monday, so first day in response should be Monday
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        assertFalse(monday.getSlots().isEmpty());
        WeeklyAvailabilityResponse.SlotInfo slot1 =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("BOOKED", slot1.getSlotStatus());
        assertTrue(slot1.getIsBooked());
        assertEquals(1L, slot1.getSessionId());
    }

    @Test
    @DisplayName("Should throw exception when tutor user not found")
    void should_throwException_when_tutorUserNotFound_forGetWeeklySchedule() {
        // Given
        String tutorId = "nonexistent";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        when(userRepository.findById(tutorId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentService.getWeeklySchedule(tutorId, startDate));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository).findById(tutorId);
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when tutor profile not found")
    void should_throwException_when_tutorProfileNotFound_forGetWeeklySchedule() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor = User.builder().userId(tutorId).username("tutor1").build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentService.getWeeklySchedule(tutorId, startDate));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorAvailabilityRepository, never()).findByUserUserId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when tutor availability not set")
    void should_throwException_when_tutorAvailabilityNotSet() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor = User.builder().userId(tutorId).username("tutor1").build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentService.getWeeklySchedule(tutorId, startDate));
        assertEquals(ErrorCode.TUTOR_AVAILABILITY_NOT_SET, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get weekly schedule with no sessions")
    void should_getWeeklySchedule_withNoSessions() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2,3")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1,2")
                        .isWorkOnTuesday(true)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        assertEquals(tutorId, result.getUserId());
        assertFalse(result.getDays().isEmpty());

        // All slots should be AVAILABLE
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        assertTrue(monday.getSlots().stream().allMatch(s -> s.getSlotStatus().equals("AVAILABLE")));
    }

    @Test
    @DisplayName("Should get weekly schedule with exceptions")
    void should_getWeeklySchedule_withExceptions() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .session(session)
                        .reason("Medical emergency")
                        .status(ExceptionStatus.APPROVED)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L))
                .thenReturn(List.of(exception));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo slot1 =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("EXCEPTION", slot1.getSlotStatus());
        assertTrue(slot1.getHasException());
        assertEquals("Medical emergency", slot1.getExceptionReason());
    }

    @Test
    @DisplayName("Should get weekly schedule with schedule changes")
    void should_getWeeklySchedule_withScheduleChanges() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2,3")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1,2")
                        .isWorkOnTuesday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate) // Monday
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(1L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(startDate.plusDays(1)) // Tuesday
                        .newSLot(2)
                        .content("Reschedule due to conflict")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Monday slot should be MOVED_FROM
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo mondaySlot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_FROM", mondaySlot.getSlotStatus());
        assertTrue(mondaySlot.getHasScheduleChange());

        // Tuesday slot 2 should be MOVED_TO
        LocalDate tuesday = startDate.plusDays(1);
        WeeklyAvailabilityResponse.DaySchedule tuesdaySchedule =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(tuesday))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo tuesdaySlot =
                tuesdaySchedule.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 2)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_TO", tuesdaySlot.getSlotStatus());
    }

    @Test
    @DisplayName("Should get weekly schedule with multiple sessions on same day")
    void should_getWeeklySchedule_withMultipleSessions() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2,3,4")
                        .isWorkOnMonday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(2L)
                        .sessionDate(startDate)
                        .slotNumber(2)
                        .tutorClass(tutorClass)
                        .sessionNumber(2)
                        .build();

        ClassSession session3 =
                ClassSession.builder()
                        .id(3L)
                        .sessionDate(startDate)
                        .slotNumber(3)
                        .tutorClass(tutorClass)
                        .sessionNumber(3)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session1, session2, session3));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        // Should have 4 slots (3 booked + 1 available)
        assertEquals(4, monday.getSlots().size());

        // Check slots 1, 2, 3 are BOOKED
        long bookedCount =
                monday.getSlots().stream().filter(s -> s.getSlotStatus().equals("BOOKED")).count();
        assertEquals(3, bookedCount);

        // Check slot 4 is AVAILABLE
        WeeklyAvailabilityResponse.SlotInfo slot4 =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 4)
                        .findFirst()
                        .orElseThrow();
        assertEquals("AVAILABLE", slot4.getSlotStatus());
    }

    @Test
    @DisplayName("Should get weekly schedule with no work days")
    void should_getWeeklySchedule_withNoWorkDays() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .isWorkOnMonday(false)
                        .isWorkOnTuesday(false)
                        .isWorkOnWednesday(false)
                        .isWorkOnThursday(false)
                        .isWorkOnFriday(false)
                        .isWorkOnSaturday(false)
                        .isWorkOnSunday(false)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        // Days list might be empty since no slots are available or booked
        assertTrue(result.getDays().isEmpty());
    }

    @Test
    @DisplayName("Should get weekly schedule with unavailable slots")
    void should_getWeeklySchedule_withUnavailableSlots() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1") // Only slot 1 available
                        .isWorkOnMonday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        // Session on slot 2 which is not available
        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(2)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        // Should have 2 slots (slot 1 available, slot 2 booked but not available)
        assertEquals(2, monday.getSlots().size());

        // Slot 1 should be AVAILABLE
        WeeklyAvailabilityResponse.SlotInfo slot1 =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();
        assertEquals("AVAILABLE", slot1.getSlotStatus());
        assertTrue(slot1.getIsAvailable());

        // Slot 2 should be BOOKED (even though not in available slots)
        WeeklyAvailabilityResponse.SlotInfo slot2 =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 2)
                        .findFirst()
                        .orElseThrow();
        assertEquals("BOOKED", slot2.getSlotStatus());
    }

    @Test
    @DisplayName("Should get weekly schedule for full week")
    void should_getWeeklySchedule_forFullWeek() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1")
                        .isWorkOnTuesday(true)
                        .wednesdaySlots("1")
                        .isWorkOnWednesday(true)
                        .thursdaySlots("1")
                        .isWorkOnThursday(true)
                        .fridaySlots("1")
                        .isWorkOnFriday(true)
                        .saturdaySlots("1")
                        .isWorkOnSaturday(true)
                        .sundaySlots("1")
                        .isWorkOnSunday(true)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(startDate.plusDays(6), result.getEndDate());
        // Should have 7 days with slots
        assertEquals(7, result.getDays().size());

        // Verify each day has at least one slot
        result.getDays()
                .forEach(
                        day -> {
                            assertFalse(day.getSlots().isEmpty());
                            assertTrue(day.getIsWorkDay());
                        });
    }

    @Test
    @DisplayName("Should handle null tutor name components")
    void should_handleNullTutorNameComponents_forGetWeeklySchedule() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName(null)
                        .lastName(null)
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1")
                        .isWorkOnMonday(true)
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        assertEquals("null null", result.getTutorName());
    }

    @Test
    @DisplayName("Should build schedule change info with MOVED_FROM direction")
    void should_buildScheduleChangeInfo_withMovedFromDirection() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1,2")
                        .isWorkOnTuesday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        LocalDate newDate = startDate.plusDays(1); // Tuesday
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(100L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(2)
                        .content("Rescheduled due to conflict")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Find the slot on old date (Monday)
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo mondaySlot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        // Verify schedule change info on MOVED_FROM
        assertNotNull(mondaySlot.getScheduleChangeInfo());
        assertEquals(100L, mondaySlot.getScheduleChangeInfo().getScheduleChangeId());
        assertEquals(startDate, mondaySlot.getScheduleChangeInfo().getOldDate());
        assertEquals(newDate, mondaySlot.getScheduleChangeInfo().getNewDate());
        assertEquals(2, mondaySlot.getScheduleChangeInfo().getNewSlot());
        assertEquals(
                "Rescheduled due to conflict", mondaySlot.getScheduleChangeInfo().getContent());
        assertTrue(mondaySlot.getScheduleChangeInfo().getIsOldDate());
        assertEquals("MOVED_FROM", mondaySlot.getScheduleChangeInfo().getChangeDirection());
    }

    @Test
    @DisplayName("Should build schedule change info with MOVED_TO direction")
    void should_buildScheduleChangeInfo_withMovedToDirection() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1,2,3")
                        .isWorkOnTuesday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        LocalDate newDate = startDate.plusDays(1); // Tuesday
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(200L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(3)
                        .content("Changed to accommodate student")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Find the slot on new date (Tuesday)
        WeeklyAvailabilityResponse.DaySchedule tuesday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(newDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo tuesdaySlot =
                tuesday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 3)
                        .findFirst()
                        .orElseThrow();

        // Verify schedule change info on MOVED_TO
        assertNotNull(tuesdaySlot.getScheduleChangeInfo());
        assertEquals(200L, tuesdaySlot.getScheduleChangeInfo().getScheduleChangeId());
        assertEquals(startDate, tuesdaySlot.getScheduleChangeInfo().getOldDate());
        assertEquals(newDate, tuesdaySlot.getScheduleChangeInfo().getNewDate());
        assertEquals(3, tuesdaySlot.getScheduleChangeInfo().getNewSlot());
        assertEquals(
                "Changed to accommodate student", tuesdaySlot.getScheduleChangeInfo().getContent());
        assertFalse(tuesdaySlot.getScheduleChangeInfo().getIsOldDate());
        assertEquals("MOVED_TO", tuesdaySlot.getScheduleChangeInfo().getChangeDirection());
    }

    @Test
    @DisplayName("Should build schedule change info with both old and new dates in same week")
    void should_buildScheduleChangeInfo_withBothDatesInSameWeek() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .wednesdaySlots("1,2,3")
                        .isWorkOnWednesday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate) // Monday
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        LocalDate newDate = startDate.plusDays(2); // Wednesday
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(2)
                        .content("Moved within same week")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Verify MOVED_FROM on Monday
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo mondaySlot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_FROM", mondaySlot.getSlotStatus());
        assertNotNull(mondaySlot.getScheduleChangeInfo());
        assertTrue(mondaySlot.getScheduleChangeInfo().getIsOldDate());
        assertEquals("MOVED_FROM", mondaySlot.getScheduleChangeInfo().getChangeDirection());

        // Verify MOVED_TO on Wednesday
        WeeklyAvailabilityResponse.DaySchedule wednesday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(newDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo wednesdaySlot =
                wednesday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 2)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_TO", wednesdaySlot.getSlotStatus());
        assertNotNull(wednesdaySlot.getScheduleChangeInfo());
        assertFalse(wednesdaySlot.getScheduleChangeInfo().getIsOldDate());
        assertEquals("MOVED_TO", wednesdaySlot.getScheduleChangeInfo().getChangeDirection());
    }

    @Test
    @DisplayName("Should build schedule change info with all required fields populated")
    void should_buildScheduleChangeInfo_withAllFieldsPopulated() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2,3,4,5")
                        .isWorkOnMonday(true)
                        .fridaySlots("1,2,3,4")
                        .isWorkOnFriday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(3)
                        .tutorClass(tutorClass)
                        .sessionNumber(5)
                        .build();

        LocalDate newDate = startDate.plusDays(4); // Friday
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(999L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(4)
                        .content("Emergency schedule change - family matter")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Verify all fields on MOVED_FROM
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo mondaySlot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 3)
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.ScheduleChangeInfo info = mondaySlot.getScheduleChangeInfo();
        assertNotNull(info);
        assertEquals(999L, info.getScheduleChangeId());
        assertEquals(startDate, info.getOldDate());
        assertEquals(newDate, info.getNewDate());
        assertEquals(4, info.getNewSlot());
        assertEquals("Emergency schedule change - family matter", info.getContent());
        assertTrue(info.getIsOldDate());
        assertEquals("MOVED_FROM", info.getChangeDirection());
    }

    @Test
    @DisplayName("Should handle schedule change with empty content")
    void should_buildScheduleChangeInfo_withEmptyContent() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23);

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1")
                        .isWorkOnMonday(true)
                        .tuesdaySlots("1")
                        .isWorkOnTuesday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        LocalDate newDate = startDate.plusDays(1);
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(500L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(1)
                        .content("") // Empty content
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo slot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertNotNull(slot.getScheduleChangeInfo());
        assertEquals("", slot.getScheduleChangeInfo().getContent());
    }

    @Test
    @DisplayName("Should handle schedule change moving to same slot different day")
    void should_buildScheduleChangeInfo_sameSLotDifferentDay() {
        // Given
        String tutorId = "tutor1";
        LocalDate startDate = LocalDate.of(2023, 10, 23); // Monday

        User tutor =
                User.builder()
                        .userId(tutorId)
                        .username("tutor1")
                        .firstName("Tutor")
                        .lastName("One")
                        .build();

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .id(1L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorAvailability availability =
                TutorAvailability.builder()
                        .user(tutor)
                        .mondaySlots("1,2")
                        .isWorkOnMonday(true)
                        .thursdaySlots("1,2")
                        .isWorkOnThursday(true)
                        .build();

        TutorClass tutorClass = TutorClass.builder().id(1L).build();

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionDate(startDate)
                        .slotNumber(1)
                        .tutorClass(tutorClass)
                        .sessionNumber(1)
                        .build();

        LocalDate newDate = startDate.plusDays(3); // Thursday
        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(600L)
                        .session(session)
                        .oldDate(startDate)
                        .newDate(newDate)
                        .newSLot(1) // Same slot, different day
                        .content("Same slot but different day")
                        .status("APPROVED")
                        .build();

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        tutorId, ProfileStatus.APPROVED))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorAvailabilityRepository.findByUserUserId(tutorId))
                .thenReturn(Optional.of(availability));
        when(sessionRepo.findByTutorAndDateRange(eq(tutorId), eq(startDate), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus("tutor1", "APPROVED"))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(1L)).thenReturn(List.of());
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse result = studentService.getWeeklySchedule(tutorId, startDate);

        // Then
        assertNotNull(result);

        // Verify Monday slot 1 is MOVED_FROM
        WeeklyAvailabilityResponse.DaySchedule monday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(startDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo mondaySlot =
                monday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_FROM", mondaySlot.getSlotStatus());
        assertEquals(1, mondaySlot.getScheduleChangeInfo().getNewSlot());

        // Verify Thursday slot 1 is MOVED_TO
        WeeklyAvailabilityResponse.DaySchedule thursday =
                result.getDays().stream()
                        .filter(d -> d.getDate().equals(newDate))
                        .findFirst()
                        .orElseThrow();

        WeeklyAvailabilityResponse.SlotInfo thursdaySlot =
                thursday.getSlots().stream()
                        .filter(s -> s.getSlotNumber() == 1)
                        .findFirst()
                        .orElseThrow();

        assertEquals("MOVED_TO", thursdaySlot.getSlotStatus());
        assertEquals(1, thursdaySlot.getScheduleChangeInfo().getNewSlot());
    }

    @Test
    @DisplayName("Should default to ALL when status filter is invalid")
    void should_defaultToAll_when_statusInvalid() {
        // Given
        String username = "student1";
        MockHelper.mockSecurityContext(username);

        User student = User.builder().userId("student1").username(username).build();
        when(userRepository.findByUsernameAndNotDeleted(username)).thenReturn(Optional.of(student));
        when(bookingMemberRepository.findByUserIdOrderByBooking_CreatedAtDesc("student1"))
                .thenReturn(Collections.emptyList());

        // When
        List<MyCourseResponse> result = studentService.getMyCourses("INVALID");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
