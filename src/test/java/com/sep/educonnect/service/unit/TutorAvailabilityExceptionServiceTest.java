package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.exception.request.ApproveExceptionRequest;
import com.sep.educonnect.dto.exception.request.BatchCreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.CreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.UpdateExceptionRequest;
import com.sep.educonnect.dto.exception.response.ExceptionListResponse;
import com.sep.educonnect.dto.exception.response.ExceptionResponse;
import com.sep.educonnect.dto.tutor.request.CreateScheduleChangeRequest;
import com.sep.educonnect.dto.tutor.response.ScheduleChangeResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.ExceptionStatus;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.enums.TeachingSlot;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExceptionMapper;
import com.sep.educonnect.mapper.ScheduleChangeMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.NotificationService;
import com.sep.educonnect.service.TutorAvailabilityExceptionService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorAvailabilityExceptionService Unit Tests")
class TutorAvailabilityExceptionServiceTest {

    @Mock private TutorAvailabilityExceptionRepository exceptionRepository;

    @Mock private TutorProfileRepository tutorProfileRepository;

    @Mock private ClassSessionRepository classSessionRepository;

    @Mock private UserRepository userRepository;

    @Mock private ExceptionMapper exceptionMapper;

    @Mock private ScheduleChangeRepository scheduleChangeRepository;

    @Mock private ScheduleChangeMapper scheduleChangeMapper;

    @Mock private NotificationService notificationService;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @InjectMocks private TutorAvailabilityExceptionService tutorAvailabilityExceptionService;

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should create tutor exception successfully")
    void should_createExceptionSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        TutorAvailabilityException savedException =
                TutorAvailabilityException.builder()
                        .id(300L)
                        .tutorProfile(profile)
                        .session(session)
                        .status(ExceptionStatus.PENDING)
                        .reason("Need time off")
                        .build();

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.countExceptionsSince(eq(100L), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(exceptionRepository.save(any(TutorAvailabilityException.class)))
                .thenReturn(savedException);
        ExceptionResponse mappedResponse =
                ExceptionResponse.builder().id(300L).status(ExceptionStatus.PENDING).build();
        when(exceptionMapper.toResponse(savedException)).thenReturn(mappedResponse);

        // When
        ExceptionResponse response = tutorAvailabilityExceptionService.createException(request);

        // Then
        assertEquals(300L, response.getId());
        verify(exceptionRepository).save(any(TutorAvailabilityException.class));
    }

