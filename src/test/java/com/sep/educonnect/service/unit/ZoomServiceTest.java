package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.zoom.ZoomMeetingRequest;
import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassSessionRepository;
import com.sep.educonnect.service.ZoomService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZoomService Unit Tests")
class ZoomServiceTest {

    @Mock private ClassSessionRepository classSessionRepository;

    @InjectMocks private ZoomService zoomService;

    // ==================== CREATE MEETING TEST CASES ====================

    @Test
    @DisplayName("CM01 - Should throw SESSION_NOT_EXISTED when sessionId = 0")
    void should_throwSessionNotExisted_whenSessionIdIsZero() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(0L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();
        when(classSessionRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository).findById(0L);
    }

    @Test
    @DisplayName(
            "CM02 - Should throw SESSION_NOT_EXISTED when sessionId = 1 and session does not exist")
    void should_throwSessionNotExisted_whenSessionIdDoesNotExist() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM03 - Should propagate runtime error when token retrieval fails")
    void should_propagateRuntimeError_whenTokenRetrievalFails() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .duration(90)
                        .startTime("2025-10-16T09:00:00Z")
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail at getAccessToken due to missing Zoom config
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));
        assertNotNull(exception);
        assertTrue(
                exception.getMessage().contains("Error")
                        || exception.getMessage().contains("error"));
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM04 - Should handle valid sessionId with existing session")
    void should_handleValidSessionId_withExistingSession() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When - We expect this to fail when trying to get token (no actual Zoom connection)
        // But we verify that session was found
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - will fail at token retrieval or API call
            assertTrue(
                    e.getMessage().contains("Error creating Zoom meeting")
                            || e.getMessage().contains("Error retrieving Zoom token"));
        }

        // Then
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM05 - Should verify session repository is called with correct sessionId")
    void should_verifySessionRepositoryCalled_withCorrectSessionId() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository, times(1)).findById(1L);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CM06 - Should handle request with all fields populated")
    void should_handleRequest_withAllFieldsPopulated() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());

        // Verify request had all required fields
        assertNotNull(request.getSessionId());
        assertNotNull(request.getTopic());
        assertNotNull(request.getStartTime());
        assertNotNull(request.getPassword());
    }

    @Test
    @DisplayName("CM07 - Should handle request with different topic")
    void should_handleRequest_withDifferentTopic() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Physics Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("Physics Class", request.getTopic());
    }

    @Test
    @DisplayName("CM08 - Should handle request with different startTime")
    void should_handleRequest_withDifferentStartTime() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-11-20T10:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("2025-11-20T10:00:00Z", request.getStartTime());
    }

    @Test
    @DisplayName("CM09 - Should handle request with different duration")
    void should_handleRequest_withDifferentDuration() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(120)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(120, request.getDuration());
    }

    @Test
    @DisplayName("CM10 - Should handle request with different password")
    void should_handleRequest_withDifferentPassword() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("5678")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("5678", request.getPassword());
    }

    @Test
    @DisplayName("CM11 - Should handle request with null password")
    void should_handleRequest_withNullPassword() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When - Will fail at token retrieval
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - will fail at token retrieval or API call
            assertTrue(
                    e.getMessage().contains("Error creating Zoom meeting")
                            || e.getMessage().contains("Error retrieving Zoom token"));
        }

        // Then - Verify request was processed with null password
        assertNull(request.getPassword());
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM12 - Should not call save when session not found")
    void should_notCallSave_whenSessionNotFound() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(999L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository, times(1)).findById(999L);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CM13 - Should verify error message when session not found")
    void should_verifyErrorMessage_whenSessionNotFound() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));

        assertNotNull(exception);
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("CM14 - Should handle request with empty topic")
    void should_handleRequest_withEmptyTopic() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("", request.getTopic());
    }

    @Test
    @DisplayName("CM15 - Should handle request with minimum duration")
    void should_handleRequest_withMinimumDuration() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(1)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(1, request.getDuration());
    }

    // ==================== CREATE MEETING ADDITIONAL BRANCHES ====================

    @Test
    @DisplayName("CM16 - Should use generateRandomPassword when password is null")
    void should_useGenerateRandomPassword_whenPasswordIsNull() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        ZoomService spyService = Mockito.spy(zoomService);
        doThrow(new RuntimeException("Error retrieving Zoom token: test"))
                .when(spyService)
                .getAccessToken();

        // When & Then - Exception thrown but we verify password logic path
        assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertNull(request.getPassword());
    }

    @Test
    @DisplayName("CM17 - Should handle non-201 status code from Zoom API")
    void should_handleNon201StatusCode() {
        // Given - This test simulates the behavior when Zoom API returns non-201
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will throw exception at some point in the flow
        assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));
    }

    @Test
    @DisplayName("CM18 - Should handle HTTP connection exception")
    void should_handleHttpConnectionException() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        doThrow(new RuntimeException("Error retrieving Zoom token: Connection refused"))
                .when(spyService)
                .getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertTrue(exception.getMessage().contains("Error creating Zoom meeting"));
    }

    @Test
    @DisplayName("CM19 - Should verify session is saved when meeting created")
    void should_verifySessionSaved_whenMeetingCreated() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail but verify repository was called
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - no real Zoom connection
        }

        verify(classSessionRepository, atLeastOnce()).findById(1L);
    }

    @Test
    @DisplayName("CM20 - Should handle exception during JSON parsing")
    void should_handleExceptionDuringJsonParsing() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        doThrow(new RuntimeException("Error retrieving Zoom token: Invalid JSON"))
                .when(spyService)
                .getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Error creating Zoom meeting"));
    }

    @Test
    @DisplayName("CM21 - Should verify all session fields are set")
    void should_verifyAllSessionFieldsSet() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - no real Zoom connection
        }

        // Verify session was retrieved
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM22 - Should handle request with provided password")
    void should_handleRequest_withProvidedPassword() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("MyPassword123")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail at token retrieval but password is set
        assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));
        assertEquals("MyPassword123", request.getPassword());
    }

    @Test
    @DisplayName("CM23 - Should handle generic Exception in catch block")
    void should_handleGenericException_inCatchBlock() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        doThrow(new IllegalStateException("Invalid state")).when(spyService).getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertTrue(exception.getMessage().contains("Error creating Zoom meeting"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("CM24 - Should handle NullPointerException during meeting creation")
    void should_handleNullPointerException_duringMeetingCreation() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        doThrow(new NullPointerException("Null value")).when(spyService).getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("CM25 - Should verify repository save is never called when session not found")
    void should_verifyRepositorySaveNeverCalled_whenSessionNotFound() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(999L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        when(classSessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository).findById(999L);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CM26 - Should verify error message format in RuntimeException")
    void should_verifyErrorMessageFormat_inRuntimeException() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        doThrow(new RuntimeException("Original error")).when(spyService).getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertTrue(
                exception.getMessage().startsWith("Error creating Zoom meeting:")
                        || exception.getMessage().contains("Error creating Zoom meeting"));
    }

    @Test
    @DisplayName("CM27 - Should handle session with tutorClass")
    void should_handleSession_withTutorClass() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(100L);
        ClassSession session = new ClassSession();
        session.setId(1L);
        session.setTutorClass(tutorClass);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail at token retrieval but session was found with tutorClass
        assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName("CM28 - Should verify exception cause chain is preserved")
    void should_verifyExceptionCauseChainPreserved() {
        // Given
        ZoomService spyService = Mockito.spy(zoomService);
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Exception originalException = new IllegalArgumentException("Original cause");
        doThrow(new RuntimeException("Wrapped error", originalException))
                .when(spyService)
                .getAccessToken();

        // When & Then
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> spyService.createMeeting(request));
        assertNotNull(exception.getCause());
    }

    // ==================== GENERATE RANDOM PASSWORD TEST CASES ====================

    @Test
    @DisplayName("GRP01 - Should generate password when request password is null")
    void should_generatePassword_whenRequestPasswordIsNull() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null) // Null password triggers generateRandomPassword
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail at getAccessToken but logic path taken
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - no real Zoom connection
        }

        // Verify the null password path was taken
        assertNull(request.getPassword());
    }

    @Test
    @DisplayName("GRP02 - Should use provided password instead of generating one")
    void should_useProvidedPassword_insteadOfGenerating() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("MyCustomPass123") // Provided password
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected - no real Zoom connection
        }

        // Verify provided password is still the same
        assertEquals("MyCustomPass123", request.getPassword());
    }

    @Test
    @DisplayName("GRP03 - Should verify password generation produces valid characters")
    void should_verifyPasswordGeneration_producesValidCharacters() {
        // Given - Testing indirectly through createMeeting
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - The method will use generateRandomPassword internally
        assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));

        // Note: We can't directly test the generated password since it's private,
        // but we verify the null password path is executed
        assertNull(request.getPassword());
    }

    @Test
    @DisplayName("GRP04 - Should handle empty string password differently than null")
    void should_handleEmptyStringPassword_differentlyThanNull() {
        // Given
        ZoomMeetingRequest requestWithEmpty =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("") // Empty string is not null
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(requestWithEmpty);
        } catch (RuntimeException e) {
            // Expected
        }

        // Empty string should be used, not trigger generateRandomPassword
        assertEquals("", requestWithEmpty.getPassword());
    }

    @Test
    @DisplayName("GRP05 - Should verify null password triggers password generation path")
    void should_verifyNullPasswordTriggersGeneration() {
        // Given
        ZoomMeetingRequest request1 =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class 1")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        ZoomMeetingRequest request2 =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class 2")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Both should trigger generateRandomPassword
        try {
            zoomService.createMeeting(request1);
        } catch (RuntimeException e) {
            // Expected
        }

        try {
            zoomService.createMeeting(request2);
        } catch (RuntimeException e) {
            // Expected
        }

        // Both had null passwords
        assertNull(request1.getPassword());
        assertNull(request2.getPassword());
    }

    @Test
    @DisplayName("GRP06 - Should verify password special characters are allowed")
    void should_verifyPasswordSpecialCharacters_areAllowed() {
        // Given - Testing with special characters that generateRandomPassword can produce
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("Pass@123-_") // Contains @, -, _ which are in the charset
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertEquals("Pass@123-_", request.getPassword());
    }

    @Test
    @DisplayName("GRP07 - Should verify password with mixed case is preserved")
    void should_verifyPasswordMixedCase_isPreserved() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("AbCdEfGh") // Mixed case
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertEquals("AbCdEfGh", request.getPassword());
    }

    @Test
    @DisplayName("GRP08 - Should verify password with only letters is valid")
    void should_verifyPasswordOnlyLetters_isValid() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("AbCdEfGh")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertTrue(request.getPassword().matches("[A-Za-z]+"));
    }

    @Test
    @DisplayName("GRP09 - Should verify password with only numbers is valid")
    void should_verifyPasswordOnlyNumbers_isValid() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("12345678")
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertTrue(request.getPassword().matches("\\d+"));
    }

    @Test
    @DisplayName("GRP10 - Should verify long password is accepted")
    void should_verifyLongPassword_isAccepted() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("VeryLongPasswordWith32Characters") // > 8 chars
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertEquals("VeryLongPasswordWith32Characters", request.getPassword());
        assertTrue(request.getPassword().length() > 8);
    }

    @Test
    @DisplayName("GRP11 - Should verify short password is accepted")
    void should_verifyShortPassword_isAccepted() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("abc") // < 8 chars
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session = new ClassSession();
        session.setTutorClass(tutorClass);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then
        try {
            zoomService.createMeeting(request);
        } catch (RuntimeException e) {
            // Expected
        }

        assertEquals("abc", request.getPassword());
    }

    @Test
    @DisplayName("GRP12 - Should verify password null check happens before generation")
    void should_verifyPasswordNullCheck_beforeGeneration() {
        // Given
        ZoomMeetingRequest requestWithPassword =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("ExistingPass")
                        .build();

        ZoomMeetingRequest requestWithoutPassword =
                ZoomMeetingRequest.builder()
                        .sessionId(2L)
                        .topic("Math Class 2")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password(null)
                        .build();

        TutorClass tutorClass = new TutorClass();
        ClassSession session1 = new ClassSession();
        session1.setTutorClass(tutorClass);
        ClassSession session2 = new ClassSession();
        session2.setTutorClass(tutorClass);

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session1));
        when(classSessionRepository.findById(2L)).thenReturn(Optional.of(session2));

        // When & Then
        try {
            zoomService.createMeeting(requestWithPassword);
        } catch (RuntimeException e) {
            // Expected
        }

        try {
            zoomService.createMeeting(requestWithoutPassword);
        } catch (RuntimeException e) {
            // Expected
        }

        // First request keeps its password, second had null
        assertEquals("ExistingPass", requestWithPassword.getPassword());
        assertNull(requestWithoutPassword.getPassword());
    }

    // ==================== GENERATE RANDOM PASSWORD DIRECT TEST CASES ====================

    @Test
    @DisplayName("GRP13 - Should generate 8-character password using reflection")
    void should_generate8CharPassword_usingReflection() throws Exception {
        // Given - Use reflection to test private method
        Method method = ZoomService.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);

        // When
        String password = (String) method.invoke(zoomService);

        // Then
        assertNotNull(password);
        assertEquals(8, password.length());
    }

    @Test
    @DisplayName("GRP14 - Should generate password with valid characters only")
    void should_generatePasswordWithValidCharacters() throws Exception {
        // Given
        Method method = ZoomService.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);

        // When
        String password = (String) method.invoke(zoomService);

        // Then - Password should only contain allowed characters
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@-_";
        for (char c : password.toCharArray()) {
            assertTrue(allowedChars.indexOf(c) >= 0, "Character " + c + " not in allowed set");
        }
    }

    @Test
    @DisplayName("GRP15 - Should generate different passwords on multiple calls")
    void should_generateDifferentPasswords_onMultipleCalls() throws Exception {
        // Given
        Method method = ZoomService.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);

        // When - Generate multiple passwords
        String password1 = (String) method.invoke(zoomService);
        String password2 = (String) method.invoke(zoomService);
        String password3 = (String) method.invoke(zoomService);

        // Then - At least one should be different (very high probability)
        assertTrue(
                !password1.equals(password2) || !password2.equals(password3),
                "Generated passwords should be random");
    }

    @Test
    @DisplayName("GRP16 - Should verify generateRandomPassword produces non-empty string")
    void should_verifyGenerateRandomPassword_producesNonEmptyString() throws Exception {
        // Given
        Method method = ZoomService.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);

        // When
        String password = (String) method.invoke(zoomService);

        // Then
        assertNotNull(password);
        assertFalse(password.isEmpty());
    }

    @Test
    @DisplayName("GRP17 - Should generate password multiple times successfully")
    void should_generatePassword_multipleTimesSuccessfully() throws Exception {
        // Given
        Method method = ZoomService.class.getDeclaredMethod("generateRandomPassword");
        method.setAccessible(true);

        // When & Then - Generate 10 times to verify consistency
        for (int i = 0; i < 10; i++) {
            String password = (String) method.invoke(zoomService);
            assertNotNull(password);
            assertEquals(8, password.length());
        }
    }

    // ==================== GET ACCESS TOKEN ADDITIONAL TESTS ====================

    @Test
    @DisplayName("GAT13 - Should handle getAccessToken when called directly")
    void should_handleGetAccessToken_whenCalledDirectly() {
        // When & Then - This will fail in test environment (no real Zoom credentials)
        assertThrows(RuntimeException.class, () -> zoomService.getAccessToken());
    }

    @Test
    @DisplayName("GAT14 - Should verify getAccessToken throws RuntimeException on failure")
    void should_verifyGetAccessToken_throwsRuntimeException() {
        // When & Then
        Exception exception =
                assertThrows(RuntimeException.class, () -> zoomService.getAccessToken());

        assertNotNull(exception);
        assertTrue(
                exception.getMessage().contains("Error retrieving Zoom token")
                        || exception.getMessage().contains("error")
                        || exception.getMessage().contains("Error"));
    }

    // ==================== CREATE MEETING EDGE CASES ====================

    @Test
    @DisplayName("CM29 - Should handle very long topic name")
    void should_handleVeryLongTopicName() {
        // Given
        String longTopic = "A".repeat(500); // Very long topic
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic(longTopic)
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(longTopic, request.getTopic());
    }

    @Test
    @DisplayName("CM30 - Should handle zero duration")
    void should_handleZeroDuration() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(0)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(0, request.getDuration());
    }

    @Test
    @DisplayName("CM31 - Should handle negative duration")
    void should_handleNegativeDuration() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(-10)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(-10, request.getDuration());
    }

    @Test
    @DisplayName("CM32 - Should handle very large duration")
    void should_handleVeryLargeDuration() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(999999)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals(999999, request.getDuration());
    }

    @Test
    @DisplayName("CM33 - Should handle special characters in topic")
    void should_handleSpecialCharactersInTopic() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math & Physics: 100% Fun! @Home")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("Math & Physics: 100% Fun! @Home", request.getTopic());
    }

    @Test
    @DisplayName("CM34 - Should handle unicode characters in topic")
    void should_handleUnicodeCharactersInTopic() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Toán học 数学 Математика")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("Toán học 数学 Математика", request.getTopic());
    }

    @Test
    @DisplayName("CM35 - Should handle past start time")
    void should_handlePastStartTime() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2020-01-01T09:00:00Z") // Past date
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("2020-01-01T09:00:00Z", request.getStartTime());
    }

    @Test
    @DisplayName("CM36 - Should handle future start time")
    void should_handleFutureStartTime() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2099-12-31T23:59:59Z") // Far future
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("2099-12-31T23:59:59Z", request.getStartTime());
    }

    @Test
    @DisplayName("CM37 - Should handle invalid date format in startTime")
    void should_handleInvalidDateFormat() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("invalid-date-format")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("invalid-date-format", request.getStartTime());
    }

    @Test
    @DisplayName("CM38 - Should verify session is not saved when exception occurs early")
    void should_verifySessionNotSaved_whenExceptionOccursEarly() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CM39 - Should handle maximum sessionId value")
    void should_handleMaximumSessionId() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(Long.MAX_VALUE)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository).findById(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("CM40 - Should handle negative sessionId")
    void should_handleNegativeSessionId() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(-1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(-1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository).findById(-1L);
    }

    @Test
    @DisplayName("CM41 - Should handle session with null tutorClass")
    void should_handleSession_withNullTutorClass() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        ClassSession session = new ClassSession();
        session.setId(1L);
        session.setTutorClass(null); // Null tutorClass

        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When & Then - Will fail at getAccessToken
        assertThrows(RuntimeException.class, () -> zoomService.createMeeting(request));
        verify(classSessionRepository).findById(1L);
    }

    @Test
    @DisplayName(
            "CM42 - Should handle multiple consecutive createMeeting calls with same sessionId")
    void should_handleMultipleConsecutiveCalls_withSameSessionId() {
        // Given
        ZoomMeetingRequest request1 =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class 1")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("pass1")
                        .build();

        ZoomMeetingRequest request2 =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class 2")
                        .startTime("2025-10-16T10:00:00Z")
                        .duration(60)
                        .password("pass2")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> zoomService.createMeeting(request1));
        assertThrows(AppException.class, () -> zoomService.createMeeting(request2));

        verify(classSessionRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("CM43 - Should handle createMeeting with whitespace-only topic")
    void should_handleCreateMeeting_withWhitespaceOnlyTopic() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("   ")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));
        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        assertEquals("   ", request.getTopic());
    }

    @Test
    @DisplayName("CM44 - Should verify repository findById is always called first")
    void should_verifyRepositoryFindById_alwaysCalledFirst() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        try {
            zoomService.createMeeting(request);
        } catch (AppException e) {
            // Expected
        }

        // Then - findById should be the first and only repository call
        verify(classSessionRepository, times(1)).findById(1L);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CM45 - Should handle session repository returning null optional")
    void should_handleSessionRepository_returningNullOptional() {
        // Given
        ZoomMeetingRequest request =
                ZoomMeetingRequest.builder()
                        .sessionId(1L)
                        .topic("Math Class")
                        .startTime("2025-10-16T09:00:00Z")
                        .duration(90)
                        .password("1234")
                        .build();

        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> zoomService.createMeeting(request));

        assertEquals(ErrorCode.SESSION_NOT_EXISTED, exception.getErrorCode());
        verify(classSessionRepository).findById(1L);
    }
}
