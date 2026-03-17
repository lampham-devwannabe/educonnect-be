package com.sep.educonnect.controller.admin;

import com.sep.educonnect.dto.admin.request.AddCommentRequest;
import com.sep.educonnect.dto.admin.request.ApproveProfileRequest;
import com.sep.educonnect.dto.admin.request.RejectProfileRequest;
import com.sep.educonnect.dto.admin.request.RequestRevisionRequest;
import com.sep.educonnect.dto.admin.response.TutorDocumentResponse;
import com.sep.educonnect.dto.admin.response.VerificationProcessResponse;
import com.sep.educonnect.dto.comment.ReviewCommentResponse;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.tutor.request.ReviewDocumentRequest;
import com.sep.educonnect.dto.tutor.response.TutorProfileResponse;
import com.sep.educonnect.dto.verification.VerificationProcessDetailResponse;
import com.sep.educonnect.entity.VerificationProcess;
import com.sep.educonnect.enums.VerificationStage;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.admin.ProcessProfileService;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/verification")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminVerificationController {

  final ProcessProfileService processProfileService;
  final I18nService i18nService;

  /** Get pending verifications GET /api/admin/verification/pending */
  @GetMapping("/pending")
  public ApiResponse<Page<VerificationProcessResponse>> getPendingVerifications(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "startedAt") String sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc(sort)));
    Page<VerificationProcess> processes = processProfileService.getPendingVerifications(pageable);

    Page<VerificationProcessResponse> dtoPage =
        processes.map(VerificationProcessResponse::fromEntity);

    return ApiResponse.<Page<VerificationProcessResponse>>builder().result(dtoPage).build();
  }

  /** Get verifications by stage GET /api/admin/verification/stage/{stage} */
  @GetMapping("/stage/{stage}")
  public ApiResponse<Page<VerificationProcessResponse>> getVerificationsByStage(
      @PathVariable VerificationStage stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "startedAt") String sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc(sort)));
    Page<VerificationProcess> processes =
        processProfileService.getVerificationsByStage(stage, pageable);

    Page<VerificationProcessResponse> dtoPage =
        processes.map(VerificationProcessResponse::fromEntity);

    return ApiResponse.<Page<VerificationProcessResponse>>builder()
        .code(1000)
        .result(dtoPage)
        .build();
  }

  /** Get verification details GET /api/admin/verification/{processId} */
  @GetMapping("/{processId}")
  public ApiResponse<VerificationProcessDetailResponse> getVerificationDetails(
      @PathVariable Long processId) {

    VerificationProcessDetailResponse details =
        processProfileService.getVerificationDetails(processId);

    return ApiResponse.<VerificationProcessDetailResponse>builder()
        .code(1000)
        .result(details)
        .build();
  }

  /** Review a document POST /api/admin/verification/documents/{documentId}/review */
  @PostMapping("/documents/{documentId}/review")
  public ApiResponse<TutorDocumentResponse> reviewDocument(
      @PathVariable Long documentId, @RequestBody ReviewDocumentRequest request) {

    TutorDocumentResponse document =
        processProfileService.reviewDocument(
            documentId, request.getStatus(), request.getRejectionReason());

    return ApiResponse.<TutorDocumentResponse>builder().result(document).build();
  }

  /** Approve profile POST /api/admin/verification/{processId}/approve */
  @PostMapping("/{processId}/approve")
  public ApiResponse<TutorProfileResponse> approveProfile(
      @PathVariable Long processId, @RequestBody ApproveProfileRequest request) {

    TutorProfileResponse response =
        processProfileService.approveProfile(processId, request.getComments());

    return ApiResponse.<TutorProfileResponse>builder()
        .code(1000)
        .message(i18nService.msg("msg.profile.approve"))
        .result(response)
        .build();
  }

  /** Reject profile POST /api/admin/verification/{processId}/reject */
  @PostMapping("/{processId}/reject")
  public ApiResponse<TutorProfileResponse> rejectProfile(
      @PathVariable Long processId, @RequestBody RejectProfileRequest request) {

    TutorProfileResponse response =
        processProfileService.rejectProfile(processId, request.getRejectionReason());

    return ApiResponse.<TutorProfileResponse>builder()
        .code(1000)
        .message(i18nService.msg("msg.profile.reject"))
        .result(response)
        .build();
  }

  /** Request revision from tutor POST /api/admin/verification/{processId}/request-revision */
  @PostMapping("/{processId}/request-revision")
  public ApiResponse<TutorProfileResponse> requestRevision(
      @PathVariable Long processId, @RequestBody RequestRevisionRequest request) {

    TutorProfileResponse response =
        processProfileService.requestRevision(processId, request.getRevisionNotes());

    return ApiResponse.<TutorProfileResponse>builder()
        .code(1000)
        .message(i18nService.msg("msg.revision.request"))
        .result(response)
        .build();
  }

  /** Add comment to verification POST /api/admin/verification/{processId}/comments */
  @PostMapping("/{processId}/comments")
  public ApiResponse<ReviewCommentResponse> addComment(
      @PathVariable Long processId, @RequestBody AddCommentRequest request) {

    ReviewCommentResponse response = processProfileService.addComment(processId, request);

    return ApiResponse.<ReviewCommentResponse>builder()
        .code(1000)
        .message(i18nService.msg("msg.comment.add"))
        .result(response)
        .build();
  }

  /** Get all comments for a verification GET /api/admin/verification/{processId}/comments */
  @GetMapping("/{processId}/comments")
  public ApiResponse<List<ReviewCommentResponse>> getProcessComments(@PathVariable Long processId) {

    List<ReviewCommentResponse> result = processProfileService.getComment(processId);

    return ApiResponse.<List<ReviewCommentResponse>>builder().code(1000).result(result).build();
  }
}
