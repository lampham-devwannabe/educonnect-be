package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.rating.CreateTutorRatingRequest;
import com.sep.educonnect.dto.rating.TutorRatingDTO;
import com.sep.educonnect.dto.rating.TutorRatingSummaryDTO;
import com.sep.educonnect.dto.rating.UpdateTutorRatingRequest;
import com.sep.educonnect.service.TutorRatingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TutorRatingController {
    final TutorRatingService ratingService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TutorRatingDTO> createRating(
            @Valid @RequestBody CreateTutorRatingRequest request) {
        return ApiResponse.<TutorRatingDTO>builder()
                .result(ratingService.createRating(request))
                .build();
    }

    @PutMapping("/{ratingId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TutorRatingDTO> updateRating(
            @PathVariable Long ratingId,
            @Valid @RequestBody UpdateTutorRatingRequest request) {
        return ApiResponse.<TutorRatingDTO>builder()
                .result(ratingService.updateRating(ratingId, request))
                .build();
    }

    // Xóa rating
    @DeleteMapping("/{ratingId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> deleteRating(@PathVariable Long ratingId) {
        ratingService.deleteRating(ratingId);
        return ApiResponse.<Void>builder()
                .message("Rating deleted successfully")
                .build();
    }


    // Lấy rating của tutor
    @GetMapping("/tutor/{tutorId}")
    public ApiResponse<Page<TutorRatingDTO>> getTutorRatings(
            @PathVariable String tutorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<TutorRatingDTO>>builder()
                .result(ratingService.getTutorRatings(tutorId, page, size))
                .build();
    }

    // Lấy rating summary của tutor
    @GetMapping("/tutor/{tutorId}/summary")
    public ApiResponse<TutorRatingSummaryDTO> getTutorRatingSummary(
            @PathVariable String tutorId) {
        return ApiResponse.<TutorRatingSummaryDTO>builder()
                .result(ratingService.getTutorRatingSummary(tutorId))
                .build();
    }

    // Lấy tất cả rating của student hiện tại
    @GetMapping("/my-ratings")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TutorRatingDTO>> getMyRatings() {
        return ApiResponse.<List<TutorRatingDTO>>builder()
                .result(ratingService.getMyRatings())
                .build();
    }

    // Lấy rating của student cho 1 tutor cụ thể
    @GetMapping("/my-rating/tutor/{tutorId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TutorRatingDTO> getMyRatingForTutor(
            @PathVariable String tutorId) {
        return ApiResponse.<TutorRatingDTO>builder()
                .result(ratingService.getMyRatingForTutor(tutorId))
                .build();
    }
}
