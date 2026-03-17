package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.tutor.request.SubmitDocumentRequest;
import com.sep.educonnect.dto.tutor.request.UpdateDocumentRequest;
import com.sep.educonnect.entity.TutorDocument;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.enums.DocumentType;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.TutorDocumentRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.TutorDocumentService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorDocumentService Unit Tests")
class TutorDocumentServiceTest {

    private final User tutorUser =
            User.builder().userId("user-1").username("tutor").dob(LocalDate.of(1990, 1, 1)).build();
    private final TutorProfile tutorProfile =
            TutorProfile.builder().id(100L).user(tutorUser).build();
    @Mock private TutorProfileRepository tutorProfileRepository;
    @Mock private TutorDocumentRepository tutorDocumentRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TutorDocumentService tutorDocumentService;

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    // ===================== getSubmitDocument Tests =====================

    @Test
    @DisplayName("GSD01 - Should return empty list when user has no submitted documents")
    void GSD01_should_returnEmptyList_when_userHasNoDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1")).thenReturn(List.of());

        // When
        List<TutorDocument> documents = tutorDocumentService.getSubmitDocument();

        // Then
        assertNotNull(documents);
        assertEquals(0, documents.size());
        assertTrue(documents.isEmpty());
        verify(userRepository).findByUsername("tutor");
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }

    @Test
    @DisplayName("GSD02 - Should return submitted documents for current user")
    void GSD02_should_returnDocuments_when_userHasDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document1 =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .fileName("file-1")
                        .type(DocumentType.CV)
                        .build();
        TutorDocument document2 =
                TutorDocument.builder()
                        .id(2L)
                        .profile(tutorProfile)
                        .fileName("file-2")
                        .type(DocumentType.CERTIFICATE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document1, document2));

        // When
        List<TutorDocument> documents = tutorDocumentService.getSubmitDocument();

        // Then
        assertNotNull(documents);
        assertEquals(2, documents.size());
        assertEquals(1L, documents.get(0).getId());
        assertEquals(2L, documents.get(1).getId());
        assertEquals("file-1", documents.get(0).getFileName());
        assertEquals("file-2", documents.get(1).getFileName());
        verify(userRepository).findByUsername("tutor");
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }

    @Test
    @DisplayName("GSD03 - Should return single document when user has one document")
    void GSD03_should_returnSingleDocument_when_userHasOneDocument() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .fileName("cv-file")
                        .type(DocumentType.CV)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document));

        // When
        List<TutorDocument> documents = tutorDocumentService.getSubmitDocument();

        // Then
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(1L, documents.get(0).getId());
        assertEquals("cv-file", documents.get(0).getFileName());
        assertEquals(DocumentType.CV, documents.get(0).getType());
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }

    @Test
    @DisplayName("GSD04 - Should return multiple documents of different types")
    void GSD04_should_returnMultipleDocuments_withDifferentTypes() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument cv =
                TutorDocument.builder().id(1L).profile(tutorProfile).type(DocumentType.CV).build();
        TutorDocument certificate =
                TutorDocument.builder()
                        .id(2L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .build();
        TutorDocument degree =
                TutorDocument.builder()
                        .id(3L)
                        .profile(tutorProfile)
                        .type(DocumentType.DEGREE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cv, certificate, degree));

        // When
        List<TutorDocument> documents = tutorDocumentService.getSubmitDocument();

        // Then
        assertNotNull(documents);
        assertEquals(3, documents.size());
        assertTrue(documents.stream().anyMatch(d -> d.getType() == DocumentType.CV));
        assertTrue(documents.stream().anyMatch(d -> d.getType() == DocumentType.CERTIFICATE));
        assertTrue(documents.stream().anyMatch(d -> d.getType() == DocumentType.DEGREE));
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }

    @Test
    @DisplayName("GSD05 - Should throw USER_NOT_EXISTED when user not found")
    void GSD05_should_throwUserNotFound_inGetSubmitDocument() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorDocumentService.getSubmitDocument());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(userRepository).findByUsername("ghost");
        verify(tutorDocumentRepository, never()).findByProfileUserUserId(any());
    }

    @Test
    @DisplayName("GSD06 - Should return list with specific document statuses")
    void GSD06_should_returnDocuments_withVariousStatuses() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument pending =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .status(com.sep.educonnect.enums.DocumentStatus.PENDING)
                        .build();
        TutorDocument approved =
                TutorDocument.builder()
                        .id(2L)
                        .profile(tutorProfile)
                        .status(com.sep.educonnect.enums.DocumentStatus.APPROVED)
                        .build();
        TutorDocument rejected =
                TutorDocument.builder()
                        .id(3L)
                        .profile(tutorProfile)
                        .status(com.sep.educonnect.enums.DocumentStatus.REJECTED)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(pending, approved, rejected));

        // When
        List<TutorDocument> documents = tutorDocumentService.getSubmitDocument();

        // Then
        assertNotNull(documents);
        assertEquals(3, documents.size());
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }

    // ===================== submitTutorDocument Tests =====================

    @Test
    @DisplayName("STD01 - Should submit tutor document with valid fileId and CV type")
    void STD01_should_submitDocument_withValidFileIdAndCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertNotNull(result);
        assertEquals("file-123", result.getFileName());
        assertEquals(DocumentType.CV, result.getType());
        assertNotNull(result.getUploadedAt());
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, result.getStatus());
        assertEquals(tutorProfile, result.getProfile());
        verify(tutorDocumentRepository).save(any(TutorDocument.class));
    }

    @Test
    @DisplayName("STD02 - Should submit document with CERTIFICATE type")
    void STD02_should_submitDocument_withCertificateType() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder()
                        .fileId("cert-file-456")
                        .documentType("CERTIFICATE")
                        .build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertNotNull(result);
        assertEquals("cert-file-456", result.getFileName());
        assertEquals(DocumentType.CERTIFICATE, result.getType());
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, result.getStatus());
        verify(tutorDocumentRepository).save(any(TutorDocument.class));
    }

    @Test
    @DisplayName("STD03 - Should submit document with DEGREE type")
    void STD03_should_submitDocument_withDegreeType() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("degree-789").documentType("DEGREE").build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertNotNull(result);
        assertEquals("degree-789", result.getFileName());
        assertEquals(DocumentType.DEGREE, result.getType());
        verify(tutorDocumentRepository).save(any(TutorDocument.class));
    }

    @Test
    @DisplayName("STD04 - Should throw INVALID_DOCUMENT_TYPE when documentType is AD")
    void STD04_should_throwInvalidDocumentType_withAD() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("AD").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("STD05 - Should throw INVALID_DOCUMENT_TYPE when documentType is invalid string")
    void STD05_should_throwInvalidDocumentType_withInvalidString() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder()
                        .fileId("file-123")
                        .documentType("INVALID_TYPE")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD06 - Should throw INVALID_DOCUMENT_TYPE when documentType is null")
    void STD06_should_throwInvalidDocumentType_withNull() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType(null).build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD07 - Should throw INVALID_DOCUMENT_TYPE when documentType is blank")
    void STD07_should_throwInvalidDocumentType_withBlank() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("   ").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD08 - Should throw INVALID_DOCUMENT_TYPE when documentType is empty")
    void STD08_should_throwInvalidDocumentType_withEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD09 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is null")
    void STD09_should_throwMissingDocuments_withNullFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId(null).documentType("CV").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("STD10 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is empty")
    void STD10_should_throwMissingDocuments_withEmptyFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("").documentType("CV").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD11 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is blank")
    void STD11_should_throwMissingDocuments_withBlankFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("   ").documentType("CV").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("STD12 - Should throw USER_NOT_EXISTED when user not found")
    void STD12_should_throwUserNotFound_inSubmit() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("STD13 - Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found")
    void STD13_should_throwProfileNotFound_inSubmit() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1")).thenReturn(Optional.empty());

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.submitTutorDocument(request));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("STD14 - Should set uploadedAt to current time")
    void STD14_should_setUploadedAtToCurrentTime() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        LocalDateTime beforeSubmit = LocalDateTime.now().minusSeconds(1);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertNotNull(result.getUploadedAt());
        assertTrue(result.getUploadedAt().isAfter(beforeSubmit));
        assertTrue(result.getUploadedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("STD15 - Should set status to PENDING when submitting")
    void STD15_should_setStatusToPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("STD16 - Should associate document with tutor profile")
    void STD16_should_associateWithTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitDocumentRequest request =
                SubmitDocumentRequest.builder().fileId("file-123").documentType("CV").build();

        // When
        TutorDocument result = tutorDocumentService.submitTutorDocument(request);

        // Then
        assertNotNull(result.getProfile());
        assertEquals(tutorProfile, result.getProfile());
        assertEquals(100L, result.getProfile().getId());
    }

    // ===================== updateTutorDocument Tests =====================

    @Test
    @DisplayName("UTD01 - Should update document with valid documentId, fileId, and CV type")
    void UTD01_should_updateDocument_withValidInputs() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .type(DocumentType.CV)
                        .fileName("old-file")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("new-file-123")
                        .documentType("CV")
                        .build();

        // When
        TutorDocument updated = tutorDocumentService.updateTutorDocument(request);

        // Then
        assertNotNull(updated);
        assertEquals("new-file-123", updated.getFileName());
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, updated.getStatus());
        verify(tutorDocumentRepository).save(document);
    }

    @Test
    @DisplayName("UTD02 - Should update document with CERTIFICATE type")
    void UTD02_should_updateDocument_withCertificateType() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .fileName("old-cert")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("new-cert-456")
                        .documentType("CERTIFICATE")
                        .build();

        // When
        TutorDocument updated = tutorDocumentService.updateTutorDocument(request);

        // Then
        assertNotNull(updated);
        assertEquals("new-cert-456", updated.getFileName());
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, updated.getStatus());
    }

    @Test
    @DisplayName("UTD03 - Should update document with DEGREE type")
    void UTD03_should_updateDocument_withDegreeType() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .type(DocumentType.DEGREE)
                        .fileName("old-degree")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("new-degree-789")
                        .documentType("DEGREE")
                        .build();

        // When
        TutorDocument updated = tutorDocumentService.updateTutorDocument(request);

        // Then
        assertNotNull(updated);
        assertEquals("new-degree-789", updated.getFileName());
    }

    @Test
    @DisplayName("UTD04 - Should throw DOCUMENT_NOT_FOUND when documentId is 0")
    void UTD04_should_throwDocumentNotFound_withZeroId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(0L)).thenReturn(Optional.empty());

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(0L)
                        .fileId("file-123")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD05 - Should throw DOCUMENT_NOT_FOUND when documentId does not exist")
    void UTD05_should_throwDocumentNotFound_withNonExistentId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(999L)
                        .fileId("file-123")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD06 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is null")
    void UTD06_should_throwMissingDocuments_withNullFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId(null)
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD07 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is empty")
    void UTD07_should_throwMissingDocuments_withEmptyFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD08 - Should throw MISSING_REQUIRED_DOCUMENTS when fileId is blank")
    void UTD08_should_throwMissingDocuments_withBlankFileId() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("   ")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD09 - Should throw INVALID_DOCUMENT_TYPE when documentType is null")
    void UTD09_should_throwInvalidDocumentType_withNull() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType(null)
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD10 - Should throw INVALID_DOCUMENT_TYPE when documentType is blank")
    void UTD10_should_throwInvalidDocumentType_withBlank() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("   ")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD11 - Should throw INVALID_DOCUMENT_TYPE when documentType is empty")
    void UTD11_should_throwInvalidDocumentType_withEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD12 - Should throw INVALID_DOCUMENT_TYPE when documentType is AD")
    void UTD12_should_throwInvalidDocumentType_withAD() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("AD")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD13 - Should throw INVALID_DOCUMENT_TYPE when documentType is invalid")
    void UTD13_should_throwInvalidDocumentType_withInvalidString() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("INVALID_TYPE")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.INVALID_DOCUMENT_TYPE, exception.getErrorCode());
    }

    @Test
    @DisplayName("UTD14 - Should throw USER_NOT_EXISTED when user not found")
    void UTD14_should_throwUserNotFound_inUpdate() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD15 - Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found")
    void UTD15_should_throwProfileNotFound_inUpdate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.empty());

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("file-123")
                        .documentType("CV")
                        .build();

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorDocumentService.updateTutorDocument(request));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD16 - Should reset status to PENDING when updating document")
    void UTD16_should_resetStatusToPending() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(tutorProfile)
                        .type(DocumentType.CV)
                        .fileName("old-file")
                        .status(com.sep.educonnect.enums.DocumentStatus.APPROVED)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId(
                        "user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(tutorDocumentRepository.save(any(TutorDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateDocumentRequest request =
                UpdateDocumentRequest.builder()
                        .documentId(1L)
                        .fileId("new-file")
                        .documentType("CV")
                        .build();

        // When
        TutorDocument updated = tutorDocumentService.updateTutorDocument(request);

        // Then
        assertEquals(com.sep.educonnect.enums.DocumentStatus.PENDING, updated.getStatus());
    }
}
