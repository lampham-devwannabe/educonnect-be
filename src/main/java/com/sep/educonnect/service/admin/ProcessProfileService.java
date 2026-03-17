package com.sep.educonnect.service.admin;

import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.admin.request.AddCommentRequest;
import com.sep.educonnect.dto.admin.response.TutorDocumentResponse;
import com.sep.educonnect.dto.comment.ReviewCommentResponse;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.dto.tutor.response.ProfileDocumentResponse;
import com.sep.educonnect.dto.tutor.response.TutorProfileResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.dto.verification.VerificationProcessDetailResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.NotificationService;
import com.sep.educonnect.service.email.MailService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProcessProfileService {
  final TutorDocumentRepository documentRepository;
  final CommentRepository commentRepository;
  final VerificationProcessRepository verificationProcessRepository;
  final UserRepository userRepository;
  final TutorProfileRepository tutorProfileRepository;
  final MailService mailService;
  final I18nService i18nService;
  final NotificationService notificationService;

  @Transactional
  public Page<VerificationProcess> getPendingVerifications(Pageable pageable) {
    return verificationProcessRepository.findByCurrentStage(
        VerificationStage.DOCUMENT_REVIEW, pageable);
  }

  /** Staff gets verifications by status */
  @Transactional
  public Page<VerificationProcess> getVerificationsByStage(
      VerificationStage stage, Pageable pageable) {
    return verificationProcessRepository.findByCurrentStage(stage, pageable);
  }

  /** Staff gets verification details */
  @Transactional
  public VerificationProcessDetailResponse getVerificationDetails(Long processId) {
    VerificationProcess process =
        verificationProcessRepository
            .findById(processId)
            .orElseThrow(() -> new AppException(ErrorCode.PROCESS_NOT_FOUND));

    TutorProfile profile = process.getProfile();
    List<TutorDocument> documents =
        documentRepository.findByProfileUserUserId(profile.getUser().getUserId());
    List<ReviewComment> comments = commentRepository.findByProcessIdOrderByCreatedAtDesc(processId);

    return VerificationProcessDetailResponse.builder()
        .processId(processId)
        .profile(mapToProfileResponse(profile))
        .currentStage(process.getCurrentStage())
        .startedAt(process.getStartedAt())
        .completedAt(process.getCompletedAt())
        .reviewer(process.getReviewer() != null ? mapToUserResponse(process.getReviewer()) : null)
        .priority(process.getPriority())
        .estimatedCompletionDate(process.getEstimatedCompletionDate())
        .documents(documents.stream().map(this::mapToDocumentResponse).toList())
        .comments(comments.stream().map(this::mapToCommentResponse).toList())
        .build();
  }

  public TutorDocumentResponse reviewDocument(
      Long documentId, DocumentStatus status, String rejectionReason) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    TutorDocument document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

    if (status == DocumentStatus.REJECTED
        && (rejectionReason == null || rejectionReason.isEmpty())) {
      throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
    }

    document.setStatus(status);
    document.setVerifiedAt(LocalDateTime.now());
    document.setVerifiedBy(user);

    if (status == DocumentStatus.REJECTED) {
      document.setRejectionReason(rejectionReason);
    } else {
      document.setRejectionReason(null);
    }

    documentRepository.save(document);

    // Add comment to verification process
    Optional<VerificationProcess> processOpt =
        verificationProcessRepository.findByProfileIdAndCurrentStage(
            document.getProfile().getId(), VerificationStage.DOCUMENT_REVIEW);

    if (processOpt.isPresent()) {
      VerificationProcess process = processOpt.get();
      ReviewComment comment = new ReviewComment();
      comment.setProcessId(process.getProcessId());
      comment.setDocumentId(documentId);
      comment.setCommentType(CommentType.DOCUMENT_ISSUE);
      comment.setComment(
          status == DocumentStatus.APPROVED
              ? "Document approved: " + document.getType()
              : "Document rejected: " + rejectionReason);
      comment.setIsVisibleToTutor(status == DocumentStatus.REJECTED);
      comment.setCreatedBy(user.getUsername());
      comment.setCreatedAt(LocalDateTime.now());
      commentRepository.save(comment);
    }

    log.info(
        "Document {} reviewed with status {} by staff {}", documentId, status, user.getUserId());
    return TutorDocumentResponse.fromEntity(document);
  }

  @Transactional
  public TutorProfileResponse approveProfile(Long processId, String comments) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    VerificationProcess process =
        verificationProcessRepository
            .findById(processId)
            .orElseThrow(() -> new AppException(ErrorCode.PROCESS_NOT_FOUND));

    TutorProfile profile = process.getProfile();

    // Validate all documents are approved
    List<TutorDocument> documents =
        documentRepository.findByProfileUserUserId(profile.getUser().getUserId());
    boolean allApproved =
        documents.stream().allMatch(doc -> doc.getStatus() == DocumentStatus.APPROVED);

    if (!allApproved) {
      throw new AppException(ErrorCode.DOCUMENTS_NOT_APPROVED);
    }

    // Update profile status
    profile.setSubmissionStatus(ProfileStatus.APPROVED);
    profile.setReviewedAt(LocalDateTime.now());
    profile.setReviewedBy(user);
    tutorProfileRepository.save(profile);

    // Complete process
    process.setCurrentStage(VerificationStage.COMPLETED);
    process.setCompletedAt(LocalDateTime.now());
    verificationProcessRepository.save(process);

    // Add approval comment
    ReviewComment comment = new ReviewComment();
    comment.setProcessId(processId);
    comment.setCommentType(CommentType.APPROVAL_NOTE);
    comment.setComment(comments != null ? comments : "Profile approved");
    comment.setIsVisibleToTutor(false);
    comment.setCreatedBy(user.getUsername());
    comment.setCreatedAt(LocalDateTime.now());
    commentRepository.save(comment);

    // Notify tutor
    sendApprovalEmail(profile, comments);

    // Send notification to tutor
    if (profile.getUser() != null) {
      String tutorId = profile.getUser().getUserId();
      String message =
          "Your tutor profile has been approved"
              + (comments != null && !comments.isEmpty() ? ": " + comments : "");
      try {
        notificationService.createAndSendNotification(
            tutorId, message, NotificationType.TYPICAL, null, null);
      } catch (Exception e) {
        log.error("Failed to send approval notification to tutor {}: {}", tutorId, e.getMessage());
      }
    }

    log.info("Profile {} approved by staff {}", profile.getId(), user.getUserId());
    return mapToProfileResponse(profile);
  }

  public TutorProfileResponse rejectProfile(Long processId, String rejectionReason) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    if (rejectionReason == null || rejectionReason.isEmpty()) {
      throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
    }

    VerificationProcess process =
        verificationProcessRepository
            .findById(processId)
            .orElseThrow(() -> new AppException(ErrorCode.PROCESS_NOT_FOUND));

    TutorProfile profile = process.getProfile();

    // Update profile status
    profile.setSubmissionStatus(ProfileStatus.REJECTED);
    profile.setReviewedAt(LocalDateTime.now());
    profile.setRejectionReason(rejectionReason);
    profile.setLastReject(LocalDate.now());
    profile.setReviewedBy(user);
    tutorProfileRepository.save(profile);

    // Complete process as rejected
    process.setCurrentStage(VerificationStage.REJECTED);
    process.setCompletedAt(LocalDateTime.now());
    verificationProcessRepository.save(process);

    // Add rejection comment visible to tutor
    ReviewComment comment = new ReviewComment();
    comment.setProcessId(processId);
    comment.setCommentType(CommentType.REJECTION_REASON);
    comment.setComment(rejectionReason);
    comment.setIsVisibleToTutor(true);
    comment.setCreatedBy(user.getUsername());
    comment.setCreatedAt(LocalDateTime.now());
    commentRepository.save(comment);

    // Notify tutor
    sendRejectionEmail(profile, rejectionReason);

    // Send notification to tutor
    if (profile.getUser() != null) {
      String tutorId = profile.getUser().getUserId();
      String message = "Your tutor profile has been rejected: " + rejectionReason;
      try {
        notificationService.createAndSendNotification(
            tutorId, message, NotificationType.TYPICAL, null, null);
      } catch (Exception e) {
        log.error("Failed to send rejection notification to tutor {}: {}", tutorId, e.getMessage());
      }
    }

    log.info("Profile {} rejected by staff {}", profile.getId(), user.getUserId());
    return mapToProfileResponse(profile);
  }

  /** Staff requests revision (tutor needs to fix something but not rejected) */
  public TutorProfileResponse requestRevision(Long processId, String revisionNotes) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    if (revisionNotes == null || revisionNotes.isEmpty()) {
      throw new AppException(ErrorCode.REVISION_NOTES_REQUIRED);
    }

    VerificationProcess process =
        verificationProcessRepository
            .findById(processId)
            .orElseThrow(() -> new AppException(ErrorCode.PROCESS_NOT_FOUND));

    TutorProfile profile = process.getProfile();

    // Update profile status to require revision
    profile.setSubmissionStatus(ProfileStatus.REVISION_REQUIRED);
    profile.setReviewedAt(LocalDateTime.now());
    profile.setReviewedBy(user);
    tutorProfileRepository.save(profile);

    // Pause the process (not completed)
    process.setCurrentStage(VerificationStage.REVISION);
    process.setCompletedAt(LocalDateTime.now());
    verificationProcessRepository.save(process);

    // Add revision request comment visible to tutor
    ReviewComment comment = new ReviewComment();
    comment.setProcessId(processId);
    comment.setCommentType(CommentType.REVISION_REASON);
    comment.setComment(revisionNotes);
    comment.setIsVisibleToTutor(true);
    comment.setCreatedBy(user.getUsername());
    comment.setCreatedAt(LocalDateTime.now());
    commentRepository.save(comment);

    // Notify tutor
    sendRevisionRequestEmail(profile, revisionNotes);

    // Send notification to tutor
    if (profile.getUser() != null) {
      String tutorId = profile.getUser().getUserId();
      String message = "Your tutor profile requires revision: " + revisionNotes;
      try {
        notificationService.createAndSendNotification(
            tutorId, message, NotificationType.TYPICAL, null, null);
      } catch (Exception e) {
        log.error(
            "Failed to send revision request notification to tutor {}: {}",
            tutorId,
            e.getMessage());
      }
    }

    log.info(
        "Profile {} requires revision - requested by staff {}", profile.getId(), user.getUserId());
    return mapToProfileResponse(profile);
  }

  /** Staff adds comment to verification process */
  public ReviewCommentResponse addComment(Long processId, AddCommentRequest request) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    if (request.getComment() == null || request.getComment().isEmpty()) {
      throw new AppException(ErrorCode.COMMENT_TEXT_REQUIRED);
    }

    VerificationProcess process =
        verificationProcessRepository
            .findById(processId)
            .orElseThrow(() -> new AppException(ErrorCode.PROCESS_NOT_FOUND));

    if (request.getDocumentId() != null && request.getDocumentId() > 0) {
      documentRepository
          .findById(request.getDocumentId())
          .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    ReviewComment comment = new ReviewComment();
    comment.setProcessId(processId);
    comment.setCommentType(request.getCommentType());
    comment.setComment(request.getComment());
    comment.setIsVisibleToTutor(request.isVisibleToTutor());
    comment.setCreatedBy(user.getUsername());
    comment.setCreatedAt(LocalDateTime.now());
    comment.setDocumentId(
        request.getDocumentId() != null && request.getDocumentId() > 0
            ? request.getDocumentId()
            : null);

    commentRepository.save(comment);

    // Send notification to tutor if comment is visible to tutor
    if (request.isVisibleToTutor()
        && process.getProfile() != null
        && process.getProfile().getUser() != null) {
      String tutorId = process.getProfile().getUser().getUserId();
      String message =
          "A new comment has been added to your verification process: " + request.getComment();
      try {
        notificationService.createAndSendNotification(
            tutorId, message, NotificationType.TYPICAL, null, null);
      } catch (Exception e) {
        log.error("Failed to send comment notification to tutor {}: {}", tutorId, e.getMessage());
      }
    }

    log.info("Comment added to process {} by staff {}", processId, user.getUserId());
    return mapToCommentResponse(comment);
  }

  public List<ReviewCommentResponse> getComment(Long processId) {
    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();
    userRepository
        .findByUsername(name)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    List<ReviewComment> comments = commentRepository.findByProcessIdOrderByCreatedAtDesc(processId);

    return comments.stream().map(this::mapToCommentResponse).toList();
  }

  private TutorProfileResponse mapToProfileResponse(TutorProfile profile) {
    TutorProfileResponse response = new TutorProfileResponse();
    response.setId(profile.getId());
    response.setVideoLink(profile.getVideoLink());
    response.setBioEn(profile.getBioEn());
    response.setBioVi(profile.getBioVi());
    response.setDescVi(profile.getDescVi());
    response.setDescEn(profile.getDescEn());
    response.setProfileStatus(profile.getSubmissionStatus());
    response.setReviewCount(profile.getReviewCount());
    response.setHourlyRate(profile.getHourlyRate());
    response.setReviewedAt(profile.getReviewedAt());
    response.setReviewedBy(mapToUserResponse(profile.getReviewedBy()));
    response.setStudentCount(profile.getStudentCount());
    response.setExperience(profile.getExperience());
    response.setSubjects(profile.getSubjects());
    response.setTags(profile.getTags());

    if (profile.getUser() != null) {
      response.setTutor(mapToUserResponse(profile.getUser()));
    }

    return response;
  }

  private ProfileDocumentResponse mapToDocumentResponse(TutorDocument document) {
    ProfileDocumentResponse response = new ProfileDocumentResponse();
    response.setDocumentId(document.getId());
    response.setDocumentType(document.getType());
    response.setFileName(document.getFileName());
    response.setStatus(document.getStatus());
    response.setUploadedAt(document.getUploadedAt());
    response.setVerifiedAt(document.getVerifiedAt());
    response.setRejectionReason(document.getRejectionReason());
    return response;
  }

  private UserResponse mapToUserResponse(User user) {
    if (user == null) return null;

    UserResponse response = new UserResponse();
    response.setUserId(user.getUserId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setAvatar(user.getAvatar());
    response.setPhoneNumber(user.getPhoneNumber());
    response.setAddress(user.getAddress());
    response.setDob(user.getDob());
    return response;
  }

  private ReviewCommentResponse mapToCommentResponse(ReviewComment comment) {
    ReviewCommentResponse response = new ReviewCommentResponse();
    response.setCommentId(comment.getCommentId());
    response.setProcessId(comment.getProcessId());
    response.setDocumentId(comment.getDocumentId());
    response.setCommentType(comment.getCommentType());
    response.setComment(comment.getComment());
    response.setIsVisibleToTutor(comment.getIsVisibleToTutor());
    response.setCreatedAt(comment.getCreatedAt());
    response.setCreatedBy(comment.getCreatedBy());

    return response;
  }

  private void sendApprovalEmail(TutorProfile profile, String comments) {
    User tutor = profile.getUser();

    Email email =
        Email.builder()
            .subject(i18nService.msg("email.subject.profile.approved"))
            .to(List.of(Mailer.builder().email(tutor.getEmail()).build()))
            .build();

    Map<String, Object> variables =
        Map.of(
            "tutorName",
            tutor.getFirstName() + " " + tutor.getLastName(),
            "profileId",
            profile.getId(),
            "approvedDate",
            profile.getReviewedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            "comments",
            comments != null ? comments : "Your profile has been approved");

    mailService.send(email, TemplateMail.PROFILE_APPROVAL, variables);
    log.info("Approval email sent to tutor: {}", tutor.getEmail());
  }

  // 2. Send rejection email
  private void sendRejectionEmail(TutorProfile profile, String rejectionReason) {
    User tutor = profile.getUser();

    Email email =
        Email.builder()
            .subject(i18nService.msg("email.subject.profile.rejected"))
            .to(List.of(Mailer.builder().email(tutor.getEmail()).build()))
            .build();

    Map<String, Object> variables =
        Map.of(
            "tutorName",
            tutor.getFirstName() + " " + tutor.getLastName(),
            "profileId",
            profile.getId(),
            "rejectedDate",
            profile.getReviewedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            "rejectionReason",
            rejectionReason);

    mailService.send(email, TemplateMail.PROFILE_REJECTION, variables);
    log.info("Rejection email sent to tutor: {}", tutor.getEmail());
  }

  // 3. Send revision request email
  private void sendRevisionRequestEmail(TutorProfile profile, String revisionNotes) {
    User tutor = profile.getUser();

    Email email =
        Email.builder()
            .subject(i18nService.msg("email.subject.profile.revision"))
            .to(List.of(Mailer.builder().email(tutor.getEmail()).build()))
            .build();

    Map<String, Object> variables =
        Map.of(
            "tutorName",
            tutor.getFirstName() + " " + tutor.getLastName(),
            "profileId",
            profile.getId(),
            "revisionDate",
            profile.getReviewedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            "revisionNotes",
            revisionNotes);

    mailService.send(email, TemplateMail.PROFILE_REVISION_REQUEST, variables);
    log.info("Revision request email sent to tutor: {}", tutor.getEmail());
  }
}
