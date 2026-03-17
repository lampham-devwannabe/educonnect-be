package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.tutor.request.AvailabilityUpdateRequest;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.enums.TeachingSlot;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.TutorAvailabilityService;
import com.sep.educonnect.util.MockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorAvailabilityService Unit Tests")
class TutorAvailabilityServiceTest {

    @Mock
    private TutorAvailabilityRepository tutorAvailabilityRepository;

    @Mock
    private TutorAvailabilityExceptionRepository tutorAvailabilityExceptionRepository;

    @Mock
    private TutorProfileRepository tutorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ScheduleChangeRepository scheduleChangeRepository;

    @InjectMocks
    private TutorAvailabilityService tutorAvailabilityService;

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should update tutor availability and reset slots")
    void should_updateAvailabilitySuccessfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(3))); // pre-existing

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2),
                3, List.of(4)
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertEquals(List.of(1, 2), updated.getSlotsByDay(1));
        assertEquals(List.of(4), updated.getSlotsByDay(3));
        assertTrue(updated.isWorkOnDay(1));
        assertTrue(updated.isWorkOnDay(3));
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should create availability if not existing for tutor")
    void should_createNewAvailabilityWhenNotExisting() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        Map<Integer, List<Integer>> schedule = Map.of(1, List.of(1));
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.empty());
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertEquals(tutor, updated.getUser());
        assertEquals(List.of(1), updated.getSlotsByDay(1));
        verify(tutorAvailabilityRepository).save(any(TutorAvailability.class));
    }

    @Test
    @DisplayName("Should throw when user not found during availability update")
    void should_throwWhenUserNotFound_updateAvailability() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(Map.of())
                .build();

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> tutorAvailabilityService.updateAvailability(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should build weekly schedule response with available slots")
    void should_getWeeklyScheduleWithAvailableSlots() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday slot 1

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        LocalDate startDate = LocalDate.of(2025, 1, 6); // Monday

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals("tutor-1", response.getUserId());
        assertEquals(startDate, response.getStartDate());
        assertEquals(startDate.plusDays(6), response.getEndDate());
        assertEquals(1, response.getDays().size());

        WeeklyAvailabilityResponse.DaySchedule daySchedule = response.getDays().get(0);
        assertEquals(1, daySchedule.getDayOfWeek());
        assertTrue(daySchedule.getIsWorkDay());
        assertEquals(1, daySchedule.getSlots().size());

        WeeklyAvailabilityResponse.SlotInfo slotInfo = daySchedule.getSlots().get(0);
        assertEquals(TeachingSlot.SLOT_1.getSlotName(), slotInfo.getSlotName());
        assertEquals("AVAILABLE", slotInfo.getSlotStatus());
        assertTrue(slotInfo.getIsAvailable());
        assertFalse(slotInfo.getIsBooked());

        verify(classSessionRepository).findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class));
        verify(scheduleChangeRepository).findByCreatedByAndStatus("tutor", "APPROVED");
    }

    @Test
    @DisplayName("Should get weekly schedule with booked sessions")
    void should_getWeeklyScheduleWithBookedSessions() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday slot 1

        TutorClass tutorClass = TutorClass.builder()
                .id(1L)
                .tutor(tutor)
                .build();

        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6)) // Monday
                .slotNumber(TeachingSlot.SLOT_1.getSlotNumber())
                .sessionNumber(1)
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6); // Monday

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getDays().size());
        WeeklyAvailabilityResponse.DaySchedule daySchedule = response.getDays().get(0);
        assertEquals(1, daySchedule.getSlots().size());
        WeeklyAvailabilityResponse.SlotInfo slotInfo = daySchedule.getSlots().get(0);
        assertEquals("BOOKED", slotInfo.getSlotStatus());
        assertTrue(slotInfo.getIsBooked());
        assertEquals(200L, slotInfo.getSessionId());
    }

    @Test
    @DisplayName("Should get weekly schedule with exception")
    void should_getWeeklyScheduleWithException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday slot 1

        TutorClass tutorClass = TutorClass.builder()
                .id(1L)
                .tutor(tutor)
                .build();

        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6)) // Monday
                .slotNumber(TeachingSlot.SLOT_1.getSlotNumber())
                .sessionNumber(1)
                .build();

        TutorAvailabilityException exception = TutorAvailabilityException.builder()
                .id(300L)
                .session(session)
                .status(com.sep.educonnect.enums.ExceptionStatus.APPROVED)
                .reason("Sick")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6); // Monday

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of(exception));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        WeeklyAvailabilityResponse.SlotInfo slotInfo = response.getDays().get(0).getSlots().get(0);
        assertEquals("EXCEPTION", slotInfo.getSlotStatus());
        assertTrue(slotInfo.getHasException());
        assertEquals("Sick", slotInfo.getExceptionReason());
    }

    @Test
    @DisplayName("Should get weekly schedule with schedule change")
    void should_getWeeklyScheduleWithScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday slot 1

        TutorClass tutorClass = TutorClass.builder()
                .id(1L)
                .tutor(tutor)
                .build();

        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6)) // Monday
                .slotNumber(TeachingSlot.SLOT_1.getSlotNumber())
                .sessionNumber(1)
                .build();

        ScheduleChange scheduleChange = ScheduleChange.builder()
                .id(300L)
                .session(session)
                .oldDate(LocalDate.of(2025, 1, 6)) // Monday
                .newDate(LocalDate.of(2025, 1, 8)) // Wednesday
                .newSLot(TeachingSlot.SLOT_2.getSlotNumber())
                .content("Need to reschedule")
                .status("APPROVED")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6); // Monday

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        // Should have Monday (MOVED_FROM) and Wednesday (MOVED_TO)
        assertEquals(2, response.getDays().size());
    }

    @Test
    @DisplayName("Should throw when tutor profile not found")
    void should_throwWhenTutorProfileNotFound_getWeeklySchedule() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.empty());

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> tutorAvailabilityService.getWeeklySchedule(startDate));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when availability not set")
    void should_throwWhenAvailabilityNotSet() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.empty());

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> tutorAvailabilityService.getWeeklySchedule(startDate));
        assertEquals(ErrorCode.TUTOR_AVAILABILITY_NOT_SET, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should update availability and reset all days")
    void should_updateAvailabilityAndResetAllDays() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setIsWorkOnMonday(true);
        availability.setIsWorkOnTuesday(true);
        availability.setIsWorkOnWednesday(true);

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2) // Monday slots 1, 2
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(1));
        assertFalse(updated.isWorkOnDay(2)); // Tuesday
        assertFalse(updated.isWorkOnDay(3)); // Wednesday
        assertEquals(List.of(1, 2), updated.getSlotsByDay(1));
    }

    // ==================== COMPREHENSIVE getWeeklySchedule TESTS ====================

    @Test
    @DisplayName("Should get weekly schedule with unavailable slots")
    void should_getWeeklyScheduleWithUnavailableSlots() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2))); // Monday slots 1,2 available

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();
        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6))
                .slotNumber(3) // Slot 3 is NOT in availability, but has session
                .sessionNumber(1)
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getDays().size());
        WeeklyAvailabilityResponse.DaySchedule daySchedule = response.getDays().get(0);
        assertEquals(3, daySchedule.getSlots().size()); // 2 available + 1 booked

        // Check slot 3 is BOOKED (even though not in availability)
        WeeklyAvailabilityResponse.SlotInfo slot3 = daySchedule.getSlots().stream()
                .filter(s -> s.getSlotNumber() == 3)
                .findFirst()
                .orElseThrow();
        assertEquals("BOOKED", slot3.getSlotStatus());
        assertTrue(slot3.getIsBooked());
    }

    @Test
    @DisplayName("Should get weekly schedule with multiple days and mixed statuses")
    void should_getWeeklyScheduleWithMultipleDaysAndMixedStatuses() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2, 3))); // Monday
        availability.setSlotsByDay(2, new ArrayList<>(List.of(1, 2))); // Tuesday
        availability.setSlotsByDay(3, new ArrayList<>(List.of(1))); // Wednesday

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();

        // Monday session - BOOKED
        ClassSession mondaySession = ClassSession.builder()
                .id(201L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6))
                .slotNumber(1)
                .sessionNumber(1)
                .build();

        // Tuesday session with exception - EXCEPTION
        ClassSession tuesdaySession = ClassSession.builder()
                .id(202L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 7))
                .slotNumber(1)
                .sessionNumber(2)
                .build();

        TutorAvailabilityException exception = TutorAvailabilityException.builder()
                .id(300L)
                .session(tuesdaySession)
                .status(com.sep.educonnect.enums.ExceptionStatus.APPROVED)
                .reason("Medical appointment")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(mondaySession, tuesdaySession));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of(exception));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getDays().size()); // Monday, Tuesday, Wednesday

        // Check Monday - should have BOOKED and AVAILABLE slots
        WeeklyAvailabilityResponse.DaySchedule monday = response.getDays().get(0);
        assertEquals(1, monday.getDayOfWeek());
        assertTrue(monday.getSlots().stream().anyMatch(s -> s.getSlotStatus().equals("BOOKED")));
        assertTrue(monday.getSlots().stream().anyMatch(s -> s.getSlotStatus().equals("AVAILABLE")));

        // Check Tuesday - should have EXCEPTION
        WeeklyAvailabilityResponse.DaySchedule tuesday = response.getDays().get(1);
        assertEquals(2, tuesday.getDayOfWeek());
        WeeklyAvailabilityResponse.SlotInfo exceptionSlot = tuesday.getSlots().stream()
                .filter(s -> s.getSlotNumber() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals("EXCEPTION", exceptionSlot.getSlotStatus());
        assertEquals("Medical appointment", exceptionSlot.getExceptionReason());

        // Check Wednesday - should have AVAILABLE slot
        WeeklyAvailabilityResponse.DaySchedule wednesday = response.getDays().get(2);
        assertEquals(3, wednesday.getDayOfWeek());
        assertEquals(1, wednesday.getSlots().size());
        assertEquals("AVAILABLE", wednesday.getSlots().get(0).getSlotStatus());
    }

    @Test
    @DisplayName("Should get weekly schedule with MOVED_FROM and MOVED_TO statuses")
    void should_getWeeklyScheduleWithMovedFromAndMovedTo() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday slot 1
        availability.setSlotsByDay(3, new ArrayList<>(List.of(2))); // Wednesday slot 2

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();
        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6)) // Monday
                .slotNumber(1)
                .sessionNumber(1)
                .build();

        ScheduleChange scheduleChange = ScheduleChange.builder()
                .id(300L)
                .session(session)
                .oldDate(LocalDate.of(2025, 1, 6)) // Monday
                .newDate(LocalDate.of(2025, 1, 8)) // Wednesday
                .newSLot(2)
                .content("Reschedule to Wednesday")
                .status("APPROVED")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getDays().size());

        // Check Monday has MOVED_FROM
        WeeklyAvailabilityResponse.DaySchedule monday = response.getDays().stream()
                .filter(d -> d.getDayOfWeek() == 1)
                .findFirst()
                .orElseThrow();
        WeeklyAvailabilityResponse.SlotInfo movedFromSlot = monday.getSlots().get(0);
        assertEquals("MOVED_FROM", movedFromSlot.getSlotStatus());
        assertNotNull(movedFromSlot.getScheduleChangeInfo());
        assertEquals(300L, movedFromSlot.getScheduleChangeInfo().getScheduleChangeId());

        // Check Wednesday has MOVED_TO
        WeeklyAvailabilityResponse.DaySchedule wednesday = response.getDays().stream()
                .filter(d -> d.getDayOfWeek() == 3)
                .findFirst()
                .orElseThrow();
        WeeklyAvailabilityResponse.SlotInfo movedToSlot = wednesday.getSlots().stream()
                .filter(s -> s.getSlotNumber() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals("MOVED_TO", movedToSlot.getSlotStatus());
        assertNotNull(movedToSlot.getScheduleChangeInfo());
        assertEquals(300L, movedToSlot.getScheduleChangeInfo().getScheduleChangeId());
    }

    @Test
    @DisplayName("Should get empty weekly schedule when no availability set")
    void should_getWeeklyScheduleWithNoAvailability() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        // No slots set - all days unavailable

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getDays().size()); // No days with available/booked slots
    }

    @Test
    @DisplayName("Should get full week schedule with all days")
    void should_getFullWeekSchedule() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        // Set availability for all 7 days
        availability.setSlotsByDay(0, new ArrayList<>(List.of(1))); // Sunday
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday
        availability.setSlotsByDay(2, new ArrayList<>(List.of(1))); // Tuesday
        availability.setSlotsByDay(3, new ArrayList<>(List.of(1))); // Wednesday
        availability.setSlotsByDay(4, new ArrayList<>(List.of(1))); // Thursday
        availability.setSlotsByDay(5, new ArrayList<>(List.of(1))); // Friday
        availability.setSlotsByDay(6, new ArrayList<>(List.of(1))); // Saturday

        LocalDate startDate = LocalDate.of(2025, 1, 6); // Monday

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(7, response.getDays().size());
        // All days should have at least 1 slot
        response.getDays().forEach(day -> {
            assertTrue(day.getSlots().size() >= 1);
            assertTrue(day.getIsWorkDay());
        });
    }

    @Test
    @DisplayName("Should get weekly schedule with multiple sessions on same day")
    void should_getWeeklyScheduleWithMultipleSessionsSameDay() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2, 3, 4))); // Monday 4 slots

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();

        ClassSession session1 = ClassSession.builder()
                .id(201L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6))
                .slotNumber(1)
                .sessionNumber(1)
                .build();

        ClassSession session2 = ClassSession.builder()
                .id(202L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6))
                .slotNumber(2)
                .sessionNumber(2)
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session1, session2));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getDays().size());
        WeeklyAvailabilityResponse.DaySchedule monday = response.getDays().get(0);
        assertEquals(4, monday.getSlots().size());

        // 2 BOOKED, 2 AVAILABLE
        long bookedCount = monday.getSlots().stream()
                .filter(s -> s.getSlotStatus().equals("BOOKED"))
                .count();
        long availableCount = monday.getSlots().stream()
                .filter(s -> s.getSlotStatus().equals("AVAILABLE"))
                .count();

        assertEquals(2, bookedCount);
        assertEquals(2, availableCount);
    }

    @Test
    @DisplayName("Should get weekly schedule with pending exception not shown")
    void should_getWeeklyScheduleWithPendingExceptionNotShown() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1)));

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();
        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6))
                .slotNumber(1)
                .sessionNumber(1)
                .build();

        // PENDING exception should not affect slot status
        TutorAvailabilityException pendingException = TutorAvailabilityException.builder()
                .id(300L)
                .session(session)
                .status(com.sep.educonnect.enums.ExceptionStatus.PENDING)
                .reason("Sick")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of(pendingException));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        WeeklyAvailabilityResponse.SlotInfo slotInfo = response.getDays().get(0).getSlots().get(0);
        // Should be BOOKED, not EXCEPTION (because exception is PENDING)
        assertEquals("BOOKED", slotInfo.getSlotStatus());
        assertFalse(slotInfo.getHasException());
    }

    @Test
    @DisplayName("Should get weekly schedule with different start dates")
    void should_getWeeklyScheduleWithDifferentStartDates() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1))); // Monday

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        LocalDate startDate = LocalDate.of(2025, 2, 10); // Different month

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals(startDate, response.getStartDate());
        assertEquals(startDate.plusDays(6), response.getEndDate());
        // Should still have Monday slot
        assertTrue(response.getDays().size() > 0);
    }

    @Test
    @DisplayName("Should get weekly schedule with session outside week range but affected by schedule change")
    void should_getWeeklyScheduleWithSessionOutsideRangeButAffectedByScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").firstName("Alice").lastName("Tran").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(3, new ArrayList<>(List.of(1))); // Wednesday

        TutorClass tutorClass = TutorClass.builder().id(1L).tutor(tutor).build();
        // Session originally on Monday (outside visible range if we start on Wednesday)
        ClassSession session = ClassSession.builder()
                .id(200L)
                .tutorClass(tutorClass)
                .sessionDate(LocalDate.of(2025, 1, 6)) // Monday
                .slotNumber(1)
                .sessionNumber(1)
                .build();

        // Moved to Wednesday (in range)
        ScheduleChange scheduleChange = ScheduleChange.builder()
                .id(300L)
                .session(session)
                .oldDate(LocalDate.of(2025, 1, 6)) // Monday (outside range)
                .newDate(LocalDate.of(2025, 1, 8)) // Wednesday (in range)
                .newSLot(1)
                .content("Reschedule")
                .status("APPROVED")
                .build();

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(session));
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of(scheduleChange));
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        // Should have both Monday (MOVED_FROM) and Wednesday (MOVED_TO)
        assertTrue(response.getDays().size() >= 1);
        // Wednesday should have MOVED_TO slot
        Optional<WeeklyAvailabilityResponse.DaySchedule> wednesday = response.getDays().stream()
                .filter(d -> d.getDayOfWeek() == 3)
                .findFirst();
        assertTrue(wednesday.isPresent());
        assertTrue(wednesday.get().getSlots().stream()
                .anyMatch(s -> s.getSlotStatus().equals("MOVED_TO")));
    }

    @Test
    @DisplayName("Should verify response contains correct metadata")
    void should_verifyWeeklyScheduleResponseMetadata() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder()
                .userId("tutor-1")
                .username("tutor")
                .firstName("Alice")
                .lastName("Tran")
                .build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1)));

        LocalDate startDate = LocalDate.of(2025, 1, 6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED)).thenReturn(Optional.of(profile));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(classSessionRepository.findByTutorAndDateRange(eq("tutor-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(scheduleChangeRepository.findByCreatedByAndStatus(eq("tutor"), eq("APPROVED")))
                .thenReturn(List.of());
        when(tutorAvailabilityExceptionRepository.findByTutorProfileId(100L))
                .thenReturn(List.of());

        // When
        WeeklyAvailabilityResponse response = tutorAvailabilityService.getWeeklySchedule(startDate);

        // Then
        assertNotNull(response);
        assertEquals("tutor-1", response.getUserId());
        assertEquals("Alice Tran", response.getTutorName());
        assertEquals(startDate, response.getStartDate());
        assertEquals(startDate.plusDays(6), response.getEndDate());
        assertNotNull(response.getDays());
        verify(userRepository).findByUsername("tutor");
        verify(tutorProfileRepository).findByUserUserIdAndSubmissionStatus("tutor-1", ProfileStatus.APPROVED);
        verify(tutorAvailabilityRepository).findByUserUserId("tutor-1");
    }

    // ==================== COMPREHENSIVE updateAvailability TESTS ====================

    @Test
    @DisplayName("Should update availability with empty schedule")
    void should_updateAvailabilityWithEmptySchedule() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setIsWorkOnMonday(true);
        availability.setIsWorkOnTuesday(true);

        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(new HashMap<>())
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertFalse(updated.isWorkOnDay(1));
        assertFalse(updated.isWorkOnDay(2));
        assertFalse(updated.isWorkOnDay(3));
        assertFalse(updated.isWorkOnDay(4));
        assertFalse(updated.isWorkOnDay(5));
        assertFalse(updated.isWorkOnDay(6));
        assertFalse(updated.isWorkOnDay(0)); // Sunday is day 0
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability with all days of week")
    void should_updateAvailabilityWithAllDays() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2),
                2, List.of(3),
                3, List.of(1, 4),
                4, List.of(2, 5),
                5, List.of(1, 3, 6),
                6, List.of(2, 4),
                0, List.of(1) // Sunday is day 0
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(1));
        assertTrue(updated.isWorkOnDay(2));
        assertTrue(updated.isWorkOnDay(3));
        assertTrue(updated.isWorkOnDay(4));
        assertTrue(updated.isWorkOnDay(5));
        assertTrue(updated.isWorkOnDay(6));
        assertTrue(updated.isWorkOnDay(0)); // Sunday is day 0
        assertEquals(List.of(1, 2), updated.getSlotsByDay(1));
        assertEquals(List.of(3), updated.getSlotsByDay(2));
        assertEquals(List.of(1, 4), updated.getSlotsByDay(3));
        assertEquals(List.of(2, 5), updated.getSlotsByDay(4));
        assertEquals(List.of(1, 3, 6), updated.getSlotsByDay(5));
        assertEquals(List.of(2, 4), updated.getSlotsByDay(6));
        assertEquals(List.of(1), updated.getSlotsByDay(0)); // Sunday is day 0
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability with single slot")
    void should_updateAvailabilityWithSingleSlot() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(3, List.of(5)); // Wednesday slot 5
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(3));
        assertFalse(updated.isWorkOnDay(1));
        assertFalse(updated.isWorkOnDay(2));
        assertEquals(List.of(5), updated.getSlotsByDay(3));
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability and preserve user reference")
    void should_updateAvailabilityAndPreserveUser() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(2, List.of(1, 2, 3));
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertEquals(tutor, updated.getUser());
        assertEquals("tutor-1", updated.getUser().getUserId());
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability replacing previous schedule completely")
    void should_updateAvailabilityReplacingPreviousSchedule() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);
        availability.setSlotsByDay(1, new ArrayList<>(List.of(1, 2, 3)));
        availability.setSlotsByDay(2, new ArrayList<>(List.of(4, 5, 6)));
        availability.setSlotsByDay(3, new ArrayList<>(List.of(1)));

        Map<Integer, List<Integer>> schedule = Map.of(
                4, List.of(2),
                5, List.of(3, 4)
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertFalse(updated.isWorkOnDay(1)); // Previous schedule cleared
        assertFalse(updated.isWorkOnDay(2));
        assertFalse(updated.isWorkOnDay(3));
        assertTrue(updated.isWorkOnDay(4)); // New schedule applied
        assertTrue(updated.isWorkOnDay(5));
        assertEquals(List.of(2), updated.getSlotsByDay(4));
        assertEquals(List.of(3, 4), updated.getSlotsByDay(5));
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability with maximum slots per day")
    void should_updateAvailabilityWithMaximumSlots() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2, 3, 4, 5, 6) // All 6 slots
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(1));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), updated.getSlotsByDay(1));
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should create new availability when user has no availability")
    void should_createNewAvailabilityWhenNoneExists() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2),
                3, List.of(3, 4)
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.empty());
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertNotNull(updated);
        assertEquals(tutor, updated.getUser());
        assertTrue(updated.isWorkOnDay(1));
        assertTrue(updated.isWorkOnDay(3));
        assertFalse(updated.isWorkOnDay(2));
        assertEquals(List.of(1, 2), updated.getSlotsByDay(1));
        assertEquals(List.of(3, 4), updated.getSlotsByDay(3));
        verify(tutorAvailabilityRepository).save(any(TutorAvailability.class));
    }

    @Test
    @DisplayName("Should throw when user not found")
    void should_throwWhenUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(Map.of(1, List.of(1)))
                .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> tutorAvailabilityService.updateAvailability(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorAvailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update availability with weekend schedule")
    void should_updateAvailabilityWithWeekendSchedule() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(
                6, List.of(1, 2, 3), // Saturday
                0, List.of(4, 5, 6)  // Sunday is day 0
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(6));
        assertTrue(updated.isWorkOnDay(0)); // Sunday is day 0
        assertFalse(updated.isWorkOnDay(1));
        assertFalse(updated.isWorkOnDay(2));
        assertEquals(List.of(1, 2, 3), updated.getSlotsByDay(6));
        assertEquals(List.of(4, 5, 6), updated.getSlotsByDay(0)); // Sunday is day 0
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability and verify repository save is called once")
    void should_verifyRepositorySaveCalledOnce() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(1, List.of(1));
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tutorAvailabilityService.updateAvailability(request);

        // Then
        verify(tutorAvailabilityRepository, times(1)).save(availability);
        verify(userRepository, times(1)).findByUsername("tutor");
        verify(tutorAvailabilityRepository, times(1)).findByUserUserId("tutor-1");
    }

    @Test
    @DisplayName("Should update availability with empty slot list for some days")
    void should_updateAvailabilityWithEmptySlotList() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = new HashMap<>();
        schedule.put(1, List.of(1, 2));
        schedule.put(2, new ArrayList<>()); // Empty list
        schedule.put(3, List.of(3));

        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(1));
        assertFalse(updated.isWorkOnDay(2)); // Empty list means no work
        assertTrue(updated.isWorkOnDay(3));
        assertEquals(List.of(1, 2), updated.getSlotsByDay(1));
        assertEquals(List.of(3), updated.getSlotsByDay(3));
        verify(tutorAvailabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Should update availability multiple times consecutively")
    void should_updateAvailabilityMultipleTimes() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First update
        Map<Integer, List<Integer>> schedule1 = Map.of(1, List.of(1, 2));
        AvailabilityUpdateRequest request1 = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule1)
                .build();
        TutorAvailability updated1 = tutorAvailabilityService.updateAvailability(request1);
        assertTrue(updated1.isWorkOnDay(1));

        // Second update
        Map<Integer, List<Integer>> schedule2 = Map.of(2, List.of(3, 4));
        AvailabilityUpdateRequest request2 = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule2)
                .build();
        TutorAvailability updated2 = tutorAvailabilityService.updateAvailability(request2);

        // Then
        assertFalse(updated2.isWorkOnDay(1)); // Previous schedule cleared
        assertTrue(updated2.isWorkOnDay(2));
        assertEquals(List.of(3, 4), updated2.getSlotsByDay(2));
        verify(tutorAvailabilityRepository, times(2)).save(availability);
    }

    @Test
    @DisplayName("Should update availability with duplicate slot numbers in same day")
    void should_updateAvailabilityWithDuplicateSlots() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorAvailability availability = new TutorAvailability();
        availability.setUser(tutor);

        Map<Integer, List<Integer>> schedule = Map.of(
                1, List.of(1, 2, 2, 3, 1) // Duplicate slots
        );
        AvailabilityUpdateRequest request = AvailabilityUpdateRequest.builder()
                .weekSchedule(schedule)
                .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorAvailabilityRepository.findByUserUserId("tutor-1")).thenReturn(Optional.of(availability));
        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorAvailability updated = tutorAvailabilityService.updateAvailability(request);

        // Then
        assertTrue(updated.isWorkOnDay(1));
        // The setSlotsByDay method should handle duplicates (behavior depends on implementation)
        assertNotNull(updated.getSlotsByDay(1));
        verify(tutorAvailabilityRepository).save(availability);
    }
}
