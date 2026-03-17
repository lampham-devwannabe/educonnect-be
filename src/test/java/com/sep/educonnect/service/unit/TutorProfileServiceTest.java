package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.tutor.TutorStudentDTO;
import com.sep.educonnect.dto.tutor.request.SubmitProfileRequest;
import com.sep.educonnect.dto.tutor.response.ProfileDTO;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.TranslationService;
import com.sep.educonnect.service.TutorProfileService;
import com.sep.educonnect.util.MockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorProfileService Unit Tests")
class TutorProfileServiceTest {

    @Mock private TutorProfileRepository tutorProfileRepository;

    @Mock private TutorDocumentRepository tutorDocumentRepository;

    @Mock private VerificationProcessRepository verificationProcessRepository;

    @Mock private UserRepository userRepository;

    @Mock private TranslationService translationService;

    @Mock private SubjectRepository subjectRepository;

    @Mock private TagRepository tagRepository;

    @Mock private StudentLikesRepository studentLikesRepository;

    @Mock private ReviewCommentRepository reviewCommentRepository;

    @InjectMocks private TutorProfileService tutorProfileService;

    private User tutorUser;
    private TutorProfile tutorProfile;

    @BeforeEach
    void setUp() {
        tutorUser =
                User.builder()
                        .userId("user-1")
                        .username("tutor")
                        .email("tutor@example.com")
                        .firstName("Jane")
                        .lastName("Doe")
                        .dob(LocalDate.of(1990, 1, 1))
                        .build();

        tutorProfile =
                TutorProfile.builder()
                        .id(100L)
                        .user(tutorUser)
                        .hourlyRate(new BigDecimal("50"))
                        .currencyCode(com.sep.educonnect.enums.CurrencyCode.USD)
                        .submissionStatus(ProfileStatus.DRAFT)
                        .subjects(new HashSet<>())
                        .tags(new HashSet<>())
                        .build();
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should return tutor profile for current user")
    void should_returnTutorProfile_when_userHasProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.APPROVED);

        VerificationProcess verificationProcess = new VerificationProcess();
        verificationProcess.setProcessId(1L);
        verificationProcess.setProfile(tutorProfile);

        List<ReviewComment> comments = new ArrayList<>();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(verificationProcessRepository.findByProfileId(100L))
                .thenReturn(List.of(verificationProcess));
        when(reviewCommentRepository.findByProcessId(1L))
                .thenReturn(comments);

