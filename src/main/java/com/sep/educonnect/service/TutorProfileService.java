package com.sep.educonnect.service;

import com.sep.educonnect.dto.home.TopTutorResponse;
import com.sep.educonnect.dto.subject.SubjectDTO;
import com.sep.educonnect.dto.subject.response.SubjectResponse;
import com.sep.educonnect.dto.tutor.TutorStudentDTO;
import com.sep.educonnect.dto.tutor.request.SubmitProfileRequest;
import com.sep.educonnect.dto.tutor.response.ProfileDTO;
import com.sep.educonnect.dto.tutor.response.TutorStudentResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorProfileService {

    final TutorProfileRepository tutorProfileRepository;
    final TutorDocumentRepository tutorDocumentRepository;
    final VerificationProcessRepository verificationProcessRepository;
    final UserRepository userRepository;
    final TranslationService translationService;
    final SubjectRepository subjectRepository;
    final TagRepository tagRepository;
    final StudentLikesRepository studentLikesRepository;
    final ReviewCommentRepository reviewCommentRepository;

    public ProfileDTO getTutorProfile() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        ProfileDTO profileDTO = ProfileDTO.builder()
                .tutorProfile(tutorProfile)
                .build();

        List<VerificationProcess> verificationProcessList = verificationProcessRepository.findByProfileId(tutorProfile.getId());
        List<ReviewComment> comment = new ArrayList<>();
        for (VerificationProcess i : verificationProcessList) {
            comment.addAll(reviewCommentRepository.findByProcessId(i.getProcessId()));

        }
        profileDTO.setComment(comment);


        return profileDTO;
    }

    public TutorStudentDTO getTutorProfileById(Long tutorId, String language) {
        TutorProfile profile =
                tutorProfileRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        return mapToResponse(profile, language);
    }

    public TutorProfile saveTutorProfile(SubmitProfileRequest request, String locale) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Set<Subject> subjects = validateAndGetSubjects(request.getSubjects());
        Set<Tag> tags = validateAndGetTags(request.getTags());

        TutorProfile tutorProfile =
                TutorProfile.builder()
                        .experience(request.getExperience())
                        .hourlyRate(request.getHourlyRate())
                        .currencyCode(CurrencyCode.valueOf(request.getCurrencyCode()))
                        .subjects(subjects)
                        .tags(tags)
                        .submissionStatus(ProfileStatus.DRAFT)
                        .build();
        Map<String, String> fieldsToTranslate = new HashMap<>();

        if ("vi".equals(locale)) {
            if (request.getBio() != null && !request.getBio().isEmpty()) {
                tutorProfile.setBioVi(request.getBio());
                fieldsToTranslate.put("bio", request.getBio());
            }
            if (request.getDesc() != null && !request.getDesc().isEmpty()) {
                tutorProfile.setDescVi(request.getDesc());
                fieldsToTranslate.put("desc", request.getDesc());
            }
        } else {
            if (request.getBio() != null && !request.getBio().isEmpty()) {
                tutorProfile.setBioEn(request.getBio());
                fieldsToTranslate.put("bio", request.getBio());
            }
            if (request.getDesc() != null && !request.getDesc().isEmpty()) {
                tutorProfile.setDescEn(request.getDesc());
                fieldsToTranslate.put("desc", request.getDesc());
            }
        }

        tutorProfile.setUser(user);

        TutorProfile savedProfile = tutorProfileRepository.save(tutorProfile);

        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = "vi".equals(locale) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture =
                    translationService.translateProfileFields(fieldsToTranslate, targetLanguage);

            translationFuture
                    .thenAccept(
                            result -> {
                                if (result.isSuccess()) {
                                    log.info("Processing successful translation result");
                                    if ("vi".equals(locale)) {
                                        if (result.getTranslation("bio") != null) {
                                            savedProfile.setBioEn(result.getTranslation("bio"));
                                        }
                                        if (result.getTranslation("desc") != null) {
                                            savedProfile.setDescEn(result.getTranslation("desc"));
                                        }
                                    } else {
                                        if (result.getTranslation("bio") != null) {
                                            savedProfile.setBioVi(result.getTranslation("bio"));
                                        }
                                        if (result.getTranslation("desc") != null) {
                                            savedProfile.setDescVi(result.getTranslation("desc"));
                                        }
                                    }

                                    tutorProfileRepository.save(savedProfile);
                                }
                            })
                    .exceptionally(
                            ex -> {
                                log.error(
                                        "Translation failed for tutor profile fields: {}",
                                        fieldsToTranslate.keySet(),
                                        ex);
                                markForManualTranslation(savedProfile.getId(), fieldsToTranslate);

                                return null;
                            });
        }

        return savedProfile;
    }

    public TutorProfile updateTutorProfile(SubmitProfileRequest request, String language) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var tutorProfile =
                tutorProfileRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        if (request.getExperience() != null) {
            tutorProfile.setExperience(request.getExperience());
        }

        if (request.getHourlyRate() != null) {
            tutorProfile.setHourlyRate(request.getHourlyRate());
        }

        if (request.getCurrencyCode() != null && !request.getCurrencyCode().isEmpty()) {
            tutorProfile.setCurrencyCode(CurrencyCode.valueOf(request.getCurrencyCode()));
        }

        if (request.getSubjects() != null && !request.getSubjects().isEmpty()) {
            Set<Subject> subjects = validateAndGetSubjects(request.getSubjects());
            tutorProfile.setSubjects(subjects);
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = validateAndGetTags(request.getTags());
            tutorProfile.setTags(tags);
        }

        Map<String, String> fieldsToTranslate = new HashMap<>();

        if ("vi".equals(language)) {
            if (request.getBio() != null && !request.getBio().isEmpty()) {
                tutorProfile.setBioVi(request.getBio());
                fieldsToTranslate.put("bio", request.getBio());
            }
            if (request.getDesc() != null && !request.getDesc().isEmpty()) {
                tutorProfile.setDescVi(request.getDesc());
                fieldsToTranslate.put("desc", request.getDesc());
            }
        } else {
            if (request.getBio() != null && !request.getBio().isEmpty()) {
                tutorProfile.setBioEn(request.getBio());
                fieldsToTranslate.put("bio", request.getBio());
            }
            if (request.getDesc() != null && !request.getDesc().isEmpty()) {
                tutorProfile.setDescEn(request.getDesc());
                fieldsToTranslate.put("desc", request.getDesc());
            }
        }

        // Save the profile immediately without waiting for translation
        TutorProfile savedProfile = tutorProfileRepository.save(tutorProfile);

        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = "vi".equals(language) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture =
                    translationService.translateProfileFields(fieldsToTranslate, targetLanguage);

            translationFuture
                    .thenAccept(
                            result -> {
                                if (result.isSuccess()) {
                                    log.info("Processing successful translation result");
                                    if ("vi".equals(language)) {
                                        if (result.getTranslation("bio") != null) {
                                            savedProfile.setBioEn(result.getTranslation("bio"));
                                        }
                                        if (result.getTranslation("desc") != null) {
                                            savedProfile.setDescEn(result.getTranslation("desc"));
                                        }
                                    } else {
                                        if (result.getTranslation("bio") != null) {
                                            savedProfile.setBioVi(result.getTranslation("bio"));
                                        }
                                        if (result.getTranslation("desc") != null) {
                                            savedProfile.setDescVi(result.getTranslation("desc"));
                                        }
                                    }

                                    tutorProfileRepository.save(savedProfile);
                                }
                            })
                    .exceptionally(
                            ex -> {
                                log.error(
                                        "Translation failed for tutor profile fields: {}",
                                        fieldsToTranslate.keySet(),
                                        ex);
                                markForManualTranslation(savedProfile.getId(), fieldsToTranslate);

                                return null;
                            });
        }

        return savedProfile;
    }

    private void markForManualTranslation(Long profileId, Map<String, String> failedFields) {
        tutorProfileRepository
                .findById(profileId)
                .ifPresent(
                        profile -> {
                            // Mark failed fields with a special indicator
                            if (failedFields.containsKey("bio")) {
                                if (profile.getBioEn() == null) {
                                    profile.setBioEn(
                                            "[TRANSLATION_FAILED] " + failedFields.get("bio"));
                                } else {
                                    profile.setBioVi(
                                            "[TRANSLATION_FAILED] " + failedFields.get("bio"));
                                }
                            }
                            if (failedFields.containsKey("desc")) {
                                if (profile.getDescEn() == null) {
                                    profile.setDescEn(
                                            "[TRANSLATION_FAILED] " + failedFields.get("desc"));
                                } else {
                                    profile.setDescVi(
                                            "[TRANSLATION_FAILED] " + failedFields.get("desc"));
                                }
                            }
                            tutorProfileRepository.save(profile);
                            log.warn("Profile {} marked for manual translation review", profileId);
                        });
    }

    public String uploadVideo(String file) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var tutorProfile =
                tutorProfileRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        tutorProfile.setVideoLink(file);
        tutorProfileRepository.save(tutorProfile);
        return file;
    }

    public TutorProfile submitProfileForReview() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var profile =
                tutorProfileRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        log.info("Submission status:   " + profile.getSubmissionStatus());

        // Validate that profile is in correct state
        if (profile.getSubmissionStatus() != ProfileStatus.DRAFT
                && profile.getSubmissionStatus() != ProfileStatus.REVISION_REQUIRED) {
            throw new AppException(ErrorCode.INVALID_SUBMISSION_STATUS);
        }

        // Validate profile completeness
        validateProfileForSubmission(profile);

        // Check if all required documents are uploaded
        List<TutorDocument> documents =
                tutorDocumentRepository.findByProfileUserUserId(profile.getUser().getUserId());
        if (documents.isEmpty() || !hasRequiredDocuments(documents)) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_DOCUMENTS);
        }

        // Update profile status
        profile.setSubmissionStatus(ProfileStatus.SUBMITTED);

        // Create verification process
        VerificationProcess process = new VerificationProcess();
        process.setProfile(profile);
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process.setStartedAt(LocalDateTime.now());
        process.setPriority(ProcessPriority.NORMAL);
        process.setEstimatedCompletionDate(calculateEstimatedCompletion(ProcessPriority.NORMAL));

        tutorProfileRepository.save(profile);
        verificationProcessRepository.save(process);


        log.info("Profile submitted for review");
        return profile;
    }

    public TutorProfile resubmitProfile() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var profile =
                tutorProfileRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        if (profile.getSubmissionStatus() != ProfileStatus.REJECTED
                && profile.getSubmissionStatus() != ProfileStatus.REVISION_REQUIRED) {
            throw new AppException(ErrorCode.INVALID_RESUBMISSION);
        }

        if (profile.getLastReject() != null && profile.getLastReject().plusMonths(1).isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_RESUBMISSION_TIME);
        }

        // Validate documents again
        List<TutorDocument> documents =
                tutorDocumentRepository.findByProfileUserUserId(profile.getUser().getUserId());
        if (documents.isEmpty() || !hasRequiredDocuments(documents)) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_DOCUMENTS);
        }

        // Update status
        profile.setSubmissionStatus(ProfileStatus.SUBMITTED);
        tutorProfileRepository.save(profile);

        // Create new verification process
        VerificationProcess process = new VerificationProcess();
        process.setCurrentStage(VerificationStage.DOCUMENT_REVIEW);
        process.setProfile(profile);
        process.setStartedAt(LocalDateTime.now());
        process.setPriority(ProcessPriority.HIGH); // Higher priority for resubmissions
        process.setEstimatedCompletionDate(calculateEstimatedCompletion(ProcessPriority.HIGH));

        verificationProcessRepository.save(process);

        log.info("Profile {} resubmitted for review", user.getUserId());
        return profile;
    }

    private void validateProfileForSubmission(TutorProfile profile) {
        List<String> errors = new ArrayList<>();

        if (profile.getHourlyRate() == null
                || profile.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Valid hourly rate is required");
        }

        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INCOMPLETE_PROFILE);
        }
    }

    private boolean hasRequiredDocuments(List<TutorDocument> documents) {
        Set<DocumentType> uploaded =
                documents.stream().map(TutorDocument::getType).collect(Collectors.toSet());

        // Required documents: CV
        return uploaded.contains(DocumentType.CV);
    }

    private LocalDateTime calculateEstimatedCompletion(ProcessPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case URGENT -> now.plusHours(24);
            case HIGH -> now.plusDays(2);
            case NORMAL -> now.plusDays(3);
            case LOW -> now.plusDays(5);
        };
    }

    private TutorStudentDTO mapToResponse(TutorProfile profile, String language) {
        User user = profile.getUser();

        Set<SubjectDTO> subjectResponses =
                profile.getSubjects().stream()
                        .map(
                                subject ->
                                        SubjectDTO.builder()
                                                .subjectId(subject.getSubjectId())
                                                .nameVi(subject.getNameVi())
                                                .nameEn(subject.getNameEn())
                                                .build())
                        .collect(Collectors.toSet());

        // Check if current user has liked this tutor
        boolean isLiked = false;
        var context = SecurityContextHolder.getContext();
        String currentUsername = context.getAuthentication().getName();
        if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
            User currentUser =
                    userRepository.findByUsernameAndNotDeleted(currentUsername).orElse(null);
            if (currentUser != null) {
                isLiked =
                        studentLikesRepository.existsByStudentIdAndTutor_Id(
                                currentUser.getUserId(), profile.getId());
            }
        }

        return TutorStudentDTO.builder()
                // TutorProfile fields
                .id(profile.getId())
                .bioEn(profile.getBioEn())
                .bioVi(profile.getBioVi())
                .experience(profile.getExperience())
                .subjects(subjectResponses)
                .hourlyRate(profile.getHourlyRate())
                .currencyCode(profile.getCurrencyCode())
                .submissionStatus(profile.getSubmissionStatus())
                .descEn(profile.getDescEn())
                .descVi(profile.getDescVi())
                .rejectionReason(profile.getRejectionReason())
                .videoLink(profile.getVideoLink())
                .studentCount(profile.getStudentCount())
                .reviewCount(profile.getReviewCount())
                .rating(profile.getRating())
                .reviewedAt(profile.getReviewedAt())
                .tags(profile.getTags())

                // Like status
                .isLikedByCurrentStudent(isLiked)
                // User fields
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dob(user.getDob())
                .avatar(user.getAvatar())

                // Metadata
                .createdBy(profile.getCreatedBy())
                .createdAt(profile.getCreatedAt())
                .modifiedBy(profile.getModifiedBy())
                .modifiedAt(profile.getModifiedAt())
                .isDeleted(profile.getIsDeleted())
                .build();
    }

    private Set<Subject> validateAndGetSubjects(Set<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_IDS_REQUIRED);
        }

        Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(subjectIds));

        if (subjects.size() != subjectIds.size()) {
            throw new AppException(ErrorCode.SOME_SUBJECTS_NOT_FOUND);
        }

        return subjects;
    }

    private Set<Tag> validateAndGetTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new AppException(ErrorCode.TAG_IDS_REQUIRED);
        }

        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));

        if (tags.size() != tagIds.size()) {
            throw new AppException(ErrorCode.SOME_TAGS_NOT_FOUND);
        }

        return tags;
    }

    @Cacheable(value = "topTutors", key = "#limit")
    public List<TopTutorResponse> getTopTutors(int limit) {
        // Only allow 20
        if (limit >= 20) {
            limit = 20;
        }
        Pageable pageable = PageRequest.of(0, limit);
        List<TutorProfile> tutors = tutorProfileRepository.findTopTutors(pageable);

        return tutors.stream()
                .map(tp -> {
                    User user = tp.getUser();
                    return TopTutorResponse.builder()
                            .id(tp.getId())
                            .tutorName(user != null ? user.getFirstName() + " " + user.getLastName() : "")
                            .avatar(user != null ? user.getAvatar() : null)
                            .hourlyRate(tp.getHourlyRate())
                            .currencyCode(tp.getCurrencyCode() != null ? tp.getCurrencyCode().name() : null)
                            .rating(tp.getRating())
                            .studentCount(tp.getStudentCount())
                            .reviewCount(tp.getReviewCount())
                            .build();
                })
                .toList();
    }
}