    @Test
    @DisplayName("Should throw unauthorized when session not owned by tutor")
    void should_throwUnauthorizedWhenSessionNotOwned() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        User anotherTutor = User.builder().userId("tutor-2").username("other").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(anotherTutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createException(request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should cancel exception when pending")
    void should_cancelExceptionSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.PENDING)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any(TutorAvailabilityException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tutorAvailabilityExceptionService.cancelException(500L);

        // Then
        assertEquals(ExceptionStatus.CANCELLED, exception.getStatus());
        verify(exceptionRepository).save(exception);
    }

    @Test
    @DisplayName("Should approve exception and mark as approved")
    void should_approveExceptionSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        ClassSession session = new ClassSession();
        session.setId(200L);
        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTitle("Math Class");
        session.setTutorClass(tutorClass);

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.PENDING)
                        .tutorProfile(
                                TutorProfile.builder()
                                        .user(User.builder().userId("tutor-1").build())
                                        .build())
                        .session(session)
                        .isApproved(false)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        ExceptionResponse mapped =
                ExceptionResponse.builder().id(600L).status(ExceptionStatus.APPROVED).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(any(TutorAvailabilityException.class))).thenReturn(exception);
        when(exceptionMapper.toResponse(exception)).thenReturn(mapped);

        // When
        ExceptionResponse response = tutorAvailabilityExceptionService.approveException(request);

        // Then
        assertEquals(ExceptionStatus.APPROVED, exception.getStatus());
        assertTrue(exception.getIsApproved());
        assertEquals(600L, response.getId());
        verify(exceptionRepository).save(exception);
    }

    @Test
    @DisplayName("Should aggregate exceptions for current tutor")
    void should_getMyExceptionsWithCounts() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException pendingException =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException approvedException =
                TutorAvailabilityException.builder()
                        .id(2L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        List<TutorAvailabilityException> exceptions = List.of(pendingException, approvedException);

        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build(),
                        ExceptionResponse.builder()
                                .id(2L)
                                .status(ExceptionStatus.APPROVED)
                                .build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileId(100L)).thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response = tutorAvailabilityExceptionService.getMyExceptions(null);

        // Then
        assertEquals(2L, response.getTotalCount());
        assertEquals(1L, response.getPendingCount());
        assertEquals(1L, response.getApprovedCount());
        assertEquals(0L, response.getRejectedCount());
        assertEquals(mappedResponses, response.getExceptions());
    }

    @Test
    @DisplayName("Should throw when session is in the past")
    void should_throwWhenSessionInPast() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().minusDays(1)); // Past date
        session.setStartTime(LocalDateTime.now().minusDays(1));

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createException(request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PAST_SESSION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when exception request is too late")
    void should_throwWhenExceptionTooLate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(1));
        session.setStartTime(LocalDateTime.now().plusHours(12)); // Less than 24 hours

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createException(request));
        assertEquals(ErrorCode.EXCEPTION_TOO_LATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when exception already exists")
    void should_throwWhenExceptionAlreadyExists() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(100L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.APPROVED)
                        .build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.existsBySessionIdAndTutorProfileIdAndStatusNot(
                        200L, 100L, ExceptionStatus.CANCELLED))
                .thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when exception limit exceeded")
    void should_throwWhenExceptionLimitExceeded() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        CreateExceptionRequest request =
                CreateExceptionRequest.builder().sessionId(200L).reason("Need time off").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.countExceptionsSince(eq(100L), any(LocalDateTime.class)))
                .thenReturn(5L);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createException(request));
        assertEquals(ErrorCode.EXCEPTION_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should create batch exceptions successfully")
    void should_createBatchExceptionsSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session1 = new ClassSession();
        session1.setId(200L);
        session1.setTutorClass(tutorClass);
        session1.setSessionDate(LocalDate.now().plusDays(5));
        session1.setStartTime(LocalDateTime.now().plusDays(5));

        ClassSession session2 = new ClassSession();
        session2.setId(201L);
        session2.setTutorClass(tutorClass);
        session2.setSessionDate(LocalDate.now().plusDays(6));
        session2.setStartTime(LocalDateTime.now().plusDays(6));

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L, 201L))
                        .reason("Need time off")
                        .build();

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder()
                        .id(300L)
                        .tutorProfile(profile)
                        .session(session1)
                        .status(ExceptionStatus.PENDING)
                        .build();

        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder()
                        .id(301L)
                        .tutorProfile(profile)
                        .session(session2)
                        .status(ExceptionStatus.PENDING)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session1));
        when(classSessionRepository.findById(201L)).thenReturn(Optional.of(session2));
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(200L, 100L)).thenReturn(false);
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(201L, 100L)).thenReturn(false);
        when(exceptionRepository.saveAll(anyList())).thenReturn(List.of(exception1, exception2));

        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder().id(300L).build(),
                        ExceptionResponse.builder().id(301L).build());
        when(exceptionMapper.toResponseList(anyList())).thenReturn(mappedResponses);

        // When
        List<ExceptionResponse> response =
                tutorAvailabilityExceptionService.createBatchExceptions(request);

        // Then
        assertEquals(2, response.size());
        verify(exceptionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw when no valid sessions in batch")
    void should_throwWhenNoValidSessionsInBatch() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(999L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in createBatchExceptions")
    void should_throwUserNotExistedWhenUserNotFoundInCreateBatchExceptions() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
    }

    @Test
    @DisplayName(
            "Should throw TUTOR_PROFILE_NOT_EXISTED when tutor profile not found in createBatchExceptions")
    void should_throwTutorProfileNotExistedWhenProfileNotFoundInCreateBatchExceptions() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should skip sessions that do not belong to tutor in batch creation")
    void should_skipSessionsThatDoNotBelongToTutorInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        User anotherTutor = User.builder().userId("tutor-2").username("another").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(anotherTutor); // Session belongs to different tutor

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip sessions that already have exceptions in batch creation")
    void should_skipSessionsThatAlreadyHaveExceptionsInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalDateTime.now().plusDays(5));

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(200L, 100L)).thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip sessions in the past in batch creation")
    void should_skipSessionsInThePastInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now().minusDays(1)); // Past date
        session.setStartTime(LocalDateTime.now().minusDays(1));

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(200L, 100L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip sessions too close to start time in batch creation")
    void should_skipSessionsTooCloseToStartTimeInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);
        session.setSessionDate(LocalDate.now());
        session.setStartTime(LocalDateTime.now().plusHours(1)); // Less than 24 hours

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(200L, 100L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should create exceptions only for valid sessions and skip invalid ones in batch")
    void should_createExceptionsOnlyForValidSessionsAndSkipInvalidOnesInBatch() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        User anotherTutor = User.builder().userId("tutor-2").username("another").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);

        TutorClass anotherClass = new TutorClass();
        anotherClass.setId(2L);
        anotherClass.setTutor(anotherTutor);

        // Valid session
        ClassSession validSession = new ClassSession();
        validSession.setId(200L);
        validSession.setTutorClass(tutorClass);
        validSession.setSessionDate(LocalDate.now().plusDays(5));
        validSession.setStartTime(LocalDateTime.now().plusDays(5));

        // Invalid session - different tutor
        ClassSession invalidSession1 = new ClassSession();
        invalidSession1.setId(201L);
        invalidSession1.setTutorClass(anotherClass);
        invalidSession1.setSessionDate(LocalDate.now().plusDays(5));
        invalidSession1.setStartTime(LocalDateTime.now().plusDays(5));

        // Invalid session - in the past
        ClassSession invalidSession2 = new ClassSession();
        invalidSession2.setId(202L);
        invalidSession2.setTutorClass(tutorClass);
        invalidSession2.setSessionDate(LocalDate.now().minusDays(1));
        invalidSession2.setStartTime(LocalDateTime.now().minusDays(1));

        // Another valid session
        ClassSession validSession2 = new ClassSession();
        validSession2.setId(203L);
        validSession2.setTutorClass(tutorClass);
        validSession2.setSessionDate(LocalDate.now().plusDays(6));
        validSession2.setStartTime(LocalDateTime.now().plusDays(6));

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L, 201L, 202L, 203L))
                        .reason("Need time off")
                        .build();

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder()
                        .id(300L)
                        .tutorProfile(profile)
                        .session(validSession)
                        .status(ExceptionStatus.PENDING)
                        .build();

        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder()
                        .id(301L)
                        .tutorProfile(profile)
                        .session(validSession2)
                        .status(ExceptionStatus.PENDING)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(validSession));
        when(classSessionRepository.findById(201L)).thenReturn(Optional.of(invalidSession1));
        when(classSessionRepository.findById(202L)).thenReturn(Optional.of(invalidSession2));
        when(classSessionRepository.findById(203L)).thenReturn(Optional.of(validSession2));
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(200L, 100L)).thenReturn(false);
        when(exceptionRepository.existsBySessionIdAndTutorProfileId(203L, 100L)).thenReturn(false);
        when(exceptionRepository.saveAll(anyList())).thenReturn(List.of(exception1, exception2));

        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder().id(300L).build(),
                        ExceptionResponse.builder().id(301L).build());
        when(exceptionMapper.toResponseList(anyList())).thenReturn(mappedResponses);

        // When
        List<ExceptionResponse> response =
                tutorAvailabilityExceptionService.createBatchExceptions(request);

        // Then
        assertEquals(2, response.size());
        verify(exceptionRepository)
                .saveAll(
                        argThat(
                                list -> {
                                    if (list instanceof List) {
                                        return ((List<?>) list).size() == 2;
                                    }
                                    return false;
                                }));
    }

    @Test
    @DisplayName("Should handle empty session list in batch creation")
    void should_handleEmptySessionListInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of())
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip sessions when repository throws exception in batch creation")
    void should_skipSessionsWhenRepositoryThrowsExceptionInBatchCreation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        BatchCreateExceptionRequest request =
                BatchCreateExceptionRequest.builder()
                        .sessionIds(List.of(200L, 201L))
                        .reason("Need time off")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L))
                .thenThrow(new RuntimeException("Database error"));
        when(classSessionRepository.findById(201L))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createBatchExceptions(request));
        assertEquals(ErrorCode.NO_VALID_SESSIONS, exception.getErrorCode());
        verify(exceptionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should update exception when pending")
    void should_updateExceptionWhenPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.PENDING)
                        .reason("Old reason")
                        .build();

        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        ExceptionResponse mapped = ExceptionResponse.builder().id(500L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(exception)).thenReturn(exception);
        when(exceptionMapper.toResponse(exception)).thenReturn(mapped);

        // When
        ExceptionResponse response =
                tutorAvailabilityExceptionService.updateException(500L, request);

        // Then
        assertEquals("New reason", exception.getReason());
        assertEquals(500L, response.getId());
        verify(exceptionRepository).save(exception);
    }

    @Test
    @DisplayName("Should throw when updating non-pending exception")
    void should_throwWhenUpdatingNonPendingException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.APPROVED)
                        .build();

        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in updateException")
    void should_throwUserNotExistedWhenUserNotFoundInUpdateException() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
        verify(exceptionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found in updateException")
    void should_throwTutorProfileNotExistedWhenProfileNotFoundInUpdateException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(exceptionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw EXCEPTION_NOT_FOUND when exception not found in updateException")
    void should_throwExceptionNotFoundWhenExceptionNotFoundInUpdateException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.EXCEPTION_NOT_FOUND, exception.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw UNAUTHORIZED when exception does not belong to tutor in updateException")
    void should_throwUnauthorizedWhenExceptionDoesNotBelongToTutorInUpdateException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorProfile anotherProfile = TutorProfile.builder().id(200L).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(anotherProfile) // Belongs to different tutor
                        .status(ExceptionStatus.PENDING)
                        .build();

        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.UNAUTHORIZED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when updating REJECTED exception")
    void should_throwWhenUpdatingRejectedException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when updating CANCELLED exception")
    void should_throwWhenUpdatingCancelledException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        UpdateExceptionRequest request =
                UpdateExceptionRequest.builder().reason("New reason").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.updateException(500L, request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update exception with null reason when PENDING")
    void should_updateExceptionWithNullReasonWhenPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.PENDING)
                        .reason("Old reason")
                        .build();

        UpdateExceptionRequest request = UpdateExceptionRequest.builder().reason(null).build();

        ExceptionResponse mapped = ExceptionResponse.builder().id(500L).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(exception)).thenReturn(exception);
        when(exceptionMapper.toResponse(exception)).thenReturn(mapped);

        // When
        ExceptionResponse response =
                tutorAvailabilityExceptionService.updateException(500L, request);

        // Then
        assertEquals("Old reason", exception.getReason()); // Reason should not change
        assertEquals(500L, response.getId());
        verify(exceptionRepository).save(exception);
    }

    @Test
    @DisplayName("Should throw when canceling non-pending exception")
    void should_throwWhenCancelingNonPendingException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.APPROVED)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in cancelException")
    void should_throwUserNotExistedWhenUserNotFoundInCancelException() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
        verify(exceptionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found in cancelException")
    void should_throwTutorProfileNotExistedWhenProfileNotFoundInCancelException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(exceptionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw EXCEPTION_NOT_FOUND when exception not found in cancelException")
    void should_throwExceptionNotFoundWhenExceptionNotFoundInCancelException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.EXCEPTION_NOT_FOUND, exception.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw UNAUTHORIZED when exception does not belong to tutor in cancelException")
    void should_throwUnauthorizedWhenExceptionDoesNotBelongToTutorInCancelException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        TutorProfile anotherProfile = TutorProfile.builder().id(200L).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(anotherProfile) // Belongs to different tutor
                        .status(ExceptionStatus.PENDING)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.UNAUTHORIZED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when canceling REJECTED exception")
    void should_throwWhenCancelingRejectedException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when canceling already CANCELLED exception")
    void should_throwWhenCancelingAlreadyCancelledException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(500L)
                        .tutorProfile(profile)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findById(500L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelException(500L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_EXCEPTION, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject exception with reason")
    void should_rejectExceptionWithReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        ClassSession session = new ClassSession();
        session.setId(200L);
        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTitle("Math Class");
        session.setTutorClass(tutorClass);

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.PENDING)
                        .tutorProfile(
                                TutorProfile.builder()
                                        .user(User.builder().userId("tutor-1").build())
                                        .build())
                        .session(session)
                        .isApproved(false)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason("Invalid reason")
                        .build();

        ExceptionResponse mapped =
                ExceptionResponse.builder().id(600L).status(ExceptionStatus.REJECTED).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));
        when(exceptionRepository.save(exception)).thenReturn(exception);
        when(exceptionMapper.toResponse(exception)).thenReturn(mapped);

        // When
        ExceptionResponse response = tutorAvailabilityExceptionService.approveException(request);

        // Then
        assertEquals(ExceptionStatus.REJECTED, exception.getStatus());
        assertFalse(exception.getIsApproved());
        assertEquals("Invalid reason", exception.getRejectionReason());
        assertEquals(600L, response.getId());
    }

    @Test
    @DisplayName("Should throw when rejecting without reason")
    void should_throwWhenRejectingWithoutReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.PENDING)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason(null)
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exceptionThrown.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when approving already processed exception")
    void should_throwWhenApprovingAlreadyProcessedException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.APPROVED)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_PROCESSED, exceptionThrown.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in approveException")
    void should_throwUserNotExistedWhenUserNotFoundInApproveException() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(exceptionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw EXCEPTION_NOT_FOUND when exception not found in approveException")
    void should_throwExceptionNotFoundWhenExceptionNotFoundInApproveException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_NOT_FOUND, exception.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when rejecting with blank reason")
    void should_throwWhenRejectingWithBlankReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.PENDING)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason("   ")
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when approving REJECTED exception")
    void should_throwWhenApprovingRejectedException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_PROCESSED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when approving CANCELLED exception")
    void should_throwWhenApprovingCancelledException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder().exceptionId(600L).approved(true).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_PROCESSED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when rejecting REJECTED exception")
    void should_throwWhenRejectingRejectedException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason("Already rejected")
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_PROCESSED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when rejecting CANCELLED exception")
    void should_throwWhenRejectingCancelledException() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason("Cannot reject cancelled")
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_PROCESSED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject exception with empty rejection reason should throw")
    void should_throwWhenRejectingWithEmptyReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();
        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(600L)
                        .status(ExceptionStatus.PENDING)
                        .build();

        ApproveExceptionRequest request =
                ApproveExceptionRequest.builder()
                        .exceptionId(600L)
                        .approved(false)
                        .rejectionReason("")
                        .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(exceptionRepository.findById(600L)).thenReturn(Optional.of(exception));

        // When & Then
        AppException exceptionThrown =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.approveException(request));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exceptionThrown.getErrorCode());
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get my exceptions filtered by status")
    void should_getMyExceptionsFilteredByStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException pendingException =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();

        List<TutorAvailabilityException> exceptions = List.of(pendingException);
        List<ExceptionResponse> mappedResponses =
                List.of(ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileIdAndStatus(100L, ExceptionStatus.PENDING))
                .thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response =
                tutorAvailabilityExceptionService.getMyExceptions(ExceptionStatus.PENDING);

        // Then
        assertEquals(1L, response.getTotalCount());
        assertEquals(1L, response.getPendingCount());
        verify(exceptionRepository).findByTutorProfileIdAndStatus(100L, ExceptionStatus.PENDING);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in getMyExceptions")
    void should_throwUserNotExistedWhenUserNotFoundInGetMyExceptions() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.getMyExceptions(null));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
        verify(exceptionRepository, never()).findByTutorProfileId(anyLong());
    }

    @Test
    @DisplayName("Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found in getMyExceptions")
    void should_throwTutorProfileNotExistedWhenProfileNotFoundInGetMyExceptions() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.getMyExceptions(null));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(exceptionRepository, never()).findByTutorProfileId(anyLong());
    }

    @Test
    @DisplayName("Should return empty list when tutor has no exceptions")
    void should_returnEmptyListWhenTutorHasNoExceptions() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileId(100L)).thenReturn(List.of());
        when(exceptionMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        ExceptionListResponse response = tutorAvailabilityExceptionService.getMyExceptions(null);

        // Then
        assertEquals(0L, response.getTotalCount());
        assertEquals(0L, response.getPendingCount());
        assertEquals(0L, response.getApprovedCount());
        assertEquals(0L, response.getRejectedCount());
        assertTrue(response.getExceptions().isEmpty());
    }

    @Test
    @DisplayName("Should get my exceptions filtered by APPROVED status")
    void should_getMyExceptionsFilteredByApprovedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException approvedException =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.APPROVED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(approvedException);
        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder()
                                .id(1L)
                                .status(ExceptionStatus.APPROVED)
                                .build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileIdAndStatus(100L, ExceptionStatus.APPROVED))
                .thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response =
                tutorAvailabilityExceptionService.getMyExceptions(ExceptionStatus.APPROVED);

        // Then
        assertEquals(1L, response.getTotalCount());
        assertEquals(0L, response.getPendingCount());
        assertEquals(1L, response.getApprovedCount());
        assertEquals(0L, response.getRejectedCount());
        verify(exceptionRepository).findByTutorProfileIdAndStatus(100L, ExceptionStatus.APPROVED);
    }

    @Test
    @DisplayName("Should get my exceptions filtered by REJECTED status")
    void should_getMyExceptionsFilteredByRejectedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException rejectedException =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(rejectedException);
        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder()
                                .id(1L)
                                .status(ExceptionStatus.REJECTED)
                                .build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileIdAndStatus(100L, ExceptionStatus.REJECTED))
                .thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response =
                tutorAvailabilityExceptionService.getMyExceptions(ExceptionStatus.REJECTED);

        // Then
        assertEquals(1L, response.getTotalCount());
        assertEquals(0L, response.getPendingCount());
        assertEquals(0L, response.getApprovedCount());
        assertEquals(1L, response.getRejectedCount());
        verify(exceptionRepository).findByTutorProfileIdAndStatus(100L, ExceptionStatus.REJECTED);
    }

    @Test
    @DisplayName("Should get my exceptions filtered by CANCELLED status")
    void should_getMyExceptionsFilteredByCancelledStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException cancelledException =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(cancelledException);
        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder()
                                .id(1L)
                                .status(ExceptionStatus.CANCELLED)
                                .build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileIdAndStatus(100L, ExceptionStatus.CANCELLED))
                .thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response =
                tutorAvailabilityExceptionService.getMyExceptions(ExceptionStatus.CANCELLED);

        // Then
        assertEquals(1L, response.getTotalCount());
        assertEquals(0L, response.getPendingCount());
        assertEquals(0L, response.getApprovedCount());
        assertEquals(0L, response.getRejectedCount());
        verify(exceptionRepository).findByTutorProfileIdAndStatus(100L, ExceptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should get all my exceptions with mixed statuses and correct counts")
    void should_getAllMyExceptionsWithMixedStatusesAndCorrectCounts() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorAvailabilityException pendingException1 =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException pendingException2 =
                TutorAvailabilityException.builder().id(2L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException approvedException1 =
                TutorAvailabilityException.builder()
                        .id(3L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        TutorAvailabilityException approvedException2 =
                TutorAvailabilityException.builder()
                        .id(4L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        TutorAvailabilityException approvedException3 =
                TutorAvailabilityException.builder()
                        .id(5L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        TutorAvailabilityException rejectedException =
                TutorAvailabilityException.builder()
                        .id(6L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        List<TutorAvailabilityException> exceptions =
                List.of(
                        pendingException1,
                        pendingException2,
                        approvedException1,
                        approvedException2,
                        approvedException3,
                        rejectedException);

        List<ExceptionResponse> mappedResponses =
                List.of(
                        ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build(),
                        ExceptionResponse.builder().id(2L).status(ExceptionStatus.PENDING).build(),
                        ExceptionResponse.builder().id(3L).status(ExceptionStatus.APPROVED).build(),
                        ExceptionResponse.builder().id(4L).status(ExceptionStatus.APPROVED).build(),
                        ExceptionResponse.builder().id(5L).status(ExceptionStatus.APPROVED).build(),
                        ExceptionResponse.builder()
                                .id(6L)
                                .status(ExceptionStatus.REJECTED)
                                .build());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(exceptionRepository.findByTutorProfileId(100L)).thenReturn(exceptions);
        when(exceptionMapper.toResponseList(exceptions)).thenReturn(mappedResponses);

        // When
        ExceptionListResponse response = tutorAvailabilityExceptionService.getMyExceptions(null);

        // Then
        assertEquals(6L, response.getTotalCount());
        assertEquals(2L, response.getPendingCount());
        assertEquals(3L, response.getApprovedCount());
        assertEquals(1L, response.getRejectedCount());
        assertEquals(6, response.getExceptions().size());
    }

    @Test
    @DisplayName("Should create schedule change successfully")
    void should_createScheduleChangeSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .session(session)
                        .oldDate(request.getOldDate())
                        .newDate(request.getNewDate())
                        .newSLot(request.getNewSlot())
                        .content(request.getContent())
                        .status("PENDING")
                        .build();

        ScheduleChangeResponse mapped =
                ScheduleChangeResponse.builder().id(300L).status("PENDING").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(false);
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.empty());
        when(classEnrollmentRepository.findByTutorClassId(anyLong())).thenReturn(List.of());
        when(scheduleChangeRepository.save(any(ScheduleChange.class))).thenReturn(scheduleChange);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(mapped);

        // When
        ScheduleChangeResponse response =
                tutorAvailabilityExceptionService.createScheduleChange(request);

        // Then
        assertEquals(300L, response.getId());
        verify(scheduleChangeRepository).save(any(ScheduleChange.class));
    }

    @Test
    @DisplayName("Should throw when new date is in the past")
    void should_throwWhenNewDateInPast() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().minusDays(1)) // Past date
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.NEW_DATE_MUST_BE_FUTURE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when schedule change too late")
    void should_throwWhenScheduleChangeTooLate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(
                                LocalDate.now()
                                        .plusDays(5)) // Less than 7 days - too late to change
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_TOO_LATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when old and new date are same")
    void should_throwWhenOldAndNewDateSame() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        LocalDate sameDate = LocalDate.now().plusDays(10);
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(sameDate)
                        .newDate(sameDate) // Same date
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.OLD_AND_NEW_DATE_SAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when schedule change already exists")
    void should_throwWhenScheduleChangeAlreadyExists() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in createScheduleChange")
    void should_throwUserNotExistedWhenUserNotFoundInCreateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository, never())
                .findByUserUserIdAndSubmissionStatus(anyString(), any());
    }

    @Test
    @DisplayName(
            "Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found in createScheduleChange")
    void should_throwTutorProfileNotExistedWhenProfileNotFoundInCreateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw SESSION_NOT_EXISTED when session not found in createScheduleChange")
    void should_throwSessionNotExistedWhenSessionNotFoundInCreateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName(
            "Should throw UNAUTHORIZED when session does not belong to tutor in createScheduleChange")
    void should_throwUnauthorizedWhenSessionDoesNotBelongToTutorInCreateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        User anotherTutor = User.builder().userId("tutor-2").username("another").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(anotherTutor); // Session belongs to different tutor

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName(
            "Should throw SESSION_MUST_REGISTER_EXCEPTION when session has no approved exception")
    void should_throwSessionMustRegisterExceptionWhenNoApprovedException() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of()); // Empty - no approved exception

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.SESSION_MUST_REGISTER_EXCEPTION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw INVALID_TEACHING_SLOT when slot is less than 1")
    void should_throwInvalidTeachingSlotWhenSlotLessThan1() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(0) // Invalid slot < 1
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.INVALID_TEACHING_SLOT, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw INVALID_TEACHING_SLOT when slot is greater than 10")
    void should_throwInvalidTeachingSlotWhenSlotGreaterThan10() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(11) // Invalid slot > 10
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.INVALID_TEACHING_SLOT, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw OVERLAP_TUTOR_SCHEDULE when tutor has conflict")
    void should_throwOverlapTutorScheduleWhenTutorHasConflict() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ClassSession conflictSession = new ClassSession();
        conflictSession.setId(999L);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(false);
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        eq("tutor-1"), any(LocalDate.class), eq(1)))
                .thenReturn(Optional.of(conflictSession)); // Tutor has conflict

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.OVERLAP_TUTOR_SCHEDULE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw OVERLAP_STUDENT_SCHEDULE when student has conflict")
    void should_throwOverlapStudentScheduleWhenStudentHasConflict() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(100L).user(tutor).build();

        User student = User.builder().userId("student-1").build();
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setTutor(tutor);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .sessionId(200L)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSlot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Need to reschedule")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(tutorProfileRepository.findByUserUserIdAndSubmissionStatus(
                        "tutor-1", com.sep.educonnect.enums.ProfileStatus.APPROVED))
                .thenReturn(Optional.of(profile));
        when(classSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(exceptionRepository.findBySessionAndIsApproved(200L, true))
                .thenReturn(List.of(TutorAvailabilityException.builder().build()));
        when(scheduleChangeRepository.existsBySessionIdAndStatusPending(200L)).thenReturn(false);
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        eq("tutor-1"), any(LocalDate.class), eq(1)))
                .thenReturn(Optional.empty()); // No tutor conflict
        when(classEnrollmentRepository.findByTutorClassId(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.countStudentConflicts(
                        anyList(), any(LocalDate.class), eq(1), eq(1L)))
                .thenReturn(1L); // Student has conflict

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.createScheduleChange(request));
        assertEquals(ErrorCode.OVERLAP_STUDENT_SCHEDULE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should update schedule change when pending")
    void should_updateScheduleChangeWhenPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);

        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .status("PENDING")
                        .session(session)
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(15))
                        .newSLot(TeachingSlot.SLOT_1.getSlotNumber())
                        .content("Old content")
                        .build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        ScheduleChangeResponse mapped =
                ScheduleChangeResponse.builder().id(300L).status("PENDING").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(scheduleChangeRepository.save(scheduleChange)).thenReturn(scheduleChange);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(mapped);
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.empty());
        when(classEnrollmentRepository.findByTutorClassId(anyLong())).thenReturn(List.of());

        // When
        ScheduleChangeResponse response =
                tutorAvailabilityExceptionService.updateScheduleChange(300L, request);

        // Then
        assertEquals(LocalDate.now().plusDays(20), scheduleChange.getNewDate());
        assertEquals(TeachingSlot.SLOT_2.getSlotNumber(), scheduleChange.getNewSLot());
        assertEquals("New content", scheduleChange.getContent());
        assertEquals(300L, response.getId());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in updateScheduleChange")
    void should_throwUserNotExistedWhenUserNotFoundInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName(
            "Should throw SCHEDULE_CHANGE_NOT_FOUND when schedule change not found in updateScheduleChange")
    void should_throwScheduleChangeNotFoundWhenNotFoundInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw UNAUTHORIZED when schedule change does not belong to user in updateScheduleChange")
    void should_throwUnauthorizedWhenScheduleChangeDoesNotBelongToUserInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy("another-tutor"); // Different owner

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw OLD_AND_NEW_DATE_SAME when dates are same in updateScheduleChange")
    void should_throwOldAndNewDateSameWhenDatesAreSameInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        LocalDate sameDate = LocalDate.now().plusDays(10);
        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(sameDate)
                        .newDate(sameDate) // Same date
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.OLD_AND_NEW_DATE_SAME, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw INVALID_TEACHING_SLOT when slot less than 1 in updateScheduleChange")
    void should_throwInvalidTeachingSlotWhenSlotLessThan1InUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(0) // Invalid slot < 1
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.INVALID_TEACHING_SLOT, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw INVALID_TEACHING_SLOT when slot greater than 10 in updateScheduleChange")
    void should_throwInvalidTeachingSlotWhenSlotGreaterThan10InUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(11) // Invalid slot > 10
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.INVALID_TEACHING_SLOT, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE when status is APPROVED")
    void should_throwCannotModifyProcessedScheduleChangeWhenStatusIsApproved() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .status("APPROVED") // Already approved
                        .session(session)
                        .build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE when status is REJECTED")
    void should_throwCannotModifyProcessedScheduleChangeWhenStatusIsRejected() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .status("REJECTED") // Already rejected
                        .session(session)
                        .build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw NEW_DATE_MUST_BE_FUTURE when new date is in past in updateScheduleChange")
    void should_throwNewDateMustBeFutureWhenNewDateIsInPastInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().minusDays(1)) // Past date
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.empty());
        when(classEnrollmentRepository.findByTutorClassId(anyLong())).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.NEW_DATE_MUST_BE_FUTURE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw SCHEDULE_CHANGE_TOO_LATE when new date is too close in updateScheduleChange")
    void should_throwScheduleChangeTooLateWhenNewDateIsTooCloseInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(5)) // Less than 7 days
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.empty());
        when(classEnrollmentRepository.findByTutorClassId(anyLong())).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_TOO_LATE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw OVERLAP_TUTOR_SCHEDULE when tutor has conflict in updateScheduleChange")
    void should_throwOverlapTutorScheduleWhenTutorHasConflictInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ClassSession conflictSession = new ClassSession();
        conflictSession.setId(999L);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        eq("tutor-1"), any(LocalDate.class), eq(2)))
                .thenReturn(Optional.of(conflictSession)); // Tutor has conflict

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.OVERLAP_TUTOR_SCHEDULE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw OVERLAP_STUDENT_SCHEDULE when student has conflict in updateScheduleChange")
    void should_throwOverlapStudentScheduleWhenStudentHasConflictInUpdateScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        User student = User.builder().userId("student-1").build();
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        ClassSession session = new ClassSession();
        session.setId(200L);
        session.setTutorClass(tutorClass);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("PENDING").session(session).build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        CreateScheduleChangeRequest request =
                CreateScheduleChangeRequest.builder()
                        .oldDate(LocalDate.now().plusDays(10))
                        .newDate(LocalDate.now().plusDays(20))
                        .newSlot(TeachingSlot.SLOT_2.getSlotNumber())
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(classSessionRepository.findBySessionDateAndSlotNumber(
                        eq("tutor-1"), any(LocalDate.class), eq(2)))
                .thenReturn(Optional.empty()); // No tutor conflict
        when(classEnrollmentRepository.findByTutorClassId(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.countStudentConflicts(
                        anyList(), any(LocalDate.class), eq(2), eq(1L)))
                .thenReturn(1L); // Student has conflict

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.updateScheduleChange(
                                        300L, request));
        assertEquals(ErrorCode.OVERLAP_STUDENT_SCHEDULE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel schedule change when pending")
    void should_cancelScheduleChangeWhenPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange.setCreatedBy(tutor.getUserId());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(scheduleChangeRepository.save(scheduleChange)).thenReturn(scheduleChange);

        // When
        tutorAvailabilityExceptionService.cancelScheduleChange(300L);

        // Then
        assertEquals("CANCELLED", scheduleChange.getStatus());
        verify(scheduleChangeRepository).save(scheduleChange);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in cancelScheduleChange")
    void should_throwUserNotExistedWhenUserNotFoundInCancelScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName(
            "Should throw SCHEDULE_CHANGE_NOT_FOUND when schedule change not found in cancelScheduleChange")
    void should_throwScheduleChangeNotFoundWhenNotFoundInCancelScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw UNAUTHORIZED when schedule change does not belong to user in cancelScheduleChange")
    void should_throwUnauthorizedWhenScheduleChangeDoesNotBelongToUserInCancelScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange.setCreatedBy("another-tutor-id"); // Different owner

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE when status is APPROVED")
    void should_throwCannotCancelProcessedScheduleChangeWhenStatusIsApproved() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("APPROVED").build();
        scheduleChange.setCreatedBy(tutor.getUserId());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE when status is REJECTED")
    void should_throwCannotCancelProcessedScheduleChangeWhenStatusIsRejected() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("REJECTED").build();
        scheduleChange.setCreatedBy(tutor.getUserId());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Should throw CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE when status is already CANCELLED")
    void should_throwCannotCancelProcessedScheduleChangeWhenStatusIsAlreadyCancelled() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("CANCELLED").build();
        scheduleChange.setCreatedBy(tutor.getUserId());

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorAvailabilityExceptionService.cancelScheduleChange(300L));
        assertEquals(ErrorCode.CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should approve schedule change")
    void should_approveScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .status("PENDING")
                        .content("Need to reschedule")
                        .build();

        ScheduleChangeResponse mapped =
                ScheduleChangeResponse.builder().id(300L).status("APPROVED").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(scheduleChangeRepository.save(scheduleChange)).thenReturn(scheduleChange);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(mapped);

        // When
        ScheduleChangeResponse response =
                tutorAvailabilityExceptionService.approveScheduleChange(300L, true, null);

        // Then
        assertEquals("APPROVED", scheduleChange.getStatus());
        assertEquals(300L, response.getId());
    }

    @Test
    @DisplayName("Should reject schedule change with reason")
    void should_rejectScheduleChangeWithReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(300L)
                        .status("PENDING")
                        .content("Need to reschedule")
                        .build();

        ScheduleChangeResponse mapped =
                ScheduleChangeResponse.builder().id(300L).status("REJECTED").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));
        when(scheduleChangeRepository.save(scheduleChange)).thenReturn(scheduleChange);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(mapped);

        // When
        ScheduleChangeResponse response =
                tutorAvailabilityExceptionService.approveScheduleChange(
                        300L, false, "Invalid request");

        // Then
        assertEquals("REJECTED", scheduleChange.getStatus());
        assertEquals("Invalid request", scheduleChange.getContent());
        assertEquals(300L, response.getId());
    }

    @Test
    @DisplayName("Should throw when rejecting schedule change without reason")
    void should_throwWhenRejectingScheduleChangeWithoutReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, false, null));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in approveScheduleChange")
    void should_throwUserNotExistedWhenUserNotFoundInApproveScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, true, null));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName(
            "Should throw SCHEDULE_CHANGE_NOT_FOUND when schedule change not found in approveScheduleChange")
    void should_throwScheduleChangeNotFoundWhenNotFoundInApproveScheduleChange() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, true, null));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SCHEDULE_CHANGE_ALREADY_PROCESSED when status is APPROVED")
    void should_throwScheduleChangeAlreadyProcessedWhenStatusIsApproved() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("APPROVED").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, true, null));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_ALREADY_PROCESSED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SCHEDULE_CHANGE_ALREADY_PROCESSED when status is REJECTED")
    void should_throwScheduleChangeAlreadyProcessedWhenStatusIsRejected() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("REJECTED").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, true, null));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_ALREADY_PROCESSED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SCHEDULE_CHANGE_ALREADY_PROCESSED when status is CANCELLED")
    void should_throwScheduleChangeAlreadyProcessedWhenStatusIsCancelled() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("CANCELLED").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, false, "Reason"));
        assertEquals(ErrorCode.SCHEDULE_CHANGE_ALREADY_PROCESSED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when rejecting schedule change with blank reason")
    void should_throwWhenRejectingScheduleChangeWithBlankReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, false, "   "));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when rejecting schedule change with empty reason")
    void should_throwWhenRejectingScheduleChangeWithEmptyReason() {
        // Given
        MockHelper.mockSecurityContext("admin");
        User admin = User.builder().userId("admin-1").username("admin").build();

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(scheduleChangeRepository.findById(300L)).thenReturn(Optional.of(scheduleChange));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.approveScheduleChange(
                                        300L, false, ""));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
        verify(scheduleChangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get my schedule changes")
    void should_getMyScheduleChanges() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("PENDING").build());
        LocalDate endDate = startDate.plusDays(6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndNewDateBetween(
                        tutor.getUsername(), startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges(null, startDate);

        // Then
        assertEquals(1, response.size());
        assertEquals(300L, response.get(0).getId());
    }

    @Test
    @DisplayName("Should get pending schedule changes with pagination")
    void should_getPendingScheduleChangesWithPagination() {
        // Given
        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange1 = ScheduleChange.builder().id(1L).status("PENDING").build();
        ScheduleChange scheduleChange2 = ScheduleChange.builder().id(2L).status("PENDING").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange1, scheduleChange2);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 2);

        ScheduleChangeResponse response1 =
                ScheduleChangeResponse.builder().id(1L).status("PENDING").build();
        ScheduleChangeResponse response2 =
                ScheduleChangeResponse.builder().id(2L).status("PENDING").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange1)).thenReturn(response1);
        when(scheduleChangeMapper.toResponse(scheduleChange2)).thenReturn(response2);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getPendingScheduleChanges(null, 0, 10, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should return empty page when no pending schedule changes exist")
    void should_returnEmptyPageWhenNoPendingScheduleChangesExist() {
        // Given
        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<ScheduleChange> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getPendingScheduleChanges(null, 0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get pending schedule changes with different page size")
    void should_getPendingScheduleChangesWithDifferentPageSize() {
        // Given
        Pageable pageable =
                PageRequest.of(
                        1,
                        5,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "createdAt")); // Page 1, size 5

        ScheduleChange scheduleChange1 = ScheduleChange.builder().id(6L).status("PENDING").build();
        ScheduleChange scheduleChange2 = ScheduleChange.builder().id(7L).status("PENDING").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange1, scheduleChange2);
        Page<ScheduleChange> scheduleChangePage =
                new PageImpl<>(scheduleChanges, pageable, 12); // 12 total

        ScheduleChangeResponse response1 =
                ScheduleChangeResponse.builder().id(6L).status("PENDING").build();
        ScheduleChangeResponse response2 =
                ScheduleChangeResponse.builder().id(7L).status("PENDING").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange1)).thenReturn(response1);
        when(scheduleChangeMapper.toResponse(scheduleChange2)).thenReturn(response2);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getPendingScheduleChanges(null, 1, 5, null);

        // Then
        assertEquals(12, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages()); // 12 items / 5 per page = 3 pages
        assertEquals(1, result.getNumber()); // Current page number
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get single pending schedule change")
    void should_getSinglePendingScheduleChange() {
        // Given
        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange =
                ScheduleChange.builder()
                        .id(1L)
                        .status("PENDING")
                        .content("Need to reschedule")
                        .build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 1);

        ScheduleChangeResponse response =
                ScheduleChangeResponse.builder()
                        .id(1L)
                        .status("PENDING")
                        .content("Need to reschedule")
                        .build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(response);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getPendingScheduleChanges(null, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Need to reschedule", result.getContent().get(0).getContent());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get all schedule changes without status filter")
    void should_getAllScheduleChangesWithoutStatusFilter() {
        // Given
        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange1 = ScheduleChange.builder().id(1L).status("PENDING").build();
        ScheduleChange scheduleChange2 = ScheduleChange.builder().id(2L).status("APPROVED").build();
        ScheduleChange scheduleChange3 = ScheduleChange.builder().id(3L).status("REJECTED").build();

        List<ScheduleChange> scheduleChanges =
                List.of(scheduleChange1, scheduleChange2, scheduleChange3);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 3);

        ScheduleChangeResponse response1 =
                ScheduleChangeResponse.builder().id(1L).status("PENDING").build();
        ScheduleChangeResponse response2 =
                ScheduleChangeResponse.builder().id(2L).status("APPROVED").build();
        ScheduleChangeResponse response3 =
                ScheduleChangeResponse.builder().id(3L).status("REJECTED").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange1)).thenReturn(response1);
        when(scheduleChangeMapper.toResponse(scheduleChange2)).thenReturn(response2);
        when(scheduleChangeMapper.toResponse(scheduleChange3)).thenReturn(response3);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(null, null, 0, 10, null);

        // Then
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        assertEquals("APPROVED", result.getContent().get(1).getStatus());
        assertEquals("REJECTED", result.getContent().get(2).getStatus());
        verify(scheduleChangeRepository).searchScheduleChanges(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get all schedule changes filtered by PENDING status")
    void should_getAllScheduleChangesFilteredByPendingStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange1 = ScheduleChange.builder().id(1L).status("PENDING").build();
        ScheduleChange scheduleChange2 = ScheduleChange.builder().id(2L).status("PENDING").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange1, scheduleChange2);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 2);

        ScheduleChangeResponse response1 =
                ScheduleChangeResponse.builder().id(1L).status("PENDING").build();
        ScheduleChangeResponse response2 =
                ScheduleChangeResponse.builder().id(2L).status("PENDING").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange1)).thenReturn(response1);
        when(scheduleChangeMapper.toResponse(scheduleChange2)).thenReturn(response2);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "PENDING", null, 0, 10, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(r -> r.getStatus().equals("PENDING")));
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
        verify(scheduleChangeRepository, never()).findAll((Pageable) any());
    }

    @Test
    @DisplayName("Should get all schedule changes filtered by APPROVED status")
    void should_getAllScheduleChangesFilteredByApprovedStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange = ScheduleChange.builder().id(1L).status("APPROVED").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 1);

        ScheduleChangeResponse response =
                ScheduleChangeResponse.builder().id(1L).status("APPROVED").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(response);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "APPROVED", null, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("APPROVED", result.getContent().get(0).getStatus());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("APPROVED"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get all schedule changes filtered by REJECTED status")
    void should_getAllScheduleChangesFilteredByRejectedStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange = ScheduleChange.builder().id(1L).status("REJECTED").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 1);

        ScheduleChangeResponse response =
                ScheduleChangeResponse.builder().id(1L).status("REJECTED").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(response);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "REJECTED", null, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("REJECTED", result.getContent().get(0).getStatus());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("REJECTED"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get all schedule changes filtered by CANCELLED status")
    void should_getAllScheduleChangesFilteredByCancelledStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange = ScheduleChange.builder().id(1L).status("CANCELLED").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 1);

        ScheduleChangeResponse response =
                ScheduleChangeResponse.builder().id(1L).status("CANCELLED").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(response);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "CANCELLED", null, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("CANCELLED", result.getContent().get(0).getStatus());
        verify(scheduleChangeRepository)
                .searchScheduleChanges(eq("CANCELLED"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should return empty page when no schedule changes exist in getAllScheduleChanges")
    void should_returnEmptyPageWhenNoScheduleChangesExistInGetAllScheduleChanges() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScheduleChange> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(null, null, 0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(scheduleChangeRepository).searchScheduleChanges(any(), any(), eq(pageable));
    }

    @Test
    @DisplayName(
            "Should return empty page when no schedule changes with specific status exist in getAllScheduleChanges")
    void
            should_returnEmptyPageWhenNoScheduleChangesWithSpecificStatusExistInGetAllScheduleChanges() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScheduleChange> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "PENDING", null, 0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(scheduleChangeRepository).searchScheduleChanges(eq("PENDING"), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should get all schedule changes with pagination")
    void should_getAllScheduleChangesWithPagination() {
        // Given
        Pageable pageable =
                PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt")); // Page 1, size 5

        ScheduleChange scheduleChange1 = ScheduleChange.builder().id(6L).status("APPROVED").build();
        ScheduleChange scheduleChange2 = ScheduleChange.builder().id(7L).status("REJECTED").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange1, scheduleChange2);
        Page<ScheduleChange> scheduleChangePage =
                new PageImpl<>(scheduleChanges, pageable, 15); // 15 total

        ScheduleChangeResponse response1 =
                ScheduleChangeResponse.builder().id(6L).status("APPROVED").build();
        ScheduleChangeResponse response2 =
                ScheduleChangeResponse.builder().id(7L).status("REJECTED").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange1)).thenReturn(response1);
        when(scheduleChangeMapper.toResponse(scheduleChange2)).thenReturn(response2);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(null, null, 1, 5, null);

        // Then
        assertEquals(15, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages()); // 15 items / 5 per page = 3 pages
        assertEquals(1, result.getNumber()); // Current page number
        verify(scheduleChangeRepository).searchScheduleChanges(any(), any(), eq(pageable));
    }

    @Test
    @DisplayName("Should treat blank status as null in getAllScheduleChanges")
    void should_treatBlankStatusAsNullInGetAllScheduleChanges() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ScheduleChange scheduleChange = ScheduleChange.builder().id(1L).status("PENDING").build();

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        Page<ScheduleChange> scheduleChangePage = new PageImpl<>(scheduleChanges, pageable, 1);

        ScheduleChangeResponse response =
                ScheduleChangeResponse.builder().id(1L).status("PENDING").build();

        when(scheduleChangeRepository.searchScheduleChanges(any(), any(), any(Pageable.class)))
                .thenReturn(scheduleChangePage);
        when(scheduleChangeMapper.toResponse(scheduleChange)).thenReturn(response);

        // When
        Page<ScheduleChangeResponse> result =
                tutorAvailabilityExceptionService.getAllScheduleChanges(
                        "   ", null, 0, 10, null); // Blank string

        // Then
        assertEquals(1, result.getTotalElements());
        verify(scheduleChangeRepository).searchScheduleChanges(any(), any(), eq(pageable));
        verify(scheduleChangeRepository, never()).findByStatus(anyString(), any());
    }

    @Test
    @DisplayName("Should get my schedule changes filtered by status")
    void should_getMyScheduleChangesFilteredByStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("PENDING").build());

        LocalDate endDate = startDate.plusDays(6);
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "PENDING", startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges("PENDING", startDate);

        // Then
        assertEquals(1, response.size());
        verify(scheduleChangeRepository)
                .findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "PENDING", startDate, endDate);
    }

    @Test
    @DisplayName("Should throw USER_NOT_EXISTED when user not found in getMyScheduleChanges")
    void should_throwUserNotExistedWhenUserNotFoundInGetMyScheduleChanges() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorAvailabilityExceptionService.getMyScheduleChanges(
                                        null, startDate));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(scheduleChangeRepository, never())
                .findByCreatedByAndNewDateBetween(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should return empty list when no schedule changes exist in getMyScheduleChanges")
    void should_returnEmptyListWhenNoScheduleChangesExistInGetMyScheduleChanges() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = startDate.plusDays(6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndNewDateBetween(
                        tutor.getUsername(), startDate, endDate))
                .thenReturn(List.of());
        when(scheduleChangeMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges(null, startDate);

        // Then
        assertTrue(response.isEmpty());
        verify(scheduleChangeRepository)
                .findByCreatedByAndNewDateBetween(tutor.getUsername(), startDate, endDate);
    }

    @Test
    @DisplayName("Should get my schedule changes filtered by APPROVED status")
    void should_getMyScheduleChangesFilteredByApprovedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("APPROVED").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("APPROVED").build());

        LocalDate endDate = startDate.plusDays(6);
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "APPROVED", startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges("APPROVED", startDate);

        // Then
        assertEquals(1, response.size());
        assertEquals("APPROVED", response.get(0).getStatus());
        verify(scheduleChangeRepository)
                .findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "APPROVED", startDate, endDate);
    }

    @Test
    @DisplayName("Should get my schedule changes filtered by REJECTED status")
    void should_getMyScheduleChangesFilteredByRejectedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("REJECTED").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("REJECTED").build());

        LocalDate endDate = startDate.plusDays(6);
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "REJECTED", startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges("REJECTED", startDate);

        // Then
        assertEquals(1, response.size());
        assertEquals("REJECTED", response.get(0).getStatus());
        verify(scheduleChangeRepository)
                .findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "REJECTED", startDate, endDate);
    }

    @Test
    @DisplayName("Should get my schedule changes filtered by CANCELLED status")
    void should_getMyScheduleChangesFilteredByCancelledStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange =
                ScheduleChange.builder().id(300L).status("CANCELLED").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("CANCELLED").build());

        LocalDate endDate = startDate.plusDays(6);
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "CANCELLED", startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges("CANCELLED", startDate);

        // Then
        assertEquals(1, response.size());
        assertEquals("CANCELLED", response.get(0).getStatus());
        verify(scheduleChangeRepository)
                .findByCreatedByAndStatusAndNewDateBetween(
                        tutor.getUsername(), "CANCELLED", startDate, endDate);
    }

    @Test
    @DisplayName("Should treat blank status as null in getMyScheduleChanges")
    void should_treatBlankStatusAsNullInGetMyScheduleChanges() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange = ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges = List.of(scheduleChange);
        List<ScheduleChangeResponse> mapped =
                List.of(ScheduleChangeResponse.builder().id(300L).status("PENDING").build());
        LocalDate endDate = startDate.plusDays(6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndNewDateBetween(
                        tutor.getUsername(), startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges(
                        "   ", startDate); // Blank string

        // Then
        assertEquals(1, response.size());
        verify(scheduleChangeRepository)
                .findByCreatedByAndNewDateBetween(tutor.getUsername(), startDate, endDate);
        verify(scheduleChangeRepository, never())
                .findByCreatedByAndStatusAndNewDateBetween(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Should get multiple schedule changes with different statuses")
    void should_getMultipleScheduleChangesWithDifferentStatuses() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        User tutor = User.builder().userId("tutor-1").username("tutor").build();
        LocalDate startDate = LocalDate.of(2024, 1, 15);

        ScheduleChange scheduleChange1 =
                ScheduleChange.builder().id(300L).status("PENDING").build();
        scheduleChange1.setCreatedBy(tutor.getUsername());
        ScheduleChange scheduleChange2 =
                ScheduleChange.builder().id(301L).status("APPROVED").build();
        scheduleChange2.setCreatedBy(tutor.getUsername());
        ScheduleChange scheduleChange3 =
                ScheduleChange.builder().id(302L).status("REJECTED").build();
        scheduleChange3.setCreatedBy(tutor.getUsername());

        List<ScheduleChange> scheduleChanges =
                List.of(scheduleChange1, scheduleChange2, scheduleChange3);
        List<ScheduleChangeResponse> mapped =
                List.of(
                        ScheduleChangeResponse.builder().id(300L).status("PENDING").build(),
                        ScheduleChangeResponse.builder().id(301L).status("APPROVED").build(),
                        ScheduleChangeResponse.builder().id(302L).status("REJECTED").build());
        LocalDate endDate = startDate.plusDays(6);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutor));
        when(scheduleChangeRepository.findByCreatedByAndNewDateBetween(
                        tutor.getUsername(), startDate, endDate))
                .thenReturn(scheduleChanges);
        when(scheduleChangeMapper.toResponseList(scheduleChanges)).thenReturn(mapped);

        // When
        List<ScheduleChangeResponse> response =
                tutorAvailabilityExceptionService.getMyScheduleChanges(null, startDate);

        // Then
        assertEquals(3, response.size());
        assertEquals(300L, response.get(0).getId());
        assertEquals(301L, response.get(1).getId());
        assertEquals(302L, response.get(2).getId());
        verify(scheduleChangeRepository)
                .findByCreatedByAndNewDateBetween(tutor.getUsername(), startDate, endDate);
    }

    @Test
    @DisplayName("Should get pending exceptions with pagination")
    void should_getPendingExceptionsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder().id(2L).status(ExceptionStatus.PENDING).build();

        List<TutorAvailabilityException> exceptions = List.of(exception1, exception2);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 2);

        ExceptionResponse response1 =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build();
        ExceptionResponse response2 =
                ExceptionResponse.builder().id(2L).status(ExceptionStatus.PENDING).build();

        when(exceptionRepository.findPendingExceptions(pageable)).thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception1)).thenReturn(response1);
        when(exceptionMapper.toResponse(exception2)).thenReturn(response2);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getPendingExceptions(0, 10, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        verify(exceptionRepository).findPendingExceptions(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no pending exceptions exist")
    void should_returnEmptyPageWhenNoPendingExceptionsExist() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TutorAvailabilityException> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(exceptionRepository.findPendingExceptions(pageable)).thenReturn(emptyPage);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getPendingExceptions(0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(exceptionRepository).findPendingExceptions(pageable);
    }

    @Test
    @DisplayName("Should get pending exceptions with different page size")
    void should_getPendingExceptionsWithDifferentPageSize() {
        // Given
        Pageable pageable =
                PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt")); // Page 1, size 5

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder().id(6L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder().id(7L).status(ExceptionStatus.PENDING).build();

        List<TutorAvailabilityException> exceptions = List.of(exception1, exception2);
        Page<TutorAvailabilityException> exceptionPage =
                new PageImpl<>(exceptions, pageable, 12); // 12 total

        ExceptionResponse response1 =
                ExceptionResponse.builder().id(6L).status(ExceptionStatus.PENDING).build();
        ExceptionResponse response2 =
                ExceptionResponse.builder().id(7L).status(ExceptionStatus.PENDING).build();

        when(exceptionRepository.findPendingExceptions(pageable)).thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception1)).thenReturn(response1);
        when(exceptionMapper.toResponse(exception2)).thenReturn(response2);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getPendingExceptions(1, 5, null);

        // Then
        assertEquals(12, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages()); // 12 items / 5 per page = 3 pages
        assertEquals(1, result.getNumber()); // Current page number
        verify(exceptionRepository).findPendingExceptions(pageable);
    }

    @Test
    @DisplayName("Should get single pending exception")
    void should_getSinglePendingException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.PENDING)
                        .reason("Medical appointment")
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 1);

        ExceptionResponse response =
                ExceptionResponse.builder()
                        .id(1L)
                        .status(ExceptionStatus.PENDING)
                        .reason("Medical appointment")
                        .build();

        when(exceptionRepository.findPendingExceptions(pageable)).thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception)).thenReturn(response);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getPendingExceptions(0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Medical appointment", result.getContent().get(0).getReason());
        verify(exceptionRepository).findPendingExceptions(pageable);
    }

    @Test
    @DisplayName("Should get all exceptions without status filter")
    void should_getAllExceptionsWithoutStatusFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder()
                        .id(2L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        TutorAvailabilityException exception3 =
                TutorAvailabilityException.builder()
                        .id(3L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception1, exception2, exception3);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 3);

        ExceptionResponse response1 =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build();
        ExceptionResponse response2 =
                ExceptionResponse.builder().id(2L).status(ExceptionStatus.APPROVED).build();
        ExceptionResponse response3 =
                ExceptionResponse.builder().id(3L).status(ExceptionStatus.REJECTED).build();

        when(exceptionRepository.findAllWithDetails(pageable)).thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception1)).thenReturn(response1);
        when(exceptionMapper.toResponse(exception2)).thenReturn(response2);
        when(exceptionMapper.toResponse(exception3)).thenReturn(response3);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(null, 0, 10, null);

        // Then
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals(ExceptionStatus.PENDING, result.getContent().get(0).getStatus());
        assertEquals(ExceptionStatus.APPROVED, result.getContent().get(1).getStatus());
        assertEquals(ExceptionStatus.REJECTED, result.getContent().get(2).getStatus());
        verify(exceptionRepository).findAllWithDetails(pageable);
        verify(exceptionRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("Should get all exceptions filtered by PENDING status")
    void should_getAllExceptionsFilteredByPendingStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder().id(1L).status(ExceptionStatus.PENDING).build();
        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder().id(2L).status(ExceptionStatus.PENDING).build();

        List<TutorAvailabilityException> exceptions = List.of(exception1, exception2);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 2);

        ExceptionResponse response1 =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.PENDING).build();
        ExceptionResponse response2 =
                ExceptionResponse.builder().id(2L).status(ExceptionStatus.PENDING).build();

        when(exceptionRepository.findByStatus(ExceptionStatus.PENDING, pageable))
                .thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception1)).thenReturn(response1);
        when(exceptionMapper.toResponse(exception2)).thenReturn(response2);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(
                        ExceptionStatus.PENDING, 0, 10, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(
                result.getContent().stream()
                        .allMatch(r -> r.getStatus() == ExceptionStatus.PENDING));
        verify(exceptionRepository).findByStatus(ExceptionStatus.PENDING, pageable);
        verify(exceptionRepository, never()).findAllWithDetails(any());
    }

    @Test
    @DisplayName("Should get all exceptions filtered by APPROVED status")
    void should_getAllExceptionsFilteredByApprovedStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.APPROVED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 1);

        ExceptionResponse response =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.APPROVED).build();

        when(exceptionRepository.findByStatus(ExceptionStatus.APPROVED, pageable))
                .thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception)).thenReturn(response);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(
                        ExceptionStatus.APPROVED, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(ExceptionStatus.APPROVED, result.getContent().get(0).getStatus());
        verify(exceptionRepository).findByStatus(ExceptionStatus.APPROVED, pageable);
    }

    @Test
    @DisplayName("Should get all exceptions filtered by REJECTED status")
    void should_getAllExceptionsFilteredByRejectedStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 1);

        ExceptionResponse response =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.REJECTED).build();

        when(exceptionRepository.findByStatus(ExceptionStatus.REJECTED, pageable))
                .thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception)).thenReturn(response);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(
                        ExceptionStatus.REJECTED, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(ExceptionStatus.REJECTED, result.getContent().get(0).getStatus());
        verify(exceptionRepository).findByStatus(ExceptionStatus.REJECTED, pageable);
    }

    @Test
    @DisplayName("Should get all exceptions filtered by CANCELLED status")
    void should_getAllExceptionsFilteredByCancelledStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        TutorAvailabilityException exception =
                TutorAvailabilityException.builder()
                        .id(1L)
                        .status(ExceptionStatus.CANCELLED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception);
        Page<TutorAvailabilityException> exceptionPage = new PageImpl<>(exceptions, pageable, 1);

        ExceptionResponse response =
                ExceptionResponse.builder().id(1L).status(ExceptionStatus.CANCELLED).build();

        when(exceptionRepository.findByStatus(ExceptionStatus.CANCELLED, pageable))
                .thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception)).thenReturn(response);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(
                        ExceptionStatus.CANCELLED, 0, 10, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(ExceptionStatus.CANCELLED, result.getContent().get(0).getStatus());
        verify(exceptionRepository).findByStatus(ExceptionStatus.CANCELLED, pageable);
    }

    @Test
    @DisplayName("Should return empty page when no exceptions exist")
    void should_returnEmptyPageWhenNoExceptionsExist() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TutorAvailabilityException> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(exceptionRepository.findAllWithDetails(pageable)).thenReturn(emptyPage);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(null, 0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(exceptionRepository).findAllWithDetails(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no exceptions with specific status exist")
    void should_returnEmptyPageWhenNoExceptionsWithSpecificStatusExist() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TutorAvailabilityException> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(exceptionRepository.findByStatus(ExceptionStatus.PENDING, pageable))
                .thenReturn(emptyPage);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(
                        ExceptionStatus.PENDING, 0, 10, null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(exceptionRepository).findByStatus(ExceptionStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("Should get all exceptions with pagination")
    void should_getAllExceptionsWithPagination() {
        // Given
        Pageable pageable =
                PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt")); // Page 1, size 5

        TutorAvailabilityException exception1 =
                TutorAvailabilityException.builder()
                        .id(6L)
                        .status(ExceptionStatus.APPROVED)
                        .build();
        TutorAvailabilityException exception2 =
                TutorAvailabilityException.builder()
                        .id(7L)
                        .status(ExceptionStatus.REJECTED)
                        .build();

        List<TutorAvailabilityException> exceptions = List.of(exception1, exception2);
        Page<TutorAvailabilityException> exceptionPage =
                new PageImpl<>(exceptions, pageable, 15); // 15 total

        ExceptionResponse response1 =
                ExceptionResponse.builder().id(6L).status(ExceptionStatus.APPROVED).build();
        ExceptionResponse response2 =
                ExceptionResponse.builder().id(7L).status(ExceptionStatus.REJECTED).build();

        when(exceptionRepository.findAllWithDetails(pageable)).thenReturn(exceptionPage);
        when(exceptionMapper.toResponse(exception1)).thenReturn(response1);
        when(exceptionMapper.toResponse(exception2)).thenReturn(response2);

        // When
        Page<ExceptionResponse> result =
                tutorAvailabilityExceptionService.getAllExceptions(null, 1, 5, null);

        // Then
        assertEquals(15, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages()); // 15 items / 5 per page = 3 pages
        assertEquals(1, result.getNumber()); // Current page number
        verify(exceptionRepository).findAllWithDetails(pageable);
    }
}
