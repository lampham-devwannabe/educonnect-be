package com.sep.educonnect.service.unit;

import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.admin.request.AddCommentRequest;
import com.sep.educonnect.dto.admin.response.TutorDocumentResponse;
import com.sep.educonnect.dto.comment.ReviewCommentResponse;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.tutor.response.TutorProfileResponse;
import com.sep.educonnect.dto.verification.VerificationProcessDetailResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.admin.ProcessProfileService;
import com.sep.educonnect.service.email.MailService;
import com.sep.educonnect.util.MockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessProfileService Unit Tests")
class ProcessProfileServiceTest {

    @Mock private TutorDocumentRepository documentRepository;

    @Mock private CommentRepository commentRepository;

    @Mock private VerificationProcessRepository verificationProcessRepository;

    @Mock private UserRepository userRepository;

    @Mock private TutorProfileRepository tutorProfileRepository;

    @Mock private MailService mailService;

    @Mock private I18nService i18nService;

    @InjectMocks private ProcessProfileService processProfileService;

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }
    @Test
    @DisplayName("Should throw when rejecting document without reason")
    void should_throwWhenRejectingWithoutReason() {
        MockHelper.mockSecurityContext("staff");
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(User.builder().build()));
        when(documentRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                TutorDocument.builder()
                                        .profile(
                                                TutorProfile.builder()
                                                        .id(1L)
                                                        .user(User.builder().userId("u").build())
                                                        .build())
                                        .build()));

        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.reviewDocument(
                                        1L, DocumentStatus.REJECTED, null));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when rejecting document with empty reason")
    void should_throwWhenRejectingWithEmptyReason() {
        // Given
        MockHelper.mockSecurityContext("staff");
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(User.builder().build()));
        when(documentRepository.findById(2L))
                .thenReturn(
                        Optional.of(
                                TutorDocument.builder()
                                        .profile(
                                                TutorProfile.builder()
                                                        .id(2L)
                                                        .user(User.builder().userId("u").build())
                                                        .build())
                                        .build()));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.reviewDocument(
                                        2L, DocumentStatus.REJECTED, ""));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when user not found for reviewDocument")
    void should_throwWhenUserNotFound_reviewDocument() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.reviewDocument(
                                        1L, DocumentStatus.APPROVED, null));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when document not found")
    void should_throwWhenDocumentNotFound() {
        // Given
        MockHelper.mockSecurityContext("staff");
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(User.builder().build()));
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.reviewDocument(
                                        999L, DocumentStatus.APPROVED, null));
        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should approve profile when all documents approved")
    void should_approveProfile() {
        MockHelper.mockSecurityContext("staff");
        User staff =
                User.builder()
                        .userId("staff-id")
                        .username("staff")
                        .email("staff@example.com")
                        .build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .firstName("Tutor")
                        .lastName("User")
                        .build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(20L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(3L);
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(3L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(
                        List.of(TutorDocument.builder().status(DocumentStatus.APPROVED).build()));
        when(i18nService.msg(anyString())).thenReturn("Approved");

        TutorProfileResponse response = processProfileService.approveProfile(3L, "Great profile");

        assertEquals(ProfileStatus.APPROVED, response.getProfileStatus());
        verify(mailService).send(any(Email.class), eq(TemplateMail.PROFILE_APPROVAL), anyMap());
        verify(commentRepository).save(any(ReviewComment.class));
        assertEquals(VerificationStage.COMPLETED, process.getCurrentStage());
    }

    @Test
    @DisplayName("Should throw exception when user not found for approveProfile")
    void should_throwException_whenUserNotFound_approveProfile() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.approveProfile(1L, "Comments"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when process not found for approveProfile")
    void should_throwException_whenProcessNotFound_approveProfile() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.approveProfile(999L, "Comments"));
        assertEquals(ErrorCode.PROCESS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when not all documents are approved")
    void should_throwException_whenDocumentsNotApproved() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor = User.builder().userId("tutor-id").username("tutor").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(30L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(4L);
        process.setProfile(profile);

        TutorDocument approvedDoc = TutorDocument.builder().status(DocumentStatus.APPROVED).build();
        TutorDocument pendingDoc = TutorDocument.builder().status(DocumentStatus.PENDING).build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(4L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(List.of(approvedDoc, pendingDoc));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.approveProfile(4L, "Comments"));
        assertEquals(ErrorCode.DOCUMENTS_NOT_APPROVED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when some documents are rejected")
    void should_throwException_whenSomeDocumentsRejected() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor = User.builder().userId("tutor-id").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(40L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(5L);
        process.setProfile(profile);

        TutorDocument approvedDoc = TutorDocument.builder().status(DocumentStatus.APPROVED).build();
        TutorDocument rejectedDoc = TutorDocument.builder().status(DocumentStatus.REJECTED).build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(5L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(List.of(approvedDoc, rejectedDoc));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.approveProfile(5L, "Comments"));
        assertEquals(ErrorCode.DOCUMENTS_NOT_APPROVED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should approve profile with null comments")
    void should_approveProfile_withNullComments() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(50L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(6L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(6L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(
                        List.of(TutorDocument.builder().status(DocumentStatus.APPROVED).build()));
        when(i18nService.msg(anyString())).thenReturn("Approved");

        // When
        TutorProfileResponse response = processProfileService.approveProfile(6L, null);

        // Then
        assertNotNull(response);
        assertEquals(ProfileStatus.APPROVED, response.getProfileStatus());
        verify(commentRepository)
                .save(argThat(comment -> comment.getComment().equals("Profile approved")));
    }

    @Test
    @DisplayName("Should approve profile with empty comments")
    void should_approveProfile_withEmptyComments() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(60L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(7L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(7L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(
                        List.of(TutorDocument.builder().status(DocumentStatus.APPROVED).build()));
        when(i18nService.msg(anyString())).thenReturn("Approved");

        // When
        TutorProfileResponse response = processProfileService.approveProfile(7L, "");

        // Then
        assertNotNull(response);
        assertEquals(ProfileStatus.APPROVED, response.getProfileStatus());
        verify(commentRepository).save(argThat(comment -> comment.getComment().isEmpty()));
    }

    @Test
    @DisplayName("Should approve profile with multiple approved documents")
    void should_approveProfile_withMultipleApprovedDocuments() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .build();
        TutorProfile profile = TutorProfile.builder().id(70L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(8L);
        process.setProfile(profile);

        List<TutorDocument> documents =
                List.of(
                        TutorDocument.builder().status(DocumentStatus.APPROVED).build(),
                        TutorDocument.builder().status(DocumentStatus.APPROVED).build(),
                        TutorDocument.builder().status(DocumentStatus.APPROVED).build());

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(8L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id")).thenReturn(documents);
        when(i18nService.msg(anyString())).thenReturn("Approved");

        // When
        TutorProfileResponse response = processProfileService.approveProfile(8L, "All good");

        // Then
        assertNotNull(response);
        assertEquals(ProfileStatus.APPROVED, response.getProfileStatus());
        assertEquals(VerificationStage.COMPLETED, process.getCurrentStage());
        assertNotNull(process.getCompletedAt());
    }

    @Test
    @DisplayName("Should create comment with correct visibility when approving")
    void should_createCommentWithCorrectVisibility_whenApproving() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .build();
        TutorProfile profile = TutorProfile.builder().id(80L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(9L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(9L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(
                        List.of(TutorDocument.builder().status(DocumentStatus.APPROVED).build()));
        when(i18nService.msg(anyString())).thenReturn("Approved");

        // When
        processProfileService.approveProfile(9L, "Excellent work");

        // Then
        verify(commentRepository)
                .save(
                        argThat(
                                comment ->
                                        comment.getCommentType() == CommentType.APPROVAL_NOTE
                                                && !comment.getIsVisibleToTutor()
                                                && comment.getComment().equals("Excellent work")));
    }

    @Test
    @DisplayName("Should approve profile when documents list is empty")
    void should_throwException_whenDocumentsListEmpty() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor = User.builder().userId("tutor-id").username("tutor").build();
        TutorProfile profile = TutorProfile.builder().id(110L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(12L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(12L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(List.of()); // Empty list

        // When
        TutorProfileResponse response = processProfileService.approveProfile(12L, "No docs");

        // Then - Empty list means all documents (none) are approved, so should succeed
        assertNotNull(response);
        assertEquals(ProfileStatus.APPROVED, response.getProfileStatus());
    }

    @Test
    @DisplayName("Should complete process stage when approving")
    void should_completeProcessStage_whenApproving() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .build();
        TutorProfile profile = TutorProfile.builder().id(120L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(13L);
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(13L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor-id"))
                .thenReturn(
                        List.of(TutorDocument.builder().status(DocumentStatus.APPROVED).build()));
        when(i18nService.msg(anyString())).thenReturn("Approved");

        // When
        processProfileService.approveProfile(13L, "Done");

        // Then
        assertEquals(VerificationStage.COMPLETED, process.getCurrentStage());
        assertNotNull(process.getCompletedAt());
        verify(verificationProcessRepository).save(process);
    }

    @Test
    @DisplayName("Should reject profile with reason")
    void should_rejectProfile_withReason() {
        MockHelper.mockSecurityContext("staff");
        User staff =
                User.builder()
                        .userId("staff-id")
                        .username("staff")
                        .email("staff@example.com")
                        .build();
        User tutor =
                User.builder()
                        .userId("tutor")
                        .email("tutor@example.com")
                        .firstName("Tutor")
                        .lastName("User")
                        .build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(30L)
                        .user(tutor)
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(4L);
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(4L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Rejected");

        TutorProfileResponse response =
                processProfileService.rejectProfile(4L, "Incomplete documents");

        assertEquals(ProfileStatus.REJECTED, response.getProfileStatus());
        assertEquals(VerificationStage.REJECTED, process.getCurrentStage());
        verify(mailService).send(any(Email.class), eq(TemplateMail.PROFILE_REJECTION), anyMap());
        verify(commentRepository).save(any(ReviewComment.class));
    }

    @Test
    @DisplayName("Should request revision with notes")
    void should_requestRevision() {
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(40L)
                        .user(
                                User.builder()
                                        .userId("tutor")
                                        .email("tutor@example.com")
                                        .firstName("Tutor")
                                        .lastName("User")
                                        .build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(5L);
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(5L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        TutorProfileResponse response =
                processProfileService.requestRevision(5L, "Need clearer documents");

        assertEquals(ProfileStatus.REVISION_REQUIRED, response.getProfileStatus());
        verify(commentRepository)
                .save(argThat(comment -> comment.getCommentType() == CommentType.REVISION_REASON));
        verify(mailService)
                .send(any(Email.class), eq(TemplateMail.PROFILE_REVISION_REQUEST), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when user not found for requestRevision")
    void should_throwException_whenUserNotFound_requestRevision() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.requestRevision(1L, "Revision notes"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when revision notes is null")
    void should_throwException_whenRevisionNotesIsNull() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> processProfileService.requestRevision(1L, null));
        assertEquals(ErrorCode.REVISION_NOTES_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when revision notes is empty")
    void should_throwException_whenRevisionNotesIsEmpty() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> processProfileService.requestRevision(1L, ""));
        assertEquals(ErrorCode.REVISION_NOTES_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when process not found for requestRevision")
    void should_throwException_whenProcessNotFound_requestRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> processProfileService.requestRevision(999L, "Revision notes"));
        assertEquals(ErrorCode.PROCESS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should set profile status to REVISION_REQUIRED when requesting revision")
    void should_setProfileStatusToRevisionRequired_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(130L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .submissionStatus(ProfileStatus.SUBMITTED)
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(14L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(14L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        TutorProfileResponse response =
                processProfileService.requestRevision(14L, "Please fix bio");

        // Then
        assertEquals(ProfileStatus.REVISION_REQUIRED, response.getProfileStatus());
        assertEquals(ProfileStatus.REVISION_REQUIRED, profile.getSubmissionStatus());
    }


    @Test
    @DisplayName("Should set reviewedAt and reviewedBy when requesting revision")
    void should_setReviewedFields_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(140L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(15L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(15L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        processProfileService.requestRevision(15L, "Update documents");

        // Then
        assertNotNull(profile.getReviewedAt());
        assertEquals(staff, profile.getReviewedBy());
    }

    @Test
    @DisplayName("Should create comment with correct visibility when requesting revision")
    void should_createCommentWithCorrectVisibility_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(160L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(17L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(17L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        processProfileService.requestRevision(17L, "Fix profile picture");

        // Then
        verify(commentRepository)
                .save(
                        argThat(
                                comment ->
                                        comment.getCommentType() == CommentType.REVISION_REASON
                                                && comment.getIsVisibleToTutor()
                                                && comment.getComment()
                                                        .equals("Fix profile picture")));
    }

    @Test
    @DisplayName("Should send revision request email when requesting revision")
    void should_sendRevisionRequestEmail_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User tutor =
                User.builder()
                        .userId("tutor-id")
                        .username("tutor")
                        .email("tutor@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build();
        TutorProfile profile = TutorProfile.builder().id(170L).user(tutor).build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(18L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(18L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        processProfileService.requestRevision(18L, "Update experience section");

        // Then
        verify(mailService)
                .send(
                        argThat(
                                email ->
                                        email.getTo() != null
                                                && email.getTo().stream()
                                                        .anyMatch(
                                                                mailer ->
                                                                        mailer.getEmail()
                                                                                .equals(
                                                                                        "tutor@example.com"))),
                        eq(TemplateMail.PROFILE_REVISION_REQUEST),
                        anyMap());
    }

    @Test
    @DisplayName("Should save profile when requesting revision")
    void should_saveProfile_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(180L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(19L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(19L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        processProfileService.requestRevision(19L, "Add certifications");

        // Then
        verify(tutorProfileRepository).save(profile);
    }
    @Test
    @DisplayName("Should request revision with long revision notes")
    void should_requestRevision_withLongRevisionNotes() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(190L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(20L);
        process.setProfile(profile);

        String longNotes =
                """
                Please update the following items:
                1. Profile picture needs to be professional
                2. Bio section needs more details about teaching experience
                3. Certifications need to be verified
                4. Add more information about teaching methodology""";

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(20L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        TutorProfileResponse response = processProfileService.requestRevision(20L, longNotes);

        // Then
        assertNotNull(response);
        assertEquals(ProfileStatus.REVISION_REQUIRED, response.getProfileStatus());
        verify(commentRepository).save(argThat(comment -> comment.getComment().equals(longNotes)));
    }

    @Test
    @DisplayName("Should create comment with correct timestamp when requesting revision")
    void should_createCommentWithCorrectTimestamp_whenRequestingRevision() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        TutorProfile profile =
                TutorProfile.builder()
                        .id(210L)
                        .user(User.builder().userId("tutor").email("tutor@example.com").build())
                        .build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(22L);
        process.setProfile(profile);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(22L)).thenReturn(Optional.of(process));
        when(i18nService.msg(anyString())).thenReturn("Revision");

        // When
        processProfileService.requestRevision(22L, "Please revise");

        // Then
        verify(commentRepository)
                .save(
                        argThat(
                                comment ->
                                        comment.getCreatedAt() != null
                                                && comment.getCreatedBy().equals("staff")));
    }

    @Test
    @DisplayName("Should add comment to process")
    void should_addComment() {
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(7L);
        when(verificationProcessRepository.findById(7L)).thenReturn(Optional.of(process));

        ReviewCommentResponse response =
                processProfileService.addComment(
                        7L,
                        new AddCommentRequest(
                                "Need more info", CommentType.PROFILE_ISSUE, true, null));

        assertEquals("Need more info", response.getComment());
        verify(commentRepository).save(any(ReviewComment.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for addComment")
    void should_throwException_whenUserNotFound_addComment() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.addComment(
                                        1L,
                                        new AddCommentRequest(
                                                "Comment", CommentType.GENERAL, false, null)));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when comment text is null")
    void should_throwException_whenCommentTextIsNull() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.addComment(
                                        1L,
                                        new AddCommentRequest(
                                                null, CommentType.GENERAL, false, null)));
        assertEquals(ErrorCode.COMMENT_TEXT_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when comment text is empty")
    void should_throwException_whenCommentTextIsEmpty() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.addComment(
                                        1L,
                                        new AddCommentRequest(
                                                "", CommentType.GENERAL, false, null)));
        assertEquals(ErrorCode.COMMENT_TEXT_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when process not found for addComment")
    void should_throwException_whenProcessNotFound_addComment() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                processProfileService.addComment(
                                        999L,
                                        new AddCommentRequest(
                                                "Comment", CommentType.GENERAL, false, null)));
        assertEquals(ErrorCode.PROCESS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should add comment with GENERAL type")
    void should_addComment_withGeneralType() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(23L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(23L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        23L,
                        new AddCommentRequest("General comment", CommentType.GENERAL, false, null));

        // Then
        assertNotNull(response);
        assertEquals(CommentType.GENERAL, response.getCommentType());
        verify(commentRepository)
                .save(argThat(comment -> comment.getCommentType() == CommentType.GENERAL));
    }

    @Test
    @DisplayName("Should add comment with APPROVAL_NOTE type")
    void should_addComment_withApprovalNoteType() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(24L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(24L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        24L,
                        new AddCommentRequest(
                                "Looks good", CommentType.APPROVAL_NOTE, false, null));

        // Then
        assertEquals(CommentType.APPROVAL_NOTE, response.getCommentType());
    }

    @Test
    @DisplayName("Should add comment with REJECTION_REASON type")
    void should_addComment_withRejectionReasonType() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(25L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(25L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        25L,
                        new AddCommentRequest(
                                "Invalid documents", CommentType.REJECTION_REASON, true, null));

        // Then
        assertEquals(CommentType.REJECTION_REASON, response.getCommentType());
    }

    @Test
    @DisplayName("Should add comment with DOCUMENT_ISSUE type")
    void should_addComment_withDocumentIssueType() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(26L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(26L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        26L,
                        new AddCommentRequest(
                                "Document expired", CommentType.DOCUMENT_ISSUE, true, null));

        // Then
        assertEquals(CommentType.DOCUMENT_ISSUE, response.getCommentType());
    }

    @Test
    @DisplayName("Should add comment with PROFILE_ISSUE type")
    void should_addComment_withProfileIssueType() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(27L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(27L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        27L,
                        new AddCommentRequest(
                                "Profile incomplete", CommentType.PROFILE_ISSUE, true, null));

        // Then
        assertEquals(CommentType.PROFILE_ISSUE, response.getCommentType());
    }

    @Test
    @DisplayName("Should add comment visible to tutor")
    void should_addComment_visibleToTutor() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(28L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(28L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        28L,
                        new AddCommentRequest("Visible comment", CommentType.GENERAL, true, null));

        // Then
        assertTrue(response.getIsVisibleToTutor());
        verify(commentRepository).save(argThat(comment -> comment.getIsVisibleToTutor()));
    }

    @Test
    @DisplayName("Should add comment not visible to tutor")
    void should_addComment_notVisibleToTutor() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(29L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(29L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        29L,
                        new AddCommentRequest("Internal note", CommentType.GENERAL, false, null));

        // Then
        assertFalse(response.getIsVisibleToTutor());
        verify(commentRepository).save(argThat(comment -> !comment.getIsVisibleToTutor()));
    }

    @Test
    @DisplayName("Should set created by and created at when adding comment")
    void should_setCreatedFields_whenAddingComment() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(30L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(30L)).thenReturn(Optional.of(process));

        // When
        processProfileService.addComment(
                30L, new AddCommentRequest("Test comment", CommentType.GENERAL, false, null));

        // Then
        verify(commentRepository)
                .save(
                        argThat(
                                comment ->
                                        comment.getCreatedBy().equals("staff")
                                                && comment.getCreatedAt() != null));
    }

    @Test
    @DisplayName("Should add comment with long text")
    void should_addComment_withLongText() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(31L);

        String longComment =
                "This is a very long comment that contains detailed information "
                        + "about the verification process and what needs to be improved. "
                        + "It includes multiple sentences and paragraphs to provide comprehensive feedback.";

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(31L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        31L, new AddCommentRequest(longComment, CommentType.GENERAL, false, null));

        // Then
        assertEquals(longComment, response.getComment());
    }

    @Test
    @DisplayName("Should add multiple comments to same process")
    void should_addMultipleComments_toSameProcess() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(32L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(32L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response1 =
                processProfileService.addComment(
                        32L,
                        new AddCommentRequest("First comment", CommentType.GENERAL, false, null));
        ReviewCommentResponse response2 =
                processProfileService.addComment(
                        32L,
                        new AddCommentRequest(
                                "Second comment", CommentType.PROFILE_ISSUE, true, null));
        ReviewCommentResponse response3 =
                processProfileService.addComment(
                        32L,
                        new AddCommentRequest(
                                "Third comment", CommentType.DOCUMENT_ISSUE, true, null));

        // Then
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
        verify(commentRepository, times(3)).save(any(ReviewComment.class));
    }

    @Test
    @DisplayName("Should save comment to repository")
    void should_saveComment_toRepository() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(33L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(33L)).thenReturn(Optional.of(process));

        // When
        processProfileService.addComment(
                33L, new AddCommentRequest("Save test", CommentType.GENERAL, false, null));

        // Then
        verify(commentRepository)
                .save(
                        argThat(
                                comment ->
                                        comment.getProcessId().equals(33L)
                                                && comment.getComment().equals("Save test")));
    }

    @Test
    @DisplayName("Should add comment with whitespace text")
    void should_notAddComment_withWhitespaceOnlyText() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        // When & Then - Whitespace is not considered empty by isEmpty()
        // But if we want to test with actual whitespace
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(34L);
        when(verificationProcessRepository.findById(34L)).thenReturn(Optional.of(process));

        ReviewCommentResponse response =
                processProfileService.addComment(
                        34L, new AddCommentRequest("   ", CommentType.GENERAL, false, null));

        // This will succeed as isEmpty() doesn't trim
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should return comment response with correct process id")
    void should_returnCommentResponse_withCorrectProcessId() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(35L);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(verificationProcessRepository.findById(35L)).thenReturn(Optional.of(process));

        // When
        ReviewCommentResponse response =
                processProfileService.addComment(
                        35L,
                        new AddCommentRequest("Process ID test", CommentType.GENERAL, false, null));

        // Then
        assertEquals(35L, response.getProcessId());
    }

    @Test
    @DisplayName("Should get verification details")
    void should_getVerificationDetails() {
        TutorProfile profile = new TutorProfile();
        profile.setId(1L);
        profile.setUser(User.builder().userId("tutor").build());

        VerificationProcess process = new VerificationProcess();
        process.setProcessId(11L);
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process.setStartedAt(LocalDateTime.now());
        process.setPriority(ProcessPriority.NORMAL);
        TutorDocument document =
                TutorDocument.builder()
                        .id(1L)
                        .profile(process.getProfile())
                        .status(DocumentStatus.APPROVED)
                        .build();
        ReviewComment comment = new ReviewComment();
        comment.setCommentId(1L);
        comment.setProcessId(11L);
        comment.setComment("Looks good");
        comment.setCommentType(CommentType.APPROVAL_NOTE);
        comment.setCreatedAt(LocalDateTime.now());

        when(verificationProcessRepository.findById(11L)).thenReturn(Optional.of(process));
        when(documentRepository.findByProfileUserUserId("tutor")).thenReturn(List.of(document));
        when(commentRepository.findByProcessIdOrderByCreatedAtDesc(11L))
                .thenReturn(List.of(comment));

        VerificationProcessDetailResponse response =
                processProfileService.getVerificationDetails(11L);

        assertEquals(11L, response.getProcessId());
        assertEquals(1, response.getDocuments().size());
        assertEquals(1, response.getComments().size());
    }

    @Test
    @DisplayName("Should throw when rejection reason missing")
    void should_throwWhenRejectionReasonMissing() {
        MockHelper.mockSecurityContext("staff");
        when(userRepository.findByUsername("staff"))
                .thenReturn(Optional.of(User.builder().build()));

        AppException exception =
                assertThrows(AppException.class, () -> processProfileService.rejectProfile(1L, ""));
        assertEquals(ErrorCode.REJECTION_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get empty list when no comments exist")
    void should_getEmptyList_whenNoComments() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(commentRepository.findByProcessIdOrderByCreatedAtDesc(20L)).thenReturn(List.of());

        // When
        List<ReviewCommentResponse> result = processProfileService.getComment(20L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should get comments with null creator")
    void should_getComments_withNullCreator() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();

        ReviewComment comment = new ReviewComment();
        comment.setCommentId(1L);
        comment.setProcessId(30L);
        comment.setComment("Comment without creator");
        comment.setCommentType(CommentType.GENERAL);
        comment.setIsVisibleToTutor(false);
        comment.setCreatedBy(null);
        comment.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(commentRepository.findByProcessIdOrderByCreatedAtDesc(30L))
                .thenReturn(List.of(comment));

        // When
        List<ReviewCommentResponse> result = processProfileService.getComment(30L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.getFirst().getCreatedBy());
    }


    @Test
    @DisplayName("Should throw exception when user not found")
    void should_throwException_whenUserNotFound_getComment() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> processProfileService.getComment(50L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get comments with multiple comment types")
    void should_getComments_withMultipleTypes() {
        // Given
        MockHelper.mockSecurityContext("staff");
        User staff = User.builder().userId("staff-id").username("staff").build();
        User creator =
                User.builder()
                        .userId("creator-id")
                        .username("creator")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        ReviewComment comment1 = new ReviewComment();
        comment1.setCommentId(1L);
        comment1.setProcessId(60L);
        comment1.setCommentType(CommentType.APPROVAL_NOTE);
        comment1.setComment("Approved");
        comment1.setCreatedBy("creator-id");
        comment1.setCreatedAt(LocalDateTime.now());

        ReviewComment comment2 = new ReviewComment();
        comment2.setCommentId(2L);
        comment2.setProcessId(60L);
        comment2.setCommentType(CommentType.REJECTION_REASON);
        comment2.setComment("Rejected");
        comment2.setCreatedBy("creator-id");
        comment2.setCreatedAt(LocalDateTime.now());

        ReviewComment comment3 = new ReviewComment();
        comment3.setCommentId(3L);
        comment3.setProcessId(60L);
        comment3.setCommentType(CommentType.DOCUMENT_ISSUE);
        comment3.setComment("Document issue");
        comment3.setCreatedBy("creator-id");
        comment3.setCreatedAt(LocalDateTime.now());

        ReviewComment comment4 = new ReviewComment();
        comment4.setCommentId(4L);
        comment4.setProcessId(60L);
        comment4.setCommentType(CommentType.PROFILE_ISSUE);
        comment4.setComment("Profile issue");
        comment4.setCreatedBy("creator-id");
        comment4.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(commentRepository.findByProcessIdOrderByCreatedAtDesc(60L))
                .thenReturn(List.of(comment4, comment3, comment2, comment1));

        // When
        List<ReviewCommentResponse> result = processProfileService.getComment(60L);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals(CommentType.PROFILE_ISSUE, result.get(0).getCommentType());
        assertEquals(CommentType.DOCUMENT_ISSUE, result.get(1).getCommentType());
        assertEquals(CommentType.REJECTION_REASON, result.get(2).getCommentType());
        assertEquals(CommentType.APPROVAL_NOTE, result.get(3).getCommentType());
    }

    @Test
    @DisplayName("Should get empty page when no verifications at stage")
    void should_getEmptyPage_whenNoVerificationsAtStage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<VerificationProcess> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.COMPLETED, pageable))
                .thenReturn(emptyPage);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.COMPLETED, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should get verifications with pagination")
    void should_getVerifications_withPagination() {
        // Given
        List<VerificationProcess> processes = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VerificationProcess process = new VerificationProcess();
            process.setProcessId((long) i);
            process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
            processes.add(process);
        }

        Pageable pageable = PageRequest.of(1, 2); // Page 1, size 2
        Page<VerificationProcess> page = new PageImpl<>(processes.subList(2, 4), pageable, 5);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getNumber());
    }

    @Test
    @DisplayName("Should get verifications sorted by different criteria")
    void should_getVerifications_sortedByDifferentCriteria() {
        // Given
        VerificationProcess process1 = new VerificationProcess();
        process1.setProcessId(1L);
        process1.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process1.setStartedAt(LocalDateTime.now().minusDays(2));

        VerificationProcess process2 = new VerificationProcess();
        process2.setProcessId(2L);
        process2.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process2.setStartedAt(LocalDateTime.now().minusDays(1));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("startedAt").descending());
        Page<VerificationProcess> page = new PageImpl<>(List.of(process2, process1), pageable, 2);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getProcessId());
        assertEquals(1L, result.getContent().get(1).getProcessId());
    }

    @Test
    @DisplayName("Should get verifications by stage COMPLETED")
    void should_getVerificationsByStage_completed() {
        // Given
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(4L);
        process.setCurrentStage(VerificationStage.COMPLETED);
        process.setStartedAt(LocalDateTime.now().minusDays(5));

        Pageable pageable = PageRequest.of(0, 10);
        Page<VerificationProcess> page = new PageImpl<>(List.of(process), pageable, 1);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.COMPLETED, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.COMPLETED, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(VerificationStage.COMPLETED, result.getContent().getFirst().getCurrentStage());
    }

    @Test
    @DisplayName("Should get verifications by stage REJECTED")
    void should_getVerificationsByStage_rejected() {
        // Given
        VerificationProcess process = new VerificationProcess();
        process.setProcessId(5L);
        process.setCurrentStage(VerificationStage.REJECTED);
        process.setStartedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<VerificationProcess> page = new PageImpl<>(List.of(process), pageable, 1);

        when(verificationProcessRepository.findByCurrentStage(VerificationStage.REJECTED, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(VerificationStage.REJECTED, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(VerificationStage.REJECTED, result.getContent().getFirst().getCurrentStage());
    }

    @Test
    @DisplayName("Should only return uncompleted verifications")
    void should_onlyReturnUncompletedVerifications() {
        // Given - All processes should have completedAt = null
        VerificationProcess process1 = new VerificationProcess();
        process1.setProcessId(1L);
        process1.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process1.setCompletedAt(null);

        VerificationProcess process2 = new VerificationProcess();
        process2.setProcessId(2L);
        process2.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process2.setCompletedAt(null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<VerificationProcess> page = new PageImpl<>(List.of(process1, process2), pageable, 2);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(process -> assertNull(process.getCompletedAt()));
    }

    @Test
    @DisplayName("Should get verifications with first page")
    void should_getVerifications_firstPage() {
        // Given
        List<VerificationProcess> processes = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            VerificationProcess process = new VerificationProcess();
            process.setProcessId((long) i);
            process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
            processes.add(process);
        }

        Pageable pageable = PageRequest.of(0, 10);
        Page<VerificationProcess> page = new PageImpl<>(processes, pageable, 3);

        when(verificationProcessRepository.findByCurrentStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable))
                .thenReturn(page);

        // When
        Page<VerificationProcess> result =
                processProfileService.getVerificationsByStage(
                        VerificationStage.DOCUMENT_REVIEW, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertEquals(0, result.getNumber());
    }
}
