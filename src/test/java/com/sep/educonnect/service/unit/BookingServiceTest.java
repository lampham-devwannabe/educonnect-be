package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.booking.BookTrialRequest;
import com.sep.educonnect.dto.booking.BookingResponse;
import com.sep.educonnect.dto.booking.BookingStateResponse;
import com.sep.educonnect.dto.booking.CreateBookingRequest;
import com.sep.educonnect.dto.booking.JoinClassRequest;
import com.sep.educonnect.dto.notification.request.NotificationUpdateRequest;
import com.sep.educonnect.dto.tutor.request.WeeklySchedule;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.BookingService;
import com.sep.educonnect.service.NotificationService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;

    @Mock private UserRepository userRepository;

    @Mock private CourseRepository courseRepository;

    @Mock private BookingMemberRepository bookingMemberRepository;

    @Mock private TutorClassRepository tutorClassRepository;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock private ClassSessionRepository classSessionRepository;

    @Mock private NotificationService notificationService;

    @InjectMocks private BookingService bookingService;

    private User currentUser;
    private Course course;
    private TutorClass tutorClass;

    @BeforeEach
    void setUp() {
        currentUser =
                User.builder()
                        .userId("user-1")
                        .username("student")
                        .email("student@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        User tutor = User.builder().userId("tutor-1").firstName("Dr.").lastName("Smith").build();

        course =
                Course.builder()
                        .id(1L)
                        .name("Math Course")
                        .type(CourseType.ONLINE)
                        .status(CourseStatus.ONGOING)
                        .price(new BigDecimal("100"))
                        .isCombo(false)
                        .tutor(tutor)
                        .build();

        tutorClass =
                TutorClass.builder()
                        .id(10L)
                        .course(course)
                        .title("Math Class 1")
                        .maxStudents(5)
                        .currentStudents(2)
                        .lastJoinDate(LocalDate.now().plusDays(7))
                        .build();
    }

    @Test
    @DisplayName("Should create booking successfully for ONLINE course")
    void should_createBooking_successfully_forOnlineCourse() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });

        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(GroupType.INDIVIDUAL, response.getGroupType());
        assertEquals(RegistrationType.REGULAR, response.getRegistrationType());
        assertEquals(
                new BigDecimal("50"), response.getTotalAmount()); // coursePrice(50) * members(1)
        assertNotNull(response.getScheduleDescription());
        assertNotNull(response.getCourse());
        assertEquals(1, response.getMembers().size());
        assertEquals("OWNER", response.getMembers().get(0).getRole());
        assertEquals(BookingMemberStatus.APPROVED, response.getMembers().get(0).getStatus());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(1, savedBooking.getBookingMembers().size());
    }

    @Test
    @DisplayName("Should create booking successfully for SELF_PACED course")
    void should_createBooking_successfully_forSelfPacedCourse() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(new BigDecimal("200"));

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(
                BookingStatus.APPROVED, response.getBookingStatus()); // Self-paced is auto-approved
        assertEquals(new BigDecimal("50"), response.getTotalAmount()); // Course price * 1 member
        assertNull(response.getScheduleDescription()); // No schedule for self-paced

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(BookingStatus.APPROVED, savedBooking.getBookingStatus());
    }

    @Test
    @DisplayName("Should create booking with GROUP type and multiple members")
    void should_createBooking_withGroupType_andMultipleMembers() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();
        User member2 = User.builder().userId("user-3").firstName("Bob").lastName("Jones").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-3")).thenReturn(Optional.of(member2));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-3")).thenReturn(Optional.of(member2));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(Set.of("user-2", "user-3"));
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(GroupType.GROUP, response.getGroupType());
        assertEquals(new BigDecimal("150"), response.getTotalAmount()); // 50 * 10 * 3 members
        assertEquals(3, response.getMembers().size()); // owner + 2 members
        assertTrue(response.getMembers().stream().anyMatch(m -> m.getRole().equals("OWNER")));
        assertTrue(
                response.getMembers().stream()
                        .anyMatch(m -> m.getStatus() == BookingMemberStatus.WAITING)); // Members
        // are
        // waiting

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(3, savedBooking.getBookingMembers().size());
    }

    @Test
    @DisplayName("Should throw when course not found")
    void should_throw_when_courseNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when course is INACTIVE")
    void should_throw_when_courseInactive() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setStatus(CourseStatus.INACTIVE);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.COURSE_INACTIVE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when booking already exists")
    void should_throw_when_bookingAlreadyExists() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(true);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.BOOKING_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when GROUP type requires members but none provided")
    void should_throw_when_groupTypeRequiresMembers_butNoneProvided() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(null); // No members provided

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.BOOKING_GROUP_MEMBERS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when GROUP type requires members but empty list provided")
    void should_throw_when_groupTypeRequiresMembers_butEmptyListProvided() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(new HashSet<>()); // Empty list

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.BOOKING_GROUP_MEMBERS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw UNAUTHENTICATED when user not found")
    void should_throw_unauthenticated_when_userNotFound() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsernameAndNotDeleted("nonexistent"))
                .thenReturn(Optional.empty());

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.createBooking(request));
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should filter out null memberIds")
    void should_filterOut_nullMemberIds() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        Set<String> memberIds = new HashSet<>();
        memberIds.add("user-2");
        memberIds.add(null); // null value should be filtered out
        request.setMemberIds(memberIds);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getMembers().size()); // owner + 1 valid member (null filtered)
        assertEquals(new BigDecimal("100"), response.getTotalAmount()); // 50  * 2 members
    }

    @Test
    @DisplayName("Should filter out non-existent members")
    void should_filterOut_nonExistentMembers() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-nonexistent"))
                .thenReturn(Optional.empty()); // Non-existent user
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(Set.of("user-2", "user-nonexistent")); // One non-existent
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(
                2, response.getMembers().size()); // owner + 1 valid member (nonexistent filtered)
        assertEquals(new BigDecimal("100"), response.getTotalAmount()); // 50 * 10 * 2 members
    }

    @Test
    @DisplayName("Should handle notification failure when member not found")
    void should_handle_notificationFailure_when_memberNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-3"))
                .thenReturn(Optional.of(User.builder().userId("user-3").build()));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-3"))
                .thenReturn(Optional.empty()); // Not found for notification
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(Set.of("user-2", "user-3"));
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getMembers().size());
        // Should log warning but not fail
        verify(userRepository).findById("user-2");
        verify(userRepository).findById("user-3");
    }

    @Test
    @DisplayName("Should return null schedule description when weeklySchedules is null")
    void should_returnNullScheduleDescription_when_weeklySchedulesIsNull() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(null); // Null schedules

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNull(response.getScheduleDescription()); // Should be null
    }

    @Test
    @DisplayName("Should return null schedule description when weeklySchedules is empty")
    void should_returnNullScheduleDescription_when_weeklySchedulesIsEmpty() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(new ArrayList<>()); // Empty list

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNull(response.getScheduleDescription()); // Should be null
    }

    @Test
    @DisplayName("Should filter out null entries in weeklySchedules")
    void should_filterOut_nullEntries_in_weeklySchedules() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        List<WeeklySchedule> schedules = new ArrayList<>();
        schedules.add(null); // Null entry
        schedules.add(
                WeeklySchedule.builder()
                        .dayOfWeek(1) // Monday
                        .slotNumbers(List.of(1, 2))
                        .build());
        schedules.add(null); // Another null entry
        request.setWeeklySchedules(schedules);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getScheduleDescription());
        assertTrue(
                response.getScheduleDescription()
                        .contains("Monday")); // Only valid schedule included
        assertFalse(response.getScheduleDescription().contains("null"));
    }

    @Test
    @DisplayName("Should filter out null slot numbers in weeklySchedules")
    void should_filterOut_nullSlotNumbers_in_weeklySchedules() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        List<Integer> slotsWithNull = new ArrayList<>();
        slotsWithNull.add(1);
        slotsWithNull.add(null); // Null slot
        slotsWithNull.add(2);

        List<WeeklySchedule> schedules = new ArrayList<>();
        schedules.add(
                WeeklySchedule.builder()
                        .dayOfWeek(1) // Monday
                        .slotNumbers(slotsWithNull)
                        .build());
        request.setWeeklySchedules(schedules);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getScheduleDescription());
        assertTrue(response.getScheduleDescription().contains("Monday"));
        // Should have formatted slots, null filtered out
    }

    @Test
    @DisplayName("Should use 'the tutor' when course tutor is null")
    void should_usePlaceholder_when_courseTutorIsNull() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setTutor(null); // Tutor is null

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, atLeastOnce())
                .createAndSendNotification(
                        anyString(), messageCaptor.capture(), any(), any(), any());
        // Should contain "the tutor" instead of actual tutor name
        assertTrue(
                messageCaptor.getAllValues().stream().anyMatch(msg -> msg.contains("the tutor")));
    }

    // ==================== Tests for resolveMemberIds method ====================

    @Test
    @DisplayName("Should skip GROUP validation for SELF_PACED courses even with GROUP type")
    void should_skipGroupValidation_forSelfPacedCourses() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(new BigDecimal("200"));

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP); // GROUP type
        request.setRegistrationType(RegistrationType.REGULAR);
        request.setMemberIds(null); // No members provided but should NOT throw because self-paced

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(BookingStatus.APPROVED, response.getBookingStatus());
        assertEquals(1, response.getMembers().size()); // Only current user
        assertEquals("user-1", response.getMembers().get(0).getUserId());
    }

    @Test
    @DisplayName("Should skip GROUP validation for INDIVIDUAL type even without members")
    void should_skipGroupValidation_forIndividualType() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL); // INDIVIDUAL type
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(null); // No members provided - should be OK for INDIVIDUAL
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(1, response.getMembers().size()); // Only current user
        assertEquals("user-1", response.getMembers().get(0).getUserId());
    }

    @Test
    @DisplayName("Should handle null memberIds without iteration")
    void should_handleNullMemberIds_withoutIteration() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(null); // Null memberIds - should skip iteration
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getMembers().size()); // Only current user added
        assertEquals("user-1", response.getMembers().get(0).getUserId());

        // Verify userRepository.findByIdAndNotDeleted was never called (no iteration)
        verify(userRepository, never()).findByIdAndNotDeleted(anyString());
    }

    @Test
    @DisplayName("Should always add currentUser regardless of memberIds")
    void should_alwaysAddCurrentUser_regardlessOfMemberIds() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(new HashSet<>()); // Empty set
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        assertEquals("user-1", response.getMembers().get(0).getUserId());
        assertEquals("OWNER", response.getMembers().get(0).getRole());
        assertEquals(BookingMemberStatus.APPROVED, response.getMembers().get(0).getStatus());
    }

    @Test
    @DisplayName("Should add currentUser even when currentUser is in memberIds")
    void should_addCurrentUser_evenWhenInMemberIds() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-1")).thenReturn(Optional.of(currentUser));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setMemberIds(Set.of("user-1")); // Current user in memberIds
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // Set should deduplicate - only one entry for user-1
        assertEquals(1, response.getMembers().size());
        assertEquals("user-1", response.getMembers().get(0).getUserId());
    }

    @Test
    @DisplayName("Should handle SELF_PACED with GROUP type and empty memberIds")
    void should_handleSelfPacedWithGroupType_andEmptyMemberIds() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(new BigDecimal("200"));

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP); // GROUP type
        request.setRegistrationType(RegistrationType.REGULAR);
        request.setMemberIds(new HashSet<>()); // Empty set - should NOT throw for SELF_PACED

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(BookingStatus.APPROVED, response.getBookingStatus());
        assertEquals(1, response.getMembers().size()); // Only current user
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid memberIds")
    void should_handleMixedValidAndInvalidMemberIds() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();
        User member2 = User.builder().userId("user-3").firstName("Bob").lastName("Jones").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-3")).thenReturn(Optional.of(member2));
        when(userRepository.findByIdAndNotDeleted("user-invalid")).thenReturn(Optional.empty());
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-3")).thenReturn(Optional.of(member2));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);

        // Mix of null, valid, and invalid memberIds
        Set<String> memberIds = new HashSet<>();
        memberIds.add("user-2"); // valid
        memberIds.add(null); // null - should be filtered
        memberIds.add("user-3"); // valid
        memberIds.add("user-invalid"); // invalid - should be filtered
        request.setMemberIds(memberIds);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // Should have: user-1 (current), user-2, user-3 (null and invalid filtered out)
        assertEquals(3, response.getMembers().size());

        Set<String> resultUserIds =
                response.getMembers().stream()
                        .map(BookingResponse.MemberInfo::getUserId)
                        .collect(Collectors.toSet());
        assertTrue(resultUserIds.contains("user-1"));
        assertTrue(resultUserIds.contains("user-2"));
        assertTrue(resultUserIds.contains("user-3"));
        assertFalse(resultUserIds.contains("user-invalid"));
    }

    // ==================== Tests for calculateTotalAmount method ====================

    @Test
    @DisplayName("Should calculate total amount for SELF_PACED course with single member")
    void should_calculateTotalAmount_forSelfPacedCourse_singleMember() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(new BigDecimal("150.50"));

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice * 1 member = 150.50 * 1 = 150.50
        assertEquals(new BigDecimal("150.50"), response.getTotalAmount());
    }

    @Test
    @DisplayName("Should calculate total amount for SELF_PACED course with multiple members")
    void should_calculateTotalAmount_forSelfPacedCourse_multipleMembers() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(new BigDecimal("100.00"));

        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();
        User member2 = User.builder().userId("user-3").firstName("Bob").lastName("Jones").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-3")).thenReturn(Optional.of(member2));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-3")).thenReturn(Optional.of(member2));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        request.setMemberIds(Set.of("user-2", "user-3"));

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice * 3 members = 100.00 * 3 = 300.00
        assertEquals(new BigDecimal("300.00"), response.getTotalAmount());
        assertEquals(3, response.getMembers().size());
    }

    @Test
    @DisplayName("Should calculate total amount for ONLINE course with single member")
    void should_calculateTotalAmount_forOnlineCourse_singleMember() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("25.50"));
        request.setLessons(8);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice(25.50) * members(1) = 25.50
        assertEquals(new BigDecimal("25.50"), response.getTotalAmount());
    }

    @Test
    @DisplayName("Should calculate total amount for ONLINE course with multiple members")
    void should_calculateTotalAmount_forOnlineCourse_multipleMembers() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();
        User member2 = User.builder().userId("user-3").firstName("Bob").lastName("Jones").build();
        User member3 = User.builder().userId("user-4").firstName("Alice").lastName("Brown").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findByIdAndNotDeleted("user-3")).thenReturn(Optional.of(member2));
        when(userRepository.findByIdAndNotDeleted("user-4")).thenReturn(Optional.of(member3));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-3")).thenReturn(Optional.of(member2));
        when(userRepository.findById("user-4")).thenReturn(Optional.of(member3));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("30.00"));
        request.setLessons(12);
        request.setMemberIds(Set.of("user-2", "user-3", "user-4"));
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice(30.00) * members(4) = 120.00
        assertEquals(new BigDecimal("120.00"), response.getTotalAmount());
        assertEquals(4, response.getMembers().size());
    }

    @Test
    @DisplayName("Should calculate total amount with decimal precision")
    void should_calculateTotalAmount_withDecimalPrecision() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("33.33"));
        request.setLessons(3);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice(33.33) * members(1) = 33.33
        assertEquals(new BigDecimal("33.33"), response.getTotalAmount());
    }

    @Test
    @DisplayName("Should calculate total amount with large numbers")
    void should_calculateTotalAmount_withLargeNumbers() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("999.99"));
        request.setLessons(100);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice(999.99) * members(1) = 999.99
        assertEquals(new BigDecimal("999.99"), response.getTotalAmount());
    }

    @Test
    @DisplayName("Should calculate total amount for SELF_PACED with zero price")
    void should_calculateTotalAmount_forSelfPaced_withZeroPrice() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setType(CourseType.SELF_PACED);
        course.setPrice(BigDecimal.ZERO); // Zero price (free course)

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    @Test
    @DisplayName("Should calculate total amount for ONLINE course with one lesson")
    void should_calculateTotalAmount_forOnlineCourse_withOneLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50.00"));
        request.setLessons(1); // Only one lesson
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        // coursePrice(50.00) * members(1) = 50.00
        assertEquals(new BigDecimal("50.00"), response.getTotalAmount());
    }

    // ==================== Tests for toBookingResponse method ====================

    @Test
    @DisplayName("Should map booking to response with null course")
    void should_mapBookingToResponse_withNullCourse() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            booking.setCourse(null); // Set course to null after save
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNull(response.getCourse()); // Course should be null
        assertEquals(100L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertNotNull(response.getMembers());
    }

    @Test
    @DisplayName("Should map booking to response with null bookingMembers")
    void should_mapBookingToResponse_withNullBookingMembers() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            booking.getBookingMembers().clear(); // Clear members
                            booking.setBookingMembers(null); // Set to null
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getMembers());
        assertTrue(response.getMembers().isEmpty()); // Should return empty list, not null
    }

    @Test
    @DisplayName("Should map booking to response with empty bookingMembers list")
    void should_mapBookingToResponse_withEmptyBookingMembersList() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            booking.getBookingMembers().clear(); // Clear to empty list
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getMembers());
        assertTrue(response.getMembers().isEmpty()); // Should be empty list
    }

    @Test
    @DisplayName("Should map all booking fields correctly to response")
    void should_mapAllBookingFieldsCorrectly_toResponse() {
        // Given
        MockHelper.mockSecurityContext("student");
        User member1 = User.builder().userId("user-2").firstName("Jane").lastName("Smith").build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(userRepository.findByIdAndNotDeleted("user-2")).thenReturn(Optional.of(member1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member1));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(999L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.GROUP);
        request.setRegistrationType(RegistrationType.TRIAL);
        course.setPrice(new BigDecimal("75.50"));
        request.setLessons(15);
        request.setMemberIds(Set.of("user-2"));
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);

        // Verify all booking fields are mapped correctly
        assertEquals(999L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(RegistrationType.TRIAL, response.getRegistrationType());
        assertEquals(GroupType.GROUP, response.getGroupType());
        assertEquals(new BigDecimal("151.00"), response.getTotalAmount()); // 75.50 * 2
        assertNotNull(response.getScheduleDescription());
        assertTrue(response.getScheduleDescription().contains("Monday"));

        // Verify course is mapped
        assertNotNull(response.getCourse());
        assertEquals(1L, response.getCourse().getId());

        // Verify members are mapped with all fields
        assertNotNull(response.getMembers());
        assertEquals(2, response.getMembers().size());

        // Check owner member
        BookingResponse.MemberInfo owner =
                response.getMembers().stream()
                        .filter(m -> "OWNER".equals(m.getRole()))
                        .findFirst()
                        .orElse(null);
        assertNotNull(owner);
        assertEquals("user-1", owner.getUserId());
        assertEquals("OWNER", owner.getRole());
        assertEquals(BookingMemberStatus.APPROVED, owner.getStatus());

        // Check regular member
        BookingResponse.MemberInfo member =
                response.getMembers().stream()
                        .filter(m -> "MEMBER".equals(m.getRole()))
                        .findFirst()
                        .orElse(null);
        assertNotNull(member);
        assertEquals("user-2", member.getUserId());
        assertEquals("MEMBER", member.getRole());
        assertEquals(BookingMemberStatus.WAITING, member.getStatus());
    }

    @Test
    @DisplayName("Should map booking with null schedule description")
    void should_mapBooking_withNullScheduleDescription() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(null); // Null schedules

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNull(response.getScheduleDescription()); // Should be null
    }

    @Test
    @DisplayName("Should map course with combo false")
    void should_mapCourse_withComboFalse() {
        // Given
        MockHelper.mockSecurityContext("student");
        course.setIsCombo(false); // Not a combo

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_Id("user-1", 1L))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(100L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourseId(1L);
        request.setGroupType(GroupType.INDIVIDUAL);
        request.setRegistrationType(RegistrationType.REGULAR);
        course.setPrice(new BigDecimal("50"));
        request.setLessons(10);
        request.setWeeklySchedules(createWeeklySchedules());

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getCourse());
        assertFalse(response.getCourse().getCombo()); // Combo should be false
    }

    @Test
    @DisplayName("Should join class successfully")
    void should_joinClass_successfully() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(200L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(10);
        request.setLessons(5);
        course.setPrice(new BigDecimal("30"));

        // When
        BookingResponse response = bookingService.joinClass(request);

        // Then
        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(GroupType.INDIVIDUAL, response.getGroupType());
        assertEquals(RegistrationType.REGULAR, response.getRegistrationType());
        assertEquals(new BigDecimal("30"), response.getTotalAmount()); // Course price only
        assertTrue(response.getScheduleDescription().contains("Join Class"));
        assertEquals(1, response.getMembers().size());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(course, savedBooking.getCourse());
    }

    @Test
    @DisplayName("Should throw when joining class with deadline passed")
    void should_throw_when_joiningClass_deadlinePassed() {
        // Given
        MockHelper.mockSecurityContext("student");
        tutorClass.setLastJoinDate(LocalDate.now().minusDays(1)); // Deadline passed

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(10);
        request.setLessons(5);
        course.setPrice(new BigDecimal("30"));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.joinClass(request));
        assertEquals(ErrorCode.CLASS_JOIN_DEADLINE_PASSED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when class not found for join")
    void should_throw_when_classNotFound_forJoin() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.empty());

        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(10);
        request.setLessons(5);
        course.setPrice(new BigDecimal("30"));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.joinClass(request));
        assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should join class successfully when lastJoinDate is null (no deadline)")
    void should_joinClass_successfully_when_lastJoinDateIsNull() {
        // Given
        MockHelper.mockSecurityContext("student");
        tutorClass.setLastJoinDate(null); // No deadline

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(200L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(10);
        request.setLessons(5);
        course.setPrice(new BigDecimal("30"));

        // When
        BookingResponse response = bookingService.joinClass(request);

        // Then
        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(new BigDecimal("30"), response.getTotalAmount()); // Course price only
    }

    @Test
    @DisplayName("Should join class successfully when lastJoinDate equals today")
    void should_joinClass_successfully_when_lastJoinDateEqualsToday() {
        // Given
        MockHelper.mockSecurityContext("student");
        tutorClass.setLastJoinDate(LocalDate.now()); // Deadline is today (still allowed)

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking booking = invocation.getArgument(0);
                            booking.setId(200L);
                            return booking;
                        });
        when(notificationService.createAndSendNotification(
                        anyString(), anyString(), any(), any(), any()))
                .thenReturn(null);

        JoinClassRequest request = new JoinClassRequest();
        request.setClassId(10);
        request.setLessons(5);
        course.setPrice(new BigDecimal("30"));

        // When
        BookingResponse response = bookingService.joinClass(request);

        // Then
        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(new BigDecimal("30"), response.getTotalAmount()); // Course price only
    }

    @Test
    @DisplayName("Should get booking state - guest user not authenticated")
    void should_getBookingState_guestUser() {
        // Given
        MockHelper.mockSecurityContext("anonymousUser");

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertFalse(response.alreadyJoined());
        assertNull(response.joinedClassId());
        assertTrue(response.joinableClasses().isEmpty());

        // Verify no repository calls were made
        verify(userRepository, never()).findByUsernameAndNotDeleted(anyString());
        verify(courseRepository, never()).findByIdAndIsDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("Should get booking state - not joined; open classes available")
    void should_getBookingState_openClassesAvailable() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(false);

        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.empty());

        when(tutorClassRepository.findByCourse_IdAndIsDeletedFalse(1L))
                .thenReturn(List.of(tutorClass));

        List<Object[]> mockResult = new ArrayList<>();
        mockResult.add(new Object[] {10L, 5L});
        when(classSessionRepository.countUpcomingSessionsByClassIds(
                        anyList(), any(LocalDate.class)))
                .thenReturn(mockResult);

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertFalse(response.alreadyJoined());
        assertNull(response.joinedClassId());
        assertEquals(1, response.joinableClasses().size()); // NEW
    }

    @Test
    @DisplayName("Should get booking state - not joined; no open classes")
    void should_getBookingState_noOpenClasses() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(false);

        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.empty());

        when(tutorClassRepository.findByCourse_IdAndIsDeletedFalse(1L))
                .thenReturn(Collections.emptyList());

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertFalse(response.alreadyJoined());
        assertNull(response.joinedClassId());
        assertTrue(response.joinableClasses().isEmpty());
    }

    @Test
    @DisplayName("Should get booking state - already joined via booking")
    void should_getBookingState_alreadyJoined() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        // User already joined via booking
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(true);

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.alreadyJoined());
        assertNull(response.joinedClassId());
        assertTrue(response.joinableClasses().isEmpty());
    }

    @Test
    @DisplayName("Should get booking state - already enrolled in class")
    void should_getBookingState_alreadyEnrolledInClass() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        // No bookingMember record
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(false);

        // User has an enrollment
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.of(enrollment));

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.alreadyJoined());
        assertEquals(tutorClass.getId(), response.joinedClassId());
        assertTrue(response.joinableClasses().isEmpty());
    }

    @Test
    @DisplayName("Should get booking state - filter out full classes")
    void should_getBookingState_filterOutFullClasses() {
        // Given
        MockHelper.mockSecurityContext("student");

        TutorClass fullClass =
                TutorClass.builder()
                        .id(20L)
                        .course(course)
                        .maxStudents(5)
                        .currentStudents(5) // Full
                        .lastJoinDate(LocalDate.now().plusDays(7))
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(false);

        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.empty());

        when(tutorClassRepository.findByCourse_IdAndIsDeletedFalse(1L))
                .thenReturn(List.of(tutorClass, fullClass));

        // FIX: use tutorClass.id, not some unrelated ID
        List<Object[]> mockResult = new ArrayList<>();
        mockResult.add(new Object[] {tutorClass.getId(), 5L});

        when(classSessionRepository.countUpcomingSessionsByClassIds(
                        anyList(), any(LocalDate.class)))
                .thenReturn(mockResult);

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);

        // Only the non-full class appears
        assertEquals(1, response.joinableClasses().size());
        assertTrue(response.joinableClasses().stream().noneMatch(c -> c.id().equals(20L)));
    }

    @Test
    @DisplayName("Should approve booking successfully")
    void should_approveBooking_successfully() {
        // Given
        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(member)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.approveBooking(100L);

        // Then
        assertEquals(BookingStatus.APPROVED, booking.getBookingStatus());
        verify(bookingRepository).findById(100L);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should throw when booking not found for approval")
    void should_throw_when_bookingNotFound_forApproval() {
        // Given
        when(bookingRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.approveBooking(100L));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should approve booking with mixed member statuses")
    void should_approveBooking_withMixedMemberStatuses() {
        // Given
        BookingMember approvedMember =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember waitingMember =
                BookingMember.builder()
                        .userId("user-2")
                        .role("MEMBER")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember rejectedMember =
                BookingMember.builder()
                        .userId("user-3")
                        .role("MEMBER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(approvedMember, waitingMember, rejectedMember)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.approveBooking(100L);

        // Then
        assertEquals(BookingStatus.APPROVED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should include approved and waiting members, but not rejected
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertEquals(2, notifiedMembers.size());
        assertTrue(notifiedMembers.contains("user-1"));
        assertTrue(notifiedMembers.contains("user-2"));
        assertFalse(notifiedMembers.contains("user-3"));
    }

    @Test
    @DisplayName("Should approve booking with empty members list")
    void should_approveBooking_withEmptyMembersList() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>()) // Empty list
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.approveBooking(100L);

        // Then
        assertEquals(BookingStatus.APPROVED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should send empty list
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertTrue(notifiedMembers.isEmpty());
    }

    @Test
    @DisplayName("Should approve booking when all members are rejected")
    void should_approveBooking_whenAllMembersRejected() {
        // Given
        BookingMember rejectedMember1 =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        BookingMember rejectedMember2 =
                BookingMember.builder()
                        .userId("user-2")
                        .role("MEMBER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(rejectedMember1, rejectedMember2)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.approveBooking(100L);

        // Then
        assertEquals(BookingStatus.APPROVED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should send empty list (all rejected)
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertTrue(notifiedMembers.isEmpty());
    }

    @Test
    @DisplayName("Should reject booking successfully")
    void should_rejectBooking_successfully() {
        // Given
        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(member)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.rejectBooking(100L);

        // Then
        assertEquals(BookingStatus.REJECTED, booking.getBookingStatus());
        verify(bookingRepository).findById(100L);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should throw when booking not found for rejection")
    void should_throw_when_bookingNotFound_forRejection() {
        // Given
        when(bookingRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.rejectBooking(100L));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should reject booking with mixed member statuses")
    void should_rejectBooking_withMixedMemberStatuses() {
        // Given
        BookingMember approvedMember =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember waitingMember =
                BookingMember.builder()
                        .userId("user-2")
                        .role("MEMBER")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember rejectedMember =
                BookingMember.builder()
                        .userId("user-3")
                        .role("MEMBER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(approvedMember, waitingMember, rejectedMember)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.rejectBooking(100L);

        // Then
        assertEquals(BookingStatus.REJECTED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should include approved and waiting members, but not rejected
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertEquals(2, notifiedMembers.size());
        assertTrue(notifiedMembers.contains("user-1"));
        assertTrue(notifiedMembers.contains("user-2"));
        assertFalse(notifiedMembers.contains("user-3"));
    }

    @Test
    @DisplayName("Should reject booking with empty members list")
    void should_rejectBooking_withEmptyMembersList() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>()) // Empty list
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.rejectBooking(100L);

        // Then
        assertEquals(BookingStatus.REJECTED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should send empty list
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertTrue(notifiedMembers.isEmpty());
    }

    @Test
    @DisplayName("Should reject booking when all members are rejected")
    void should_rejectBooking_whenAllMembersRejected() {
        // Given
        BookingMember rejectedMember1 =
                BookingMember.builder()
                        .userId("user-1")
                        .role("OWNER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        BookingMember rejectedMember2 =
                BookingMember.builder()
                        .userId("user-2")
                        .role("MEMBER")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(100L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(rejectedMember1, rejectedMember2)))
                        .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing()
                .when(notificationService)
                .createAndSendNotifications(anyList(), anyString(), any(), any(), any());

        // When
        bookingService.rejectBooking(100L);

        // Then
        assertEquals(BookingStatus.REJECTED, booking.getBookingStatus());

        ArgumentCaptor<List<String>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService)
                .createAndSendNotifications(
                        memberIdsCaptor.capture(), anyString(), any(), any(), any());

        // Should send empty list (all rejected)
        List<String> notifiedMembers = memberIdsCaptor.getValue();
        assertTrue(notifiedMembers.isEmpty());
    }

    @Test
    @DisplayName("Should throw when course not found for getBookingState")
    void should_throw_when_courseNotFound_forGetBookingState() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.getBookingState(1L));
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should return guest response when authentication is null")
    void should_returnGuestResponse_when_authenticationIsNull() {
        // Given
        SecurityContextHolder.clearContext(); // Clear security context to make authentication null

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertFalse(response.alreadyJoined());
        assertNull(response.joinedClassId());
        assertTrue(response.joinableClasses().isEmpty());

        // Verify no repository calls
        verify(userRepository, never()).findByUsernameAndNotDeleted(anyString());
        verify(courseRepository, never()).findByIdAndIsDeletedFalse(anyLong());
    }

    @Test
    @DisplayName("Should throw UNAUTHENTICATED when authenticated user not found in DB")
    void should_throw_unauthenticated_when_authenticatedUserNotFoundInDB() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsernameAndNotDeleted("nonexistent"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> bookingService.getBookingState(1L));
        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should return already joined when both booking and enrollment exist")
    void should_returnAlreadyJoined_when_bothBookingAndEnrollmentExist() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        // Both booking and enrollment exist
        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(true);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.of(enrollment));

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.alreadyJoined());
        assertEquals(tutorClass.getId(), response.joinedClassId()); // From enrollment
        assertTrue(response.joinableClasses().isEmpty());

        // Should short-circuit and not query for joinable classes
        verify(tutorClassRepository, never()).findByCourse_IdAndIsDeletedFalse(anyLong());
        verify(classSessionRepository, never())
                .countUpcomingSessionsByClassIds(anyList(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should skip session count query when no enrollable classes (empty classIds list)")
    void should_skipSessionCountQuery_when_noEnrollableClasses() {
        // Given
        MockHelper.mockSecurityContext("student");

        TutorClass fullClass =
                TutorClass.builder()
                        .id(20L)
                        .course(course)
                        .maxStudents(5)
                        .currentStudents(5) // Full - cannot enroll
                        .lastJoinDate(LocalDate.now().plusDays(7))
                        .build();

        TutorClass pastDeadlineClass =
                TutorClass.builder()
                        .id(21L)
                        .course(course)
                        .maxStudents(10)
                        .currentStudents(3)
                        .lastJoinDate(LocalDate.now().minusDays(1)) // Deadline passed
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));

        when(bookingMemberRepository.existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
                        "user-1", 1L))
                .thenReturn(false);

        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id("user-1", 1L))
                .thenReturn(Optional.empty());

        // Return classes but they all fail canEnroll() check
        when(tutorClassRepository.findByCourse_IdAndIsDeletedFalse(1L))
                .thenReturn(List.of(fullClass, pastDeadlineClass));

        // When
        BookingStateResponse response = bookingService.getBookingState(1L);

        // Then
        assertNotNull(response);
        assertFalse(response.alreadyJoined());
        assertTrue(response.joinableClasses().isEmpty());

        // Should NOT call session count query because classIds list is empty after filtering
        verify(classSessionRepository, never())
                .countUpcomingSessionsByClassIds(anyList(), any(LocalDate.class));
    }

    private List<WeeklySchedule> createWeeklySchedules() {
        return Collections.singletonList(
                WeeklySchedule.builder()
                        .dayOfWeek(1) // Monday
                        .slotNumbers(Arrays.asList(1, 2))
                        .build());
    }

    @Test
    @DisplayName("Should update invite - accept")
    void should_updateInvite_accept() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100"))
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();
        booking.getBookingMembers().add(member);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, true, 1L);

        // Then
        assertEquals(BookingMemberStatus.APPROVED, member.getStatus());
        verify(notificationService).updateNotification(eq(1L), eq("user-1"), any());
    }

    @Test
    @DisplayName("Should update invite - reject")
    void should_updateInvite_reject() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100")) // 2 members * 50
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        booking.getBookingMembers().add(member1);
        booking.getBookingMembers().add(member2);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member1));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        assertEquals(BookingMemberStatus.REJECTED, member1.getStatus());
        // Total amount should be recalculated: 100 / 2 members = 50 per member.
        // Remaining 1 member * 50 = 50.
        assertEquals(new BigDecimal("50.00"), booking.getTotalAmount());
        verify(notificationService).updateNotification(eq(1L), eq("user-1"), any());
    }

    @Test
    @DisplayName("Should throw AppException when user not authenticated in updateOrRejectInvite")
    void should_throwException_whenUnauthenticated_inUpdateOrRejectInvite() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.updateOrRejectInvite(100L, true, 1L);
                        });

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verify(userRepository).findByUsernameAndNotDeleted("student");
        verify(bookingMemberRepository, never()).findByUserIdAndBookingId(anyString(), anyLong());
        verify(bookingMemberRepository, never()).save(any());
        verify(notificationService, never()).updateNotification(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw AppException when booking not found in updateOrRejectInvite")
    void should_throwException_whenBookingNotFound_inUpdateOrRejectInvite() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.updateOrRejectInvite(100L, true, 1L);
                        });

        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findByUsernameAndNotDeleted("student");
        verify(bookingMemberRepository).findByUserIdAndBookingId("user-1", 100L);
        verify(bookingMemberRepository, never()).save(any());
        verify(notificationService, never()).updateNotification(anyLong(), anyString(), any());
    }

    // ==================== Additional tests for updateOrRejectInvite ====================

    @Test
    @DisplayName("Should reject invite and recalculate amount excluding already rejected members")
    void should_rejectInvite_andRecalculateAmount_excludingRejectedMembers() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("150")) // 3 active members * 50
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember member3 =
                BookingMember.builder()
                        .userId("user-3")
                        .booking(booking)
                        .status(BookingMemberStatus.REJECTED) // Already rejected
                        .build();

        BookingMember member4 =
                BookingMember.builder()
                        .userId("user-4")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        booking.getBookingMembers().add(member1);
        booking.getBookingMembers().add(member2);
        booking.getBookingMembers().add(member3);
        booking.getBookingMembers().add(member4);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member1));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        assertEquals(BookingMemberStatus.REJECTED, member1.getStatus());
        // Active members = 3 (user-1, user-2, user-4) - excluding user-3 who was already rejected
        // Amount per member = 150 / 3 = 50
        // Remaining members = 2 (user-2, user-4)
        // New total = 50 * 2 = 100
        assertEquals(new BigDecimal("100.00"), booking.getTotalAmount());
    }

    @Test
    @DisplayName("Should reject invite when only one member remains")
    void should_rejectInvite_whenOnlyOneMemberRemains() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100")) // 2 members * 50
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        booking.getBookingMembers().add(member1);
        booking.getBookingMembers().add(member2);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member1));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        assertEquals(BookingMemberStatus.REJECTED, member1.getStatus());
        // Active members = 2, remaining = 1
        // Amount per member = 100 / 2 = 50
        // New total = 50 * 1 = 50
        assertEquals(new BigDecimal("50.00"), booking.getTotalAmount());
    }

    @Test
    @DisplayName("Should accept invite and verify notification message")
    void should_acceptInvite_andVerifyNotificationMessage() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100"))
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();
        booking.getBookingMembers().add(member);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, true, 1L);

        // Then
        ArgumentCaptor<NotificationUpdateRequest> updateRequestCaptor =
                ArgumentCaptor.forClass(NotificationUpdateRequest.class);
        verify(notificationService)
                .updateNotification(eq(1L), eq("user-1"), updateRequestCaptor.capture());

        NotificationUpdateRequest capturedRequest = updateRequestCaptor.getValue();
        assertEquals(NotificationType.TYPICAL, capturedRequest.getType());
        assertEquals("You have accepted the booking invitation", capturedRequest.getMessage());
        assertNull(capturedRequest.getActionLink());
    }

    @Test
    @DisplayName("Should reject invite and verify notification message")
    void should_rejectInvite_andVerifyNotificationMessage() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100"))
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();
        booking.getBookingMembers().add(member);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        ArgumentCaptor<NotificationUpdateRequest> updateRequestCaptor =
                ArgumentCaptor.forClass(NotificationUpdateRequest.class);
        verify(notificationService)
                .updateNotification(eq(1L), eq("user-1"), updateRequestCaptor.capture());

        NotificationUpdateRequest capturedRequest = updateRequestCaptor.getValue();
        assertEquals(NotificationType.TYPICAL, capturedRequest.getType());
        assertEquals("You have rejected the booking invitation", capturedRequest.getMessage());
        assertNull(capturedRequest.getActionLink());
    }

    @Test
    @DisplayName("Should calculate amount with rounding HALF_UP when rejecting")
    void should_calculateAmountWithRounding_whenRejecting() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100")) // Will be divided by 3
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember member3 =
                BookingMember.builder()
                        .userId("user-3")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        booking.getBookingMembers().add(member1);
        booking.getBookingMembers().add(member2);
        booking.getBookingMembers().add(member3);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member1));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        assertEquals(BookingMemberStatus.REJECTED, member1.getStatus());
        // Active members = 3, remaining = 2
        // Amount per member = 100 / 3 = 33.333... -> 33.33 (HALF_UP with 2 decimal places)
        // New total = 33.33 * 2 = 66.66
        assertEquals(new BigDecimal("66.66"), booking.getTotalAmount());
    }

    @Test
    @DisplayName("Should accept invite from APPROVED member")
    void should_acceptInvite_fromApprovedMember() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("100"))
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.APPROVED) // Already approved
                        .build();
        booking.getBookingMembers().add(member);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, true, 1L);

        // Then
        assertEquals(BookingMemberStatus.APPROVED, member.getStatus()); // Still approved
        verify(bookingMemberRepository).save(member);
    }

    @Test
    @DisplayName("Should reject all other members leaving zero remaining")
    void should_rejectInvite_leavingZeroRemaining() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("50")) // 1 active member * 50
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .booking(booking)
                        .status(BookingMemberStatus.REJECTED) // Already rejected
                        .build();

        BookingMember member3 =
                BookingMember.builder()
                        .userId("user-3")
                        .booking(booking)
                        .status(BookingMemberStatus.REJECTED) // Already rejected
                        .build();

        booking.getBookingMembers().add(member1);
        booking.getBookingMembers().add(member2);
        booking.getBookingMembers().add(member3);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member1));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, false, 1L);

        // Then
        assertEquals(BookingMemberStatus.REJECTED, member1.getStatus());
        // Active members = 1 (only user-1), remaining = 0
        // Amount per member = 50 / 1 = 50
        // New total = 50 * 0 = 0
        assertEquals(new BigDecimal("0.00"), booking.getTotalAmount());
    }

    @Test
    @DisplayName("Should accept invite without affecting total amount")
    void should_acceptInvite_withoutAffectingTotalAmount() {
        // Given
        MockHelper.mockSecurityContext("student");
        Booking booking =
                Booking.builder()
                        .id(100L)
                        .totalAmount(new BigDecimal("200.50"))
                        .bookingMembers(new HashSet<>())
                        .build();

        BookingMember member =
                BookingMember.builder()
                        .userId("user-1")
                        .booking(booking)
                        .status(BookingMemberStatus.WAITING)
                        .build();
        booking.getBookingMembers().add(member);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingMemberRepository.findByUserIdAndBookingId("user-1", 100L))
                .thenReturn(Optional.of(member));
        when(bookingMemberRepository.save(any(BookingMember.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(notificationService.updateNotification(anyLong(), anyString(), any()))
                .thenReturn(null);

        // When
        bookingService.updateOrRejectInvite(100L, true, 1L);

        // Then
        assertEquals(BookingMemberStatus.APPROVED, member.getStatus());
        // Total amount should remain unchanged when accepting
        assertEquals(new BigDecimal("200.50"), booking.getTotalAmount());
    }

    @Test
    @DisplayName("Should get admin booking list")
    void should_getAdminBookingList() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build())))
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getPendingMemberCount());
    }

    @Test
    @DisplayName("Should get admin booking list with status filter")
    void should_getAdminBookingList_withStatusFilter() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findByBookingStatus(BookingStatus.APPROVED, pageable))
                .thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(BookingStatus.APPROVED, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(BookingStatus.APPROVED, result.getContent().get(0).getBookingStatus());
        verify(bookingRepository).findByBookingStatus(BookingStatus.APPROVED, pageable);
    }

    @Test
    @DisplayName("Should get admin booking list with search term")
    void should_getAdminBookingList_withSearchTerm() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.searchByStatusAndTerm(null, "test", pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, "test", 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookingRepository).searchByStatusAndTerm(null, "test", pageable);
    }

    // ==================== Additional tests for getAdminBookingList ====================

    @Test
    @DisplayName("Should get admin booking list with empty search string")
    void should_getAdminBookingList_withEmptySearchString() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, "", 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // Should use findAll because empty string is treated as no search
        verify(bookingRepository).findAll(pageable);
        verify(bookingRepository, never()).searchByStatusAndTerm(any(), any(), any());
    }

    @Test
    @DisplayName("Should get admin booking list with whitespace-only search string")
    void should_getAdminBookingList_withWhitespaceOnlySearch() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, "   ", 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // Should use findAll because whitespace is trimmed to empty
        verify(bookingRepository).findAll(pageable);
        verify(bookingRepository, never()).searchByStatusAndTerm(any(), any(), any());
    }

    @Test
    @DisplayName("Should get admin booking list with search term and status filter")
    void should_getAdminBookingList_withSearchTermAndStatusFilter() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.searchByStatusAndTerm(BookingStatus.APPROVED, "math", pageable))
                .thenReturn(page);

        // When
        var result =
                bookingService.getAdminBookingList(BookingStatus.APPROVED, "math", 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(BookingStatus.APPROVED, result.getContent().get(0).getBookingStatus());
        verify(bookingRepository).searchByStatusAndTerm(BookingStatus.APPROVED, "math", pageable);
    }

    @Test
    @DisplayName("Should get admin booking list with REJECTED status")
    void should_getAdminBookingList_withRejectedStatus() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.REJECTED)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findByBookingStatus(BookingStatus.REJECTED, pageable))
                .thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(BookingStatus.REJECTED, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(BookingStatus.REJECTED, result.getContent().get(0).getBookingStatus());
        verify(bookingRepository).findByBookingStatus(BookingStatus.REJECTED, pageable);
    }

    @Test
    @DisplayName("Should get admin booking list with PAID status")
    void should_getAdminBookingList_withPaidStatus() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PAID)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findByBookingStatus(BookingStatus.PAID, pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(BookingStatus.PAID, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(BookingStatus.PAID, result.getContent().get(0).getBookingStatus());
        verify(bookingRepository).findByBookingStatus(BookingStatus.PAID, pageable);
    }

    @Test
    @DisplayName("Should get empty admin booking list")
    void should_getEmptyAdminBookingList() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> emptyPage = new PageImpl<>(List.of());

        when(bookingRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should get admin booking list with multiple pages")
    void should_getAdminBookingList_withMultiplePages() {
        // Given
        PageRequest pageable =
                PageRequest.of(
                        1,
                        5,
                        Sort.by(Sort.Direction.DESC, "createdAt")); // Second page, 5 items per page
        Booking booking1 =
                Booking.builder()
                        .id(6L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .build();
        Booking booking2 =
                Booking.builder()
                        .id(7L)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>())
                        .build();
        Page<Booking> page =
                new PageImpl<>(List.of(booking1, booking2), pageable, 12); // 12 total items

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 1, 5, null);

        // Then
        assertNotNull(result);
        assertEquals(12, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getNumber()); // Page number
        assertEquals(5, result.getSize()); // Page size
        assertEquals(3, result.getTotalPages()); // 12 items / 5 per page = 3 pages
    }

    // ==================== Tests for toBookingAdminListItem method ====================

    @Test
    @DisplayName("Should map booking to admin list item with null course")
    void should_mapBookingToAdminListItem_withNullCourse() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(null) // Null course
                        .bookingStatus(BookingStatus.PENDING)
                        .registrationType(RegistrationType.REGULAR)
                        .groupType(GroupType.INDIVIDUAL)
                        .totalAmount(new BigDecimal("100"))
                        .scheduleDescription("Test schedule")
                        .bookingMembers(new HashSet<>())
                        .transactions(null)
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        var item = result.getContent().get(0);
        assertNull(item.getCourseName()); // Course name should be null
        assertEquals(1L, item.getId());
        assertEquals(BookingStatus.PENDING, item.getBookingStatus());
    }

    @Test
    @DisplayName("Should map booking with no pending members")
    void should_mapBooking_withNoPendingMembers() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>(List.of(member1, member2)))
                        .transactions(null)
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertEquals(0, item.getPendingMemberCount()); // No WAITING members
        assertEquals(2, item.getMemberCount());
    }

    @Test
    @DisplayName("Should map booking with multiple pending members")
    void should_mapBooking_withMultiplePendingMembers() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member3 =
                BookingMember.builder()
                        .userId("user-3")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(member1, member2, member3)))
                        .transactions(null)
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertEquals(3, item.getPendingMemberCount()); // 3 WAITING members
        assertEquals(3, item.getMemberCount());
    }

    @Test
    @DisplayName("Should map booking with mixed member statuses")
    void should_mapBooking_withMixedMemberStatuses() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        BookingMember member3 =
                BookingMember.builder()
                        .userId("user-3")
                        .status(BookingMemberStatus.REJECTED)
                        .build();

        BookingMember member4 =
                BookingMember.builder()
                        .userId("user-4")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>(List.of(member1, member2, member3, member4)))
                        .transactions(null)
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertEquals(2, item.getPendingMemberCount()); // 2 WAITING members
        assertEquals(4, item.getMemberCount()); // Total 4 members
    }

    @Test
    @DisplayName("Should map booking with null transactions")
    void should_mapBooking_withNullTransactions() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .transactions(null) // Null transactions
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertFalse(item.getHasUnpaidTransactions()); // Should be false when null
    }

    @Test
    @DisplayName("Should map booking with empty transactions list")
    void should_mapBooking_withEmptyTransactionsList() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>())
                        .transactions(new HashSet<>()) // Empty set
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertFalse(item.getHasUnpaidTransactions()); // Should be false when empty
    }

    @Test
    @DisplayName("Should map booking with all paid transactions")
    void should_mapBooking_withAllPaidTransactions() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Transaction transaction1 = Transaction.builder().id(1L).status(PaymentStatus.PAID).build();

        Transaction transaction2 = Transaction.builder().id(2L).status(PaymentStatus.PAID).build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.PAID)
                        .bookingMembers(new HashSet<>())
                        .transactions(new HashSet<>(Set.of(transaction1, transaction2)))
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertFalse(item.getHasUnpaidTransactions()); // All paid, so false
    }

    @Test
    @DisplayName("Should map booking with unpaid transactions")
    void should_mapBooking_withUnpaidTransactions() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Transaction transaction1 = Transaction.builder().id(1L).status(PaymentStatus.PAID).build();

        Transaction transaction2 =
                Transaction.builder()
                        .id(2L)
                        .status(PaymentStatus.PENDING) // Unpaid
                        .build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.APPROVED)
                        .bookingMembers(new HashSet<>())
                        .transactions(new HashSet<>(Set.of(transaction1, transaction2)))
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertTrue(item.getHasUnpaidTransactions()); // Has unpaid transaction
    }

    @Test
    @DisplayName("Should map booking with failed transaction")
    void should_mapBooking_withFailedTransaction() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Transaction transaction =
                Transaction.builder()
                        .id(1L)
                        .status(PaymentStatus.FAILED) // Failed transaction
                        .build();

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(new HashSet<>())
                        .transactions(new HashSet<>(Set.of(transaction)))
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        var item = result.getContent().get(0);
        assertTrue(item.getHasUnpaidTransactions()); // Failed is not PAID
    }

    @Test
    @DisplayName("Should map all booking fields correctly to admin list item")
    void should_mapAllBookingFieldsCorrectly_toAdminListItem() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime createdAt = LocalDate.of(2024, 12, 1).atStartOfDay();

        BookingMember member1 =
                BookingMember.builder()
                        .userId("user-1")
                        .status(BookingMemberStatus.WAITING)
                        .build();

        BookingMember member2 =
                BookingMember.builder()
                        .userId("user-2")
                        .status(BookingMemberStatus.APPROVED)
                        .build();

        Transaction transaction =
                Transaction.builder().id(1L).status(PaymentStatus.PENDING).build();

        Booking booking =
                Booking.builder()
                        .id(999L)
                        .course(course)
                        .bookingStatus(BookingStatus.PENDING)
                        .registrationType(RegistrationType.TRIAL)
                        .groupType(GroupType.GROUP)
                        .totalAmount(new BigDecimal("1500.75"))
                        .scheduleDescription("Mon: 09:00-10:30, Wed: 14:00-15:30")
                        .bookingMembers(new HashSet<>(List.of(member1, member2)))
                        .transactions(new HashSet<>(Set.of(transaction)))
                        .createdAt(createdAt)
                        .createdBy("admin-user")
                        .build();
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = bookingService.getAdminBookingList(null, null, 0, 10, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        var item = result.getContent().get(0);

        // Verify all fields are mapped correctly
        assertEquals(999L, item.getId());
        assertEquals(BookingStatus.PENDING, item.getBookingStatus());
        assertEquals(RegistrationType.TRIAL, item.getRegistrationType());
        assertEquals(GroupType.GROUP, item.getGroupType());
        assertEquals(new BigDecimal("1500.75"), item.getTotalAmount());
        assertEquals("Mon: 09:00-10:30, Wed: 14:00-15:30", item.getScheduleDescription());
        assertEquals("Math Course", item.getCourseName());
        assertEquals(2, item.getMemberCount());
        assertEquals(1, item.getPendingMemberCount());
        assertTrue(item.getHasUnpaidTransactions());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals("admin-user", item.getCreatedBy());
    }

    @Test
    @DisplayName("Should get admin booking detail")
    void should_getAdminBookingDetail() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build())))
                        .transactions(
                                Set.of(
                                        Transaction.builder()
                                                .transactionId("txn1")
                                                .status(PaymentStatus.PAID)
                                                .amount(new BigDecimal("100"))
                                                .paymentDate(java.time.LocalDateTime.now())
                                                .build()))
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getBooking().getId());
        assertEquals("owner", result.getBookingOwnerUserId());
        assertTrue(result.getIsPaid());
        assertEquals(1, result.getTransactions().size());
    }

    @Test
    @DisplayName("Should throw AppException when booking not found in getAdminBookingDetail")
    void should_throwException_whenBookingNotFound_inGetAdminBookingDetail() {
        // Given
        Long bookingId = 999L;
        when(bookingRepository.findWithDetailsById(bookingId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.getAdminBookingDetail(bookingId);
                        });

        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
        verify(bookingRepository).findWithDetailsById(bookingId);
    }

    // ==================== Tests for toBookingAdminDetail method ====================

    @Test
    @DisplayName("Should get admin booking detail with no owner")
    void should_getAdminBookingDetail_withNoOwner() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("member1")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build())))
                        .transactions(null)
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertNull(result.getBookingOwnerUserId()); // No owner
        assertNull(result.getBookingOwner()); // No owner
    }

    @Test
    @DisplayName("Should get admin booking detail with rejected members")
    void should_getAdminBookingDetail_withRejectedMembers() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member1")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.REJECTED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member2")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.REJECTED)
                                                        .build())))
                        .transactions(null)
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getRejectedMemberCount());
        assertEquals(2, result.getRejectedMembers().size());
        assertTrue(
                result.getRejectedMembers().stream()
                        .anyMatch(m -> "member1".equals(m.getUserId())));
        assertTrue(
                result.getRejectedMembers().stream()
                        .anyMatch(m -> "member2".equals(m.getUserId())));
    }

    @Test
    @DisplayName("Should get admin booking detail with mixed member statuses")
    void should_getAdminBookingDetail_withMixedMemberStatuses() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member1")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member2")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member3")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member4")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.REJECTED)
                                                        .build())))
                        .transactions(null)
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getMemberCount()); // Total members
        assertEquals(2, result.getApprovedMemberCount()); // 2 APPROVED
        assertEquals(2, result.getWaitingMemberCount()); // 2 WAITING
        assertEquals(1, result.getRejectedMemberCount()); // 1 REJECTED
    }

    @Test
    @DisplayName("Should get admin booking detail with null transactions")
    void should_getAdminBookingDetail_withNullTransactions() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build())))
                        .transactions(null) // Null transactions
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsPaid()); // Should be false
        assertTrue(result.getTransactions().isEmpty()); // Empty list
    }

    @Test
    @DisplayName("Should get admin booking detail with empty transactions")
    void should_getAdminBookingDetail_withEmptyTransactions() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build())))
                        .transactions(new HashSet<>()) // Empty set
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsPaid()); // Should be false
        assertTrue(result.getTransactions().isEmpty()); // Empty list
    }

    @Test
    @DisplayName("Should get admin booking detail with isPaid true when any transaction is PAID")
    void should_getAdminBookingDetail_withIsPaidTrue() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PAID)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build())))
                        .transactions(
                                Set.of(
                                        Transaction.builder()
                                                .transactionId("txn1")
                                                .status(PaymentStatus.PENDING)
                                                .amount(new BigDecimal("50"))
                                                .paymentDate(LocalDateTime.now())
                                                .build(),
                                        Transaction.builder()
                                                .transactionId("txn2")
                                                .status(PaymentStatus.PAID) // Has PAID
                                                .amount(new BigDecimal("100"))
                                                .paymentDate(LocalDateTime.now())
                                                .build()))
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsPaid()); // Should be true
        assertEquals(2, result.getTransactions().size());
    }

    @Test
    @DisplayName("Should get admin booking detail with isPaid false when no PAID transactions")
    void should_getAdminBookingDetail_withIsPaidFalse() {
        // Given
        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PENDING)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build())))
                        .transactions(
                                Set.of(
                                        Transaction.builder()
                                                .transactionId("txn1")
                                                .status(PaymentStatus.PENDING)
                                                .amount(new BigDecimal("50"))
                                                .paymentDate(LocalDateTime.now())
                                                .build(),
                                        Transaction.builder()
                                                .transactionId("txn2")
                                                .status(PaymentStatus.FAILED)
                                                .amount(new BigDecimal("100"))
                                                .paymentDate(LocalDateTime.now())
                                                .build()))
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsPaid()); // Should be false (no PAID)
        assertEquals(2, result.getTransactions().size());
    }

    @Test
    @DisplayName("Should get admin booking detail with transaction summaries mapped correctly")
    void should_getAdminBookingDetail_withTransactionSummariesMapped() {
        // Given
        LocalDateTime paymentDate = LocalDateTime.of(2024, 12, 1, 10, 30);

        Booking booking =
                Booking.builder()
                        .id(1L)
                        .bookingStatus(BookingStatus.PAID)
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build())))
                        .transactions(
                                Set.of(
                                        Transaction.builder()
                                                .transactionId("TXN-12345")
                                                .status(PaymentStatus.PAID)
                                                .amount(new BigDecimal("250.50"))
                                                .paymentDate(paymentDate)
                                                .build()))
                        .build();

        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTransactions().size());

        var txn = result.getTransactions().get(0);
        assertEquals("TXN-12345", txn.getReference());
        assertEquals("PayOS", txn.getGateway());
        assertEquals("PAID", txn.getStatus());
        assertEquals(new BigDecimal("250.50"), txn.getAmount());
        assertNotNull(txn.getOccurredAt());
    }

    @Test
    @DisplayName("Should get admin booking detail with all fields mapped correctly")
    void should_getAdminBookingDetail_withAllFieldsMappedCorrectly() {
        // Given
        LocalDateTime createdAt = LocalDate.of(2024, 11, 1).atStartOfDay();
        LocalDateTime modifiedAt = LocalDate.of(2024, 11, 15).atStartOfDay();

        Booking booking =
                Booking.builder()
                        .id(999L)
                        .course(course)
                        .bookingStatus(BookingStatus.APPROVED)
                        .registrationType(RegistrationType.REGULAR)
                        .groupType(GroupType.GROUP)
                        .totalAmount(new BigDecimal("1000.00"))
                        .bookingMembers(
                                new HashSet<>(
                                        List.of(
                                                BookingMember.builder()
                                                        .userId("owner123")
                                                        .role("OWNER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member1")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.APPROVED)
                                                        .build(),
                                                BookingMember.builder()
                                                        .userId("member2")
                                                        .role("MEMBER")
                                                        .status(BookingMemberStatus.WAITING)
                                                        .build())))
                        .transactions(
                                Set.of(
                                        Transaction.builder()
                                                .transactionId("TXN-001")
                                                .status(PaymentStatus.PAID)
                                                .amount(new BigDecimal("1000.00"))
                                                .paymentDate(LocalDateTime.now())
                                                .build()))
                        .createdAt(createdAt)
                        .modifiedAt(modifiedAt)
                        .createdBy("admin-user")
                        .modifiedBy("admin-user2")
                        .build();

        when(bookingRepository.findWithDetailsById(999L)).thenReturn(Optional.of(booking));

        // When
        var result = bookingService.getAdminBookingDetail(999L);

        // Then
        assertNotNull(result);

        // Verify booking wrapper
        assertNotNull(result.getBooking());
        assertEquals(999L, result.getBooking().getId());

        // Verify owner
        assertEquals("owner123", result.getBookingOwnerUserId());
        assertNotNull(result.getBookingOwner());
        assertEquals("owner123", result.getBookingOwner().getUserId());
        assertEquals("OWNER", result.getBookingOwner().getRole());
        assertEquals(BookingMemberStatus.APPROVED, result.getBookingOwner().getStatus());

        // Verify member counts
        assertEquals(3, result.getMemberCount());
        assertEquals(2, result.getApprovedMemberCount());
        assertEquals(1, result.getWaitingMemberCount());
        assertEquals(0, result.getRejectedMemberCount());
        assertTrue(result.getRejectedMembers().isEmpty());

        // Verify payment
        assertEquals(new BigDecimal("1000.00"), result.getTotalAmount());
        assertTrue(result.getIsPaid());
        assertEquals(1, result.getTransactions().size());

        // Verify audit fields
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals(modifiedAt, result.getUpdatedAt());
        assertEquals("admin-user", result.getCreatedBy());
        assertEquals("admin-user2", result.getLastModifiedBy());
    }

    @Test
    @DisplayName("Should get available trial sessions")
    void should_getAvailableTrialSessions() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSessionId());
    }

    @Test
    @DisplayName("Should throw AppException when class not found in getAvailableTrialSessions")
    void should_throwException_whenClassNotFound_inGetAvailableTrialSessions() {
        // Given
        Long classId = 999L;
        when(tutorClassRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.getAvailableTrialSessions(classId);
                        });

        assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
        verify(tutorClassRepository).findByIdAndIsDeletedFalse(999);
        verify(classSessionRepository, never())
                .findUpcomingSessionsByClassId(anyLong(), any(), any());
    }

    // ==================== Additional tests for getAvailableTrialSessions ====================

    @Test
    @DisplayName("Should get available trial sessions with no upcoming sessions")
    void should_getAvailableTrialSessions_withNoUpcomingSessions() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of()); // Empty list

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // No sessions
    }

    @Test
    @DisplayName("Should get available trial sessions with multiple sessions")
    void should_getAvailableTrialSessions_withMultipleSessions() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate nextWeek = LocalDate.now().plusDays(7);

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .topic("Introduction to Algebra")
                        .sessionDate(tomorrow)
                        .slotNumber(2)
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(2L)
                        .sessionNumber(2)
                        .topic("Linear Equations")
                        .sessionDate(nextWeek)
                        .slotNumber(5)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session1, session2));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first session
        assertEquals(1L, result.get(0).getSessionId());
        assertEquals(1, result.get(0).getSessionNumber());
        assertEquals("Introduction to Algebra", result.get(0).getTopic());
        assertEquals(tomorrow, result.get(0).getSessionDate());
        assertEquals(2, result.get(0).getSlotNumber());

        // Verify second session
        assertEquals(2L, result.get(1).getSessionId());
        assertEquals(2, result.get(1).getSessionNumber());
        assertEquals("Linear Equations", result.get(1).getTopic());
        assertEquals(nextWeek, result.get(1).getSessionDate());
        assertEquals(5, result.get(1).getSlotNumber());
    }

    @Test
    @DisplayName("Should get available trial sessions with invalid slot number returns TBD")
    void should_getAvailableTrialSessions_withInvalidSlotNumber() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .topic("Test Topic")
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(99) // Invalid slot number
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TBD", result.get(0).getTimeRange()); // Should be TBD for invalid slot
    }

    @Test
    @DisplayName("Should get available trial sessions with valid slot number and time range")
    void should_getAvailableTrialSessions_withValidSlotAndTimeRange() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .topic("Math Basics")
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(3) // Valid slot number (10:00-11:30)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("10:00-11:30", result.get(0).getTimeRange()); // Slot 3 time range
    }

    @Test
    @DisplayName("Should get available trial sessions with all different slot numbers")
    void should_getAvailableTrialSessions_withDifferentSlots() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1) // 07:00-08:30
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(2L)
                        .sessionNumber(2)
                        .sessionDate(LocalDate.now().plusDays(2))
                        .slotNumber(10) // 20:30-22:00
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session1, session2));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("07:00-08:30", result.get(0).getTimeRange()); // Slot 1
        assertEquals("20:30-22:00", result.get(1).getTimeRange()); // Slot 10
    }

    @Test
    @DisplayName("Should get available trial sessions with day of week mapping")
    void should_getAvailableTrialSessions_withDayOfWeekMapping() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        // Create a session for a specific day
        LocalDate monday = LocalDate.now().plusDays(1);
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .topic("Test")
                        .sessionDate(monday)
                        .slotNumber(3)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MONDAY", result.get(0).getDayOfWeek()); // Should map to day of week
        assertEquals(monday, result.get(0).getSessionDate());
    }

    @Test
    @DisplayName("Should get available trial sessions with limit of 2 sessions")
    void should_getAvailableTrialSessions_withLimitOf2() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session1 =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1)
                        .build();

        ClassSession session2 =
                ClassSession.builder()
                        .id(2L)
                        .sessionNumber(2)
                        .sessionDate(LocalDate.now().plusDays(2))
                        .slotNumber(2)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session1, session2));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // Max 2 sessions due to PageRequest.of(0, 2)

        // Verify PageRequest was created with page 0 and size 2
        verify(classSessionRepository)
                .findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), eq(PageRequest.of(0, 2)));
    }

    @Test
    @DisplayName("Should get available trial sessions with null topic")
    void should_getAvailableTrialSessions_withNullTopic() {
        // Given
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));

        ClassSession session =
                ClassSession.builder()
                        .id(1L)
                        .sessionNumber(1)
                        .topic(null) // Null topic
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(2)
                        .build();

        when(classSessionRepository.findUpcomingSessionsByClassId(
                        eq(10L), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(List.of(session));

        // When
        var result = bookingService.getAvailableTrialSessions(10L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getTopic()); // Topic should be null
    }

    @Test
    @DisplayName("Should book trial lesson successfully")
    void should_bookTrialLesson_successfully() {
        // Given
        MockHelper.mockSecurityContext("student");

        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        ClassSession session =
                ClassSession.builder()
                        .id(100L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(classSessionRepository.findByIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(session));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        i -> {
                            Booking b = i.getArgument(0);
                            b.setId(500L);
                            return b;
                        });

        // When
        BookingResponse response = bookingService.bookTrialLesson(request);

        // Then
        assertNotNull(response);
        assertEquals(RegistrationType.TRIAL, response.getRegistrationType());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
        verify(notificationService, times(1))
                .createAndSendNotification(eq("user-1"), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw AppException when trial already used in bookTrialLesson")
    void should_throwException_whenTrialAlreadyUsed_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.TRIAL_ALREADY_USED, exception.getErrorCode());
        verify(bookingRepository)
                .existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL);
        verify(tutorClassRepository, never()).findByIdAndIsDeletedFalse(anyInt());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when class not found in bookTrialLesson")
    void should_throwException_whenClassNotFound_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(999L);
        request.setSessionId(100L);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
        verify(tutorClassRepository).findByIdAndIsDeletedFalse(999);
        verify(classSessionRepository, never()).findByIdAndIsDeletedFalse(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when class is full in bookTrialLesson")
    void should_throwException_whenClassFull_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        TutorClass fullClass =
                TutorClass.builder()
                        .id(10L)
                        .course(course)
                        .title("Math Class 1")
                        .maxStudents(5)
                        .currentStudents(5) // Class is full
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(fullClass));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.CLASS_FULL, exception.getErrorCode());
        verify(tutorClassRepository).findByIdAndIsDeletedFalse(10);
        verify(classSessionRepository, never()).findByIdAndIsDeletedFalse(anyLong());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when session not found in bookTrialLesson")
    void should_throwException_whenSessionNotFound_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(999L);

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(classSessionRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.SESSION_NOT_FOUND, exception.getErrorCode());
        verify(classSessionRepository).findByIdAndIsDeletedFalse(999L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when session not in class in bookTrialLesson")
    void should_throwException_whenSessionNotInClass_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        TutorClass otherClass =
                TutorClass.builder()
                        .id(20L)
                        .course(course)
                        .title("Other Class")
                        .maxStudents(5)
                        .currentStudents(2)
                        .build();

        ClassSession session =
                ClassSession.builder()
                        .id(100L)
                        .tutorClass(otherClass) // Session belongs to different class
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(classSessionRepository.findByIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.SESSION_NOT_IN_CLASS, exception.getErrorCode());
        verify(classSessionRepository).findByIdAndIsDeletedFalse(100L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when session already passed in bookTrialLesson")
    void should_throwException_whenSessionAlreadyPassed_inBookTrialLesson() {
        // Given
        MockHelper.mockSecurityContext("student");
        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        ClassSession session =
                ClassSession.builder()
                        .id(100L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.now().minusDays(1)) // Session in the past
                        .slotNumber(1)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(classSessionRepository.findByIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            bookingService.bookTrialLesson(request);
                        });

        assertEquals(ErrorCode.SESSION_ALREADY_PASSED, exception.getErrorCode());
        verify(classSessionRepository).findByIdAndIsDeletedFalse(100L);
        verify(bookingRepository, never()).save(any());
    }

    // ==================== Additional tests for bookTrialLesson ====================

    @Test
    @DisplayName("Should book trial lesson when session is today")
    void should_bookTrialLesson_whenSessionIsToday() {
        // Given
        MockHelper.mockSecurityContext("student");

        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        ClassSession session =
                ClassSession.builder()
                        .id(100L)
                        .tutorClass(tutorClass)
                        .sessionDate(LocalDate.now()) // Today - should be allowed
                        .slotNumber(5)
                        .sessionNumber(3)
                        .topic("Algebra Basics")
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(tutorClass));
        when(classSessionRepository.findByIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(session));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        i -> {
                            Booking b = i.getArgument(0);
                            b.setId(500L);
                            return b;
                        });

        // When
        BookingResponse response = bookingService.bookTrialLesson(request);

        // Then
        assertNotNull(response);
        assertEquals(RegistrationType.TRIAL, response.getRegistrationType());
    }

    @Test
    @DisplayName("Should book trial lesson with null course name in notification")
    void should_bookTrialLesson_withNullCourseName() {
        // Given
        MockHelper.mockSecurityContext("student");

        TutorClass classWithoutCourse =
                TutorClass.builder()
                        .id(10L)
                        .course(null) // No course
                        .title("Math Class 1")
                        .maxStudents(5)
                        .currentStudents(2)
                        .build();

        BookTrialRequest request = new BookTrialRequest();
        request.setCourseId(1L);
        request.setClassId(10L);
        request.setSessionId(100L);

        ClassSession session =
                ClassSession.builder()
                        .id(100L)
                        .tutorClass(classWithoutCourse)
                        .sessionDate(LocalDate.now().plusDays(1))
                        .slotNumber(1)
                        .build();

        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(currentUser));
        when(bookingRepository.existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
                        "user-1", 1L, RegistrationType.TRIAL))
                .thenReturn(false);
        when(tutorClassRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(classWithoutCourse));
        when(classSessionRepository.findByIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(session));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(
                        i -> {
                            Booking b = i.getArgument(0);
                            b.setId(500L);
                            return b;
                        });

        // When
        BookingResponse response = bookingService.bookTrialLesson(request);

        // Then
        assertNotNull(response);

        // Verify notification was sent with empty course name
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, atLeastOnce())
                .createAndSendNotification(
                        eq("user-1"), messageCaptor.capture(), any(), any(), any());

        String message = messageCaptor.getValue();
        assertTrue(message.contains("Your trial lesson request for"));
    }
}
