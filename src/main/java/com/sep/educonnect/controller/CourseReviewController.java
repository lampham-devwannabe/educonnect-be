package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.review.CourseReviewDTO;
import com.sep.educonnect.dto.review.CourseReviewSummaryDTO;
import com.sep.educonnect.dto.review.CreateCourseReviewRequest;
import com.sep.educonnect.dto.review.UpdateCourseReviewRequest;
import com.sep.educonnect.service.CourseReviewService;
import com.sep.educonnect.service.I18nService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseReviewController {
    CourseReviewService reviewService;
    I18nService i18nService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CourseReviewDTO> createReview(
            @Valid @RequestBody CreateCourseReviewRequest request) {
        return ApiResponse.<CourseReviewDTO>builder()
                .result(reviewService.createReview(request))
                .build();
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CourseReviewDTO> updateReview(
            @PathVariable Long reviewId, @Valid @RequestBody UpdateCourseReviewRequest request) {
        return ApiResponse.<CourseReviewDTO>builder()
                .result(reviewService.updateReview(reviewId, request))
                .build();
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ApiResponse.<Void>builder().message(i18nService.msg("msg.review.delete")).build();
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<Page<CourseReviewDTO>> getCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<CourseReviewDTO>>builder()
                .result(reviewService.getCourseReviews(courseId, page, size))
                .build();
    }

    @GetMapping("/course/{courseId}/summary")
    public ApiResponse<CourseReviewSummaryDTO> getCourseReviewSummary(@PathVariable Long courseId) {
        return ApiResponse.<CourseReviewSummaryDTO>builder()
                .result(reviewService.getCourseReviewSummary(courseId))
                .build();
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<CourseReviewDTO>> getMyReviews() {
        return ApiResponse.<List<CourseReviewDTO>>builder()
                .result(reviewService.getMyReviews())
                .build();
    }
}