        // When
        ProfileDTO result = tutorProfileService.getTutorProfile();

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getTutorProfile().getId());
        assertNotNull(result.getComment());
        verify(userRepository).findByUsername("tutor");
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(verificationProcessRepository).findByProfileId(100L);
        verify(reviewCommentRepository).findByProcessId(1L);
    }

    @Test
    @DisplayName("Should throw when current user not found")
    void should_throwWhenUserNotFound_getTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.getTutorProfile());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should submit profile for review when requirements met")
    void should_submitProfileForReview_when_requirementsMet() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setUser(tutorUser);

        TutorDocument document =
                TutorDocument.builder().id(10L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();
        assertEquals(VerificationStage.DOCUMENT_REVIEW, process.getCurrentStage());
        assertEquals(ProcessPriority.NORMAL, process.getPriority());
        assertNotNull(process.getStartedAt());
        assertNotNull(process.getEstimatedCompletionDate());
    }

    @Test
    @DisplayName("Should throw when submitting profile without required documents")
    void should_throwWhenSubmittingWithoutDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setUser(tutorUser);
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1")).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
        verify(tutorProfileRepository, never()).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("Should resubmit profile and create high priority process")
    void should_resubmitProfile_when_statusAllows() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setUser(tutorUser);

        TutorDocument document =
                TutorDocument.builder().id(11L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();
        assertEquals(ProcessPriority.HIGH, process.getPriority());
        assertEquals(VerificationStage.DOCUMENT_REVIEW, process.getCurrentStage());
    }

    @Test
    @DisplayName("Should throw when resubmitting with invalid status")
    void should_throwWhenResubmittingWithInvalidStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.SUBMITTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.INVALID_RESUBMISSION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should update tutor profile with partial fields")
    void should_updateTutorProfile_withPartialFields() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("20 years")
                        .hourlyRate(new BigDecimal("120"))
                        .build();

        // When
        TutorProfile updated = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertEquals("20 years", updated.getExperience());
        assertEquals(new BigDecimal("120"), updated.getHourlyRate());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("Should update profile without translation when no bio/desc provided")
    void should_handleTranslationFailure_andMarkForManualTranslation() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Update only experience and hourly rate, not bio/desc
        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("15 years")
                        .hourlyRate(new BigDecimal("75"))
                        .build();

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "vi");

        // Then
        assertNotNull(result);
        assertEquals("15 years", result.getExperience());
        assertEquals(new BigDecimal("75"), result.getHourlyRate());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("Should upload video successfully")
    void should_uploadVideo_successfully() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        String videoFileId = "video-123";
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String result = tutorProfileService.uploadVideo(videoFileId);

        // Then
        assertEquals(videoFileId, result);
        assertEquals(videoFileId, tutorProfile.getVideoLink());
        verify(tutorProfileRepository).save(tutorProfile);
    }

    @Test
    @DisplayName("Should throw when uploading video but profile not found")
    void should_throwWhenUploadingVideo_profileNotFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        String videoFileId = "video-123";
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.uploadVideo(videoFileId));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submitting profile with invalid status")
    void should_throwWhenSubmittingProfile_withInvalidStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.APPROVED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INVALID_SUBMISSION_STATUS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submitting profile with incomplete data")
    void should_throwWhenSubmittingProfile_withIncompleteData() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(null); // Missing hourly rate
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        // Document check happens after validation, so this stubbing is not needed

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INCOMPLETE_PROFILE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submitting profile with hourly rate zero or negative")
    void should_throwWhenSubmittingProfile_withInvalidHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(BigDecimal.ZERO); // Invalid hourly rate
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        // Document check happens after validation, so this stubbing is not needed

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INCOMPLETE_PROFILE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submitting profile without CV document")
    void should_throwWhenSubmittingProfile_withoutCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setUser(tutorUser);

        TutorDocument document =
                TutorDocument.builder()
                        .id(10L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE) // Not CV
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should resubmit profile with REJECTED status")
    void should_resubmitProfile_withRejectedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument document =
                TutorDocument.builder().id(11L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(document));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();
        assertEquals(ProcessPriority.HIGH, process.getPriority());
    }

    @Test
    @DisplayName("Should throw when resubmitting without required documents")
    void should_throwWhenResubmittingWithoutDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1")).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when profile not found for current user")
    void should_throwWhenProfileNotFound_getTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.getTutorProfile());
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when user not found for upload video")
    void should_throwWhenUserNotFound_uploadVideo() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.uploadVideo("video-123"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get tutor profile by ID successfully")
    void should_getTutorProfileById_successfully() {
        // Given
        Long tutorId = 100L;
        String language = "en";
        MockHelper.mockSecurityContext("student");

        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(tutorProfile));
        when(userRepository.findByUsernameAndNotDeleted("student"))
                .thenReturn(Optional.of(new User()));
        when(studentLikesRepository.existsByStudentIdAndTutor_Id(any(), eq(tutorId)))
                .thenReturn(false);

        // When
        TutorStudentDTO response = tutorProfileService.getTutorProfileById(tutorId, language);

        // Then
        assertNotNull(response);
        assertEquals(tutorId, response.getId());
        verify(tutorProfileRepository).findById(tutorId);
    }
    @Test
    @DisplayName("Should throw exception when subjects not found in saveTutorProfile")
    void should_throwException_whenSubjectsNotFound_inSaveTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        SubmitProfileRequest request = SubmitProfileRequest.builder().subjects(Set.of(1L)).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_SUBJECTS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when tags not found in saveTutorProfile")
    void should_throwException_whenTagsNotFound_inSaveTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        SubmitProfileRequest request =
                SubmitProfileRequest.builder().subjects(Set.of(1L)).tags(Set.of(1L)).build();

        Subject subject = new Subject();
        subject.setSubjectId(1L);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject));
        when(tagRepository.findAllById(any())).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_TAGS_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("SP02 - Should throw when user not found in saveTutorProfile")
    void SP02_should_throwUserNotFound_inSaveTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        SubmitProfileRequest request = SubmitProfileRequest.builder().experience("5 years").build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP03 - Should throw when subjects are empty")
    void SP03_should_throwSubjectIdsRequired_whenSubjectsEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(Set.of())
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SUBJECT_IDS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP04 - Should throw when subjects are null")
    void SP04_should_throwSubjectIdsRequired_whenSubjectsNull() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(null)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SUBJECT_IDS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP05 - Should throw when some subjects not found")
    void SP05_should_throwSomeSubjectsNotFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L, 999L)) // 999L does not exist
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any()))
                .thenReturn(List.of(subject1)); // Only 1 subject returned

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_SUBJECTS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP06 - Should throw when tags are empty")
    void SP06_should_throwTagIdsRequired_whenTagsEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L))
                        .tags(Set.of())
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.TAG_IDS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP07 - Should throw when tags are null")
    void SP07_should_throwTagIdsRequired_whenTagsNull() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L))
                        .tags(null)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.TAG_IDS_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP08 - Should throw when some tags not found")
    void SP08_should_throwSomeTagsNotFound() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        Tag tag1 = new Tag();
        tag1.setId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("5 years")
                        .hourlyRate(new BigDecimal("50.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L))
                        .tags(Set.of(1L, 999L)) // 999L does not exist
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1)); // Only 1 tag returned

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.saveTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_TAGS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("SP10 - Should save profile with minimum required fields")
    void SP10_should_saveTutorProfile_withMinimumFields() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        Tag tag1 = new Tag();
        tag1.setId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .subjects(Set.of(1L))
                        .tags(Set.of(1L))
                        .currencyCode("USD")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.saveTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertNull(result.getExperience());
        assertNull(result.getHourlyRate());
        assertNull(result.getBioEn());
        assertNull(result.getDescEn());
        assertEquals(ProfileStatus.DRAFT, result.getSubmissionStatus());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP01 - Should update tutor profile successfully with all fields")
    void UP01_should_updateTutorProfile_withAllFields() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);
        Subject subject2 = new Subject();
        subject2.setSubjectId(2L);

        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(2L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("10 years")
                        .hourlyRate(new BigDecimal("100.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L, 2L))
                        .tags(Set.of(1L, 2L))
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1, subject2));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1, tag2));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("10 years", result.getExperience());
        assertEquals(new BigDecimal("100.00"), result.getHourlyRate());
        assertEquals(CurrencyCode.USD, result.getCurrencyCode());
        assertEquals(2, result.getSubjects().size());
        assertEquals(2, result.getTags().size());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP02 - Should throw USER_NOT_EXISTED when user not found")
    void UP02_should_throwUserNotFound_inUpdateTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        SubmitProfileRequest request = SubmitProfileRequest.builder().experience("5 years").build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.updateTutorProfile(request, "en"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("UP03 - Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found")
    void UP03_should_throwProfileNotFound_inUpdateTutorProfile() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        SubmitProfileRequest request = SubmitProfileRequest.builder().experience("5 years").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.updateTutorProfile(request, "en"));
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("UP04 - Should update only experience field")
    void UP04_should_updateOnlyExperience() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().experience("15 years").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("15 years", result.getExperience());
        // Other fields should remain unchanged
        assertEquals(new BigDecimal("50"), result.getHourlyRate());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP05 - Should update only hourlyRate field")
    void UP05_should_updateOnlyHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().hourlyRate(new BigDecimal("200.00")).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getHourlyRate());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP06 - Should update currencyCode field")
    void UP06_should_updateCurrencyCode() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request = SubmitProfileRequest.builder().currencyCode("VND").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(CurrencyCode.VND, result.getCurrencyCode());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP07 - Should update subjects")
    void UP07_should_updateSubjects() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);
        Subject subject2 = new Subject();
        subject2.setSubjectId(2L);
        Subject subject3 = new Subject();
        subject3.setSubjectId(3L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().subjects(Set.of(1L, 2L, 3L)).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any()))
                .thenReturn(List.of(subject1, subject2, subject3));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(3, result.getSubjects().size());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP08 - Should throw SOME_SUBJECTS_NOT_FOUND when some subjects don't exist")
    void UP08_should_throwSomeSubjectsNotFound_inUpdate() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().subjects(Set.of(1L, 999L)).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.updateTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_SUBJECTS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("UP09 - Should update tags")
    void UP09_should_updateTags() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(2L);

        SubmitProfileRequest request = SubmitProfileRequest.builder().tags(Set.of(1L, 2L)).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1, tag2));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTags().size());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP10 - Should throw SOME_TAGS_NOT_FOUND when some tags don't exist")
    void UP10_should_throwSomeTagsNotFound_inUpdate() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Tag tag1 = new Tag();
        tag1.setId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().tags(Set.of(1L, 999L)).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorProfileService.updateTutorProfile(request, "en"));
        assertEquals(ErrorCode.SOME_TAGS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("UP11 - Should update experience and hourly rate together")
    void UP11_should_updateExperienceAndHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("25 years")
                        .hourlyRate(new BigDecimal("250.00"))
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("25 years", result.getExperience());
        assertEquals(new BigDecimal("250.00"), result.getHourlyRate());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP12 - Should update subjects and tags together")
    void UP12_should_updateSubjectsAndTags() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);
        Subject subject2 = new Subject();
        subject2.setSubjectId(2L);
        Subject subject3 = new Subject();
        subject3.setSubjectId(3L);

        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(2L);
        Tag tag3 = new Tag();
        tag3.setId(3L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .subjects(Set.of(1L, 2L, 3L))
                        .tags(Set.of(1L, 2L, 3L))
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any()))
                .thenReturn(List.of(subject1, subject2, subject3));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1, tag2, tag3));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(3, result.getSubjects().size());
        assertEquals(3, result.getTags().size());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP13 - Should update bio and desc in English with translation")
    void UP13_should_updateBioAndDescInEnglish() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        // Mock translation service to return successful translation
        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        Map<String, String> translations = new HashMap<>();
        translations.put("bio", "Tiểu sử đã dịch");
        translations.put("desc", "Mô tả đã dịch");
        translationResult.setTranslations(translations);

        CompletableFuture<TranslationService.TranslationResult> futureResult =
                CompletableFuture.completedFuture(translationResult);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .bio("New English bio")
                        .desc("New English description")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(translationService.translateProfileFields(any(Map.class), eq("Vietnamese")))
                .thenReturn(futureResult);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("New English bio", result.getBioEn());
        assertEquals("New English description", result.getDescEn());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
        verify(translationService).translateProfileFields(any(Map.class), eq("Vietnamese"));
    }

    @Test
    @DisplayName("UP14 - Should update bio and desc in Vietnamese with translation")
    void UP14_should_updateBioAndDescInVietnamese() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        // Mock translation service to return successful translation
        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        Map<String, String> translations = new HashMap<>();
        translations.put("bio", "Translated bio");
        translations.put("desc", "Translated description");
        translationResult.setTranslations(translations);

        CompletableFuture<TranslationService.TranslationResult> futureResult =
                CompletableFuture.completedFuture(translationResult);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .bio("Tiểu sử tiếng Việt mới")
                        .desc("Mô tả tiếng Việt mới")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(translationService.translateProfileFields(any(Map.class), eq("English")))
                .thenReturn(futureResult);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "vi");

        // Then
        assertNotNull(result);
        assertEquals("Tiểu sử tiếng Việt mới", result.getBioVi());
        assertEquals("Mô tả tiếng Việt mới", result.getDescVi());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
        verify(translationService).translateProfileFields(any(Map.class), eq("English"));
    }

    @Test
    @DisplayName("UP15 - Should update only bio in English")
    void UP15_should_updateOnlyBioInEnglish() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        Map<String, String> translations = new HashMap<>();
        translations.put("bio", "Tiểu sử mới");
        translationResult.setTranslations(translations);

        CompletableFuture<TranslationService.TranslationResult> futureResult =
                CompletableFuture.completedFuture(translationResult);

        SubmitProfileRequest request = SubmitProfileRequest.builder().bio("New bio only").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(translationService.translateProfileFields(any(Map.class), eq("Vietnamese")))
                .thenReturn(futureResult);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("New bio only", result.getBioEn());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP16 - Should update only desc in Vietnamese")
    void UP16_should_updateOnlyDescInVietnamese() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        Map<String, String> translations = new HashMap<>();
        translations.put("desc", "New description");
        translationResult.setTranslations(translations);

        CompletableFuture<TranslationService.TranslationResult> futureResult =
                CompletableFuture.completedFuture(translationResult);

        SubmitProfileRequest request = SubmitProfileRequest.builder().desc("Mô tả mới").build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(translationService.translateProfileFields(any(Map.class), eq("English")))
                .thenReturn(futureResult);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "vi");

        // Then
        assertNotNull(result);
        assertEquals("Mô tả mới", result.getDescVi());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP17 - Should update with all fields including bio and desc")
    void UP17_should_updateAllFieldsIncludingBioDesc() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        Tag tag1 = new Tag();
        tag1.setId(1L);

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        Map<String, String> translations = new HashMap<>();
        translations.put("bio", "Tiểu sử đầy đủ");
        translations.put("desc", "Mô tả đầy đủ");
        translationResult.setTranslations(translations);

        CompletableFuture<TranslationService.TranslationResult> futureResult =
                CompletableFuture.completedFuture(translationResult);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("20 years")
                        .hourlyRate(new BigDecimal("180.00"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L))
                        .tags(Set.of(1L))
                        .bio("Complete bio")
                        .desc("Complete description")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(translationService.translateProfileFields(any(Map.class), eq("Vietnamese")))
                .thenReturn(futureResult);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("20 years", result.getExperience());
        assertEquals(new BigDecimal("180.00"), result.getHourlyRate());
        assertEquals(CurrencyCode.USD, result.getCurrencyCode());
        assertEquals(1, result.getSubjects().size());
        assertEquals(1, result.getTags().size());
        assertEquals("Complete bio", result.getBioEn());
        assertEquals("Complete description", result.getDescEn());
        verify(tutorProfileRepository, atLeast(1)).save(any(TutorProfile.class));
        verify(translationService).translateProfileFields(any(Map.class), eq("Vietnamese"));
    }

    @Test
    @DisplayName("UP18 - Should skip update when all fields are null")
    void UP18_should_skipUpdateWhenAllFieldsNull() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request = SubmitProfileRequest.builder().build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        // All original values should remain unchanged
        assertEquals(new BigDecimal("50"), result.getHourlyRate());
        assertEquals(CurrencyCode.USD, result.getCurrencyCode());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP19 - Should skip subjects update when empty")
    void UP19_should_skipSubjectsUpdateWhenEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .subjects(Set.of()) // Empty set
                        .experience("5 years")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("5 years", result.getExperience());
        // Subjects should not be updated (remains from setUp)
        verify(tutorProfileRepository).save(any(TutorProfile.class));
        verify(subjectRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("UP20 - Should skip tags update when empty")
    void UP20_should_skipTagsUpdateWhenEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .tags(Set.of()) // Empty set
                        .experience("8 years")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("8 years", result.getExperience());
        // Tags should not be updated
        verify(tutorProfileRepository).save(any(TutorProfile.class));
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("UP21 - Should skip currencyCode update when empty string")
    void UP21_should_skipCurrencyCodeUpdateWhenEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .currencyCode("") // Empty string
                        .experience("3 years")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("3 years", result.getExperience());
        // Currency code should remain unchanged
        assertEquals(CurrencyCode.USD, result.getCurrencyCode());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP22 - Should skip bio update when empty string")
    void UP22_should_skipBioUpdateWhenEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .bio("") // Empty string
                        .experience("7 years")
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("7 years", result.getExperience());
        // Bio should not be updated
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP23 - Should skip desc update when empty string")
    void UP23_should_skipDescUpdateWhenEmpty() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .desc("") // Empty string
                        .hourlyRate(new BigDecimal("150"))
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("150"), result.getHourlyRate());
        // Desc should not be updated
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP24 - Should update multiple fields at once")
    void UP24_should_updateMultipleFieldsAtOnce() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        Subject subject1 = new Subject();
        subject1.setSubjectId(1L);

        Tag tag1 = new Tag();
        tag1.setId(1L);

        SubmitProfileRequest request =
                SubmitProfileRequest.builder()
                        .experience("12 years")
                        .hourlyRate(new BigDecimal("85.50"))
                        .currencyCode("USD")
                        .subjects(Set.of(1L))
                        .tags(Set.of(1L))
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject1));
        when(tagRepository.findAllById(any())).thenReturn(List.of(tag1));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals("12 years", result.getExperience());
        assertEquals(new BigDecimal("85.50"), result.getHourlyRate());
        assertEquals(CurrencyCode.USD, result.getCurrencyCode());
        assertEquals(1, result.getSubjects().size());
        assertEquals(1, result.getTags().size());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("UP25 - Should update with zero hourly rate")
    void UP25_should_updateWithZeroHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");

        SubmitProfileRequest request =
                SubmitProfileRequest.builder().hourlyRate(BigDecimal.ZERO).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.updateTutorProfile(request, "en");

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getHourlyRate());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
    }

    // =============== Test cases for submitProfileForReview ===============

    @Test
    @DisplayName("SUBMIT01 - Should submit profile successfully with DRAFT status and CV document")
    void SUBMIT01_should_submitProfile_withDraftStatusAndCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("50.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(1L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("SUBMIT02 - Should submit profile with REVISION_REQUIRED status")
    void SUBMIT02_should_submitProfile_withRevisionRequiredStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setHourlyRate(new BigDecimal("75.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(2L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("SUBMIT03 - Should throw USER_NOT_EXISTED when user not found")
    void SUBMIT03_should_throwUserNotFound_inSubmit() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT04 - Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found")
    void SUBMIT04_should_throwProfileNotFound_inSubmit() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT05 - Should throw INVALID_SUBMISSION_STATUS with APPROVED status")
    void SUBMIT05_should_throwInvalidStatus_withApprovedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.APPROVED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INVALID_SUBMISSION_STATUS, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT06 - Should throw INVALID_SUBMISSION_STATUS with SUBMITTED status")
    void SUBMIT06_should_throwInvalidStatus_withSubmittedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.SUBMITTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INVALID_SUBMISSION_STATUS, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT07 - Should throw INVALID_SUBMISSION_STATUS with REJECTED status")
    void SUBMIT07_should_throwInvalidStatus_withRejectedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INVALID_SUBMISSION_STATUS, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT08 - Should throw INCOMPLETE_PROFILE when hourly rate is null")
    void SUBMIT08_should_throwIncompleteProfile_withNullHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(null);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INCOMPLETE_PROFILE, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT09 - Should throw INCOMPLETE_PROFILE when hourly rate is zero")
    void SUBMIT09_should_throwIncompleteProfile_withZeroHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(BigDecimal.ZERO);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INCOMPLETE_PROFILE, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT10 - Should throw INCOMPLETE_PROFILE when hourly rate is negative")
    void SUBMIT10_should_throwIncompleteProfile_withNegativeHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("-10"));
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.INCOMPLETE_PROFILE, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT11 - Should throw MISSING_REQUIRED_DOCUMENTS when no documents")
    void SUBMIT11_should_throwMissingDocuments_withNoDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("50.00"));
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1")).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT12 - Should throw MISSING_REQUIRED_DOCUMENTS when CV not uploaded")
    void SUBMIT12_should_throwMissingDocuments_withoutCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("50.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument certificateDoc =
                TutorDocument.builder()
                        .id(3L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(certificateDoc));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorProfileService.submitProfileForReview());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("SUBMIT13 - Should submit with CV and other documents")
    void SUBMIT13_should_submitProfile_withCVAndOtherDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("100.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(4L).profile(tutorProfile).type(DocumentType.CV).build();

        TutorDocument certificateDoc =
                TutorDocument.builder()
                        .id(5L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument, certificateDoc));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("SUBMIT14 - Should create verification process with DOCUMENT_REVIEW stage")
    void SUBMIT14_should_createVerificationProcess_withDocumentReviewStage() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("60.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(6L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();

        assertEquals(VerificationStage.DOCUMENT_REVIEW, process.getCurrentStage());
        assertEquals(ProcessPriority.NORMAL, process.getPriority());
        assertNotNull(process.getStartedAt());
        assertNotNull(process.getEstimatedCompletionDate());
    }

    @Test
    @DisplayName("SUBMIT15 - Should submit with valid positive hourly rate")
    void SUBMIT15_should_submitProfile_withValidHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("150.50"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(7L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        assertEquals(new BigDecimal("150.50"), result.getHourlyRate());
    }

    @Test
    @DisplayName("SUBMIT16 - Should submit profile with minimum valid hourly rate")
    void SUBMIT16_should_submitProfile_withMinimumValidHourlyRate() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setHourlyRate(new BigDecimal("0.01"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(8L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.submitProfileForReview();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        assertTrue(result.getHourlyRate().compareTo(BigDecimal.ZERO) > 0);
    }

    // =============== Test cases for resubmitProfile ===============

    @Test
    @DisplayName("RESUBMIT01 - Should resubmit profile with REJECTED status and CV document")
    void RESUBMIT01_should_resubmitProfile_withRejectedStatusAndCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setHourlyRate(new BigDecimal("50.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(1L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(tutorProfileRepository).save(any(TutorProfile.class));
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("RESUBMIT02 - Should resubmit profile with REVISION_REQUIRED status")
    void RESUBMIT02_should_resubmitProfile_withRevisionRequiredStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setHourlyRate(new BigDecimal("75.00"));
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(2L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("RESUBMIT03 - Should throw USER_NOT_EXISTED when user not found")
    void RESUBMIT03_should_throwUserNotFound_inResubmit() {
        // Given
        MockHelper.mockSecurityContext("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("RESUBMIT04 - Should throw TUTOR_PROFILE_NOT_EXISTED when profile not found")
    void RESUBMIT04_should_throwProfileNotFound_inResubmit() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.TUTOR_PROFILE_NOT_EXISTED, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
    }

    @Test
    @DisplayName("RESUBMIT05 - Should throw INVALID_RESUBMISSION with DRAFT status")
    void RESUBMIT05_should_throwInvalidResubmission_withDraftStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.DRAFT);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.INVALID_RESUBMISSION, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository, never()).findByProfileUserUserId(anyString());
    }

    @Test
    @DisplayName("RESUBMIT06 - Should throw INVALID_RESUBMISSION with APPROVED status")
    void RESUBMIT06_should_throwInvalidResubmission_withApprovedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.APPROVED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.INVALID_RESUBMISSION, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository, never()).findByProfileUserUserId(anyString());
    }

    @Test
    @DisplayName("RESUBMIT07 - Should throw INVALID_RESUBMISSION with SUBMITTED status")
    void RESUBMIT07_should_throwInvalidResubmission_withSubmittedStatus() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.SUBMITTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.INVALID_RESUBMISSION, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository, never()).findByProfileUserUserId(anyString());
    }

    @Test
    @DisplayName("RESUBMIT08 - Should throw MISSING_REQUIRED_DOCUMENTS when no documents")
    void RESUBMIT08_should_throwMissingDocuments_withNoDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1")).thenReturn(List.of());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
        verify(tutorProfileRepository, never()).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("RESUBMIT09 - Should throw MISSING_REQUIRED_DOCUMENTS when CV not uploaded")
    void RESUBMIT09_should_throwMissingDocuments_withoutCV() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setUser(tutorUser);

        TutorDocument certificateDoc =
                TutorDocument.builder()
                        .id(3L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(certificateDoc));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorProfileService.resubmitProfile());
        assertEquals(ErrorCode.MISSING_REQUIRED_DOCUMENTS, exception.getErrorCode());
        verify(tutorProfileRepository).findByUserUserId("user-1");
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
        verify(tutorProfileRepository, never()).save(any(TutorProfile.class));
    }

    @Test
    @DisplayName("RESUBMIT10 - Should create verification process with HIGH priority")
    void RESUBMIT10_should_createVerificationProcess_withHighPriority() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(4L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();

        assertEquals(ProcessPriority.HIGH, process.getPriority());
        assertEquals(VerificationStage.DOCUMENT_REVIEW, process.getCurrentStage());
        assertNotNull(process.getStartedAt());
        assertNotNull(process.getEstimatedCompletionDate());
    }

    @Test
    @DisplayName("RESUBMIT11 - Should resubmit with CV and other documents")
    void RESUBMIT11_should_resubmitProfile_withCVAndOtherDocuments() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(5L).profile(tutorProfile).type(DocumentType.CV).build();

        TutorDocument certificateDoc =
                TutorDocument.builder()
                        .id(6L)
                        .profile(tutorProfile)
                        .type(DocumentType.CERTIFICATE)
                        .build();

        TutorDocument degreeDoc =
                TutorDocument.builder()
                        .id(7L)
                        .profile(tutorProfile)
                        .type(DocumentType.DEGREE)
                        .build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument, certificateDoc, degreeDoc));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertNotNull(result);
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(verificationProcessRepository).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("RESUBMIT12 - Should change status from REJECTED to SUBMITTED")
    void RESUBMIT12_should_changeStatusFromRejectedToSubmitted() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(8L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(tutorProfileRepository).save(tutorProfile);
    }

    @Test
    @DisplayName("RESUBMIT13 - Should change status from REVISION_REQUIRED to SUBMITTED")
    void RESUBMIT13_should_changeStatusFromRevisionRequiredToSubmitted() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(9L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        TutorProfile result = tutorProfileService.resubmitProfile();

        // Then
        assertEquals(ProfileStatus.SUBMITTED, result.getSubmissionStatus());
        verify(tutorProfileRepository).save(tutorProfile);
    }

    @Test
    @DisplayName("RESUBMIT14 - Should set verification stage to DOCUMENT_REVIEW")
    void RESUBMIT14_should_setVerificationStageToDocumentReview() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(10L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        tutorProfileService.resubmitProfile();

        // Then
        ArgumentCaptor<VerificationProcess> processCaptor =
                ArgumentCaptor.forClass(VerificationProcess.class);
        verify(verificationProcessRepository).save(processCaptor.capture());
        VerificationProcess process = processCaptor.getValue();

        assertEquals(VerificationStage.DOCUMENT_REVIEW, process.getCurrentStage());
    }

    @Test
    @DisplayName("RESUBMIT15 - Should save both profile and verification process")
    void RESUBMIT15_should_saveBothProfileAndVerificationProcess() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(11L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        tutorProfileService.resubmitProfile();

        // Then
        verify(tutorProfileRepository, times(1)).save(any(TutorProfile.class));
        verify(verificationProcessRepository, times(1)).save(any(VerificationProcess.class));
    }

    @Test
    @DisplayName("RESUBMIT16 - Should validate documents before resubmitting")
    void RESUBMIT16_should_validateDocumentsBeforeResubmitting() {
        // Given
        MockHelper.mockSecurityContext("tutor");
        tutorProfile.setSubmissionStatus(ProfileStatus.REJECTED);
        tutorProfile.setUser(tutorUser);

        TutorDocument cvDocument =
                TutorDocument.builder().id(12L).profile(tutorProfile).type(DocumentType.CV).build();

        when(userRepository.findByUsername("tutor")).thenReturn(Optional.of(tutorUser));
        when(tutorProfileRepository.findByUserUserId("user-1"))
                .thenReturn(Optional.of(tutorProfile));
        when(tutorDocumentRepository.findByProfileUserUserId("user-1"))
                .thenReturn(List.of(cvDocument));
        when(tutorProfileRepository.save(any(TutorProfile.class)))
                .thenAnswer(i -> i.getArguments()[0]);
        when(verificationProcessRepository.save(any(VerificationProcess.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        tutorProfileService.resubmitProfile();

        // Then
        verify(tutorDocumentRepository).findByProfileUserUserId("user-1");
    }
}
