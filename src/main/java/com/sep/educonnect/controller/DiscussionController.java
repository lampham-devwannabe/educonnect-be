package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.discussion.request.DiscussionReplyRequest;
import com.sep.educonnect.dto.discussion.request.DiscussionRequest;
import com.sep.educonnect.dto.discussion.response.DiscussionReplyResponse;
import com.sep.educonnect.dto.discussion.response.DiscussionResponse;
import com.sep.educonnect.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscussionController {
    DiscussionService discussionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR', 'STUDENT')")
    public ApiResponse<Void> createDiscussion(@Valid @RequestBody DiscussionRequest request) {
        discussionService.createDiscussion(request);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<Page<DiscussionResponse>> getDiscussionsByLesson(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<DiscussionResponse>>builder()
                .result(discussionService.getDiscussionsByLesson(lessonId, page, size))
                .build();
    }

    @GetMapping("/{discussionId}/replies")
    public ApiResponse<Page<DiscussionReplyResponse>> getRepliesByDiscussion(
            @PathVariable Long discussionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<DiscussionReplyResponse>>builder()
                .result(discussionService.getRepliesByDiscussion(discussionId, page, size))
                .build();
    }

    @PostMapping("/{discussionId}/replies")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<DiscussionReplyResponse> addReply(@PathVariable Long discussionId,
            @Valid @RequestBody DiscussionReplyRequest request) {
        request.setDiscussionId(discussionId);

        var savedReply = discussionService.addReply(request);

        DiscussionReplyResponse response = DiscussionReplyResponse.builder()
                .id(savedReply.getId())
                .discussionId(savedReply.getDiscussion().getId())
                .content(savedReply.getContent())
                .username(null)
                .createdAt(savedReply.getCreatedAt())
                .build();

        return ApiResponse.<DiscussionReplyResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/{discussionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR', 'STUDENT')")
    public ApiResponse<Void> deleteDiscussion(@PathVariable Long discussionId) {
        discussionService.deleteDiscussion(discussionId);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/replies/{replyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR', 'STUDENT')")
    public ApiResponse<Void> deleteReply(@PathVariable Long replyId) {
        discussionService.deleteReply(replyId);
        return ApiResponse.<Void>builder().build();
    }
}
