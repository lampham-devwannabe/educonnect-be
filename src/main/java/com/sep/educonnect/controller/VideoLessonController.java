package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.video.*;
import com.sep.educonnect.enums.LessonProgressStatus;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.service.ProgressService;
import com.sep.educonnect.service.VideoService;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/video-lessons")
@RequiredArgsConstructor
public class VideoLessonController {

    private final VideoService videoService;
    private final String siteDomain = ".educonnect.dev";
    private final ProgressService progressService;

    @PostMapping
    public ApiResponse<VideoLessonResponse> create(@RequestBody VideoLessonRequest req) {
        return ApiResponse.<VideoLessonResponse>builder()
                .result(videoService.createVideoLesson(req))
                .build();
    }

    @PostMapping("/{id}/upload-url")
    public ApiResponse<VideoUploadUrlResponse> getUploadUrl(@PathVariable Long id) {
        String url = videoService.generateUploadUrl(id);
        return ApiResponse.<VideoUploadUrlResponse>builder()
                .result(
                        VideoUploadUrlResponse.builder()
                                .uploadUrl(url)
                                .expiresInSeconds(3600L)
                                .build())
                .build();
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<ApiResponse<StreamingInfo>> stream(
            @PathVariable Long id,
            @RequestParam(required = false) Long enrollmentId,
            HttpServletResponse servletResponse) {
        StreamingInfo info = videoService.getStreamingInfo(id);
        if (enrollmentId != null) {
            try {
                VideoLessonResponse lessonResponse = videoService.getVideoLesson(id);
                if (lessonResponse.getLessonId() != null) {
                    progressService.updateLessonProgress(
                            enrollmentId,
                            lessonResponse.getLessonId(),
                            LessonProgressStatus.IN_PROGRESS,
                            null,
                            null);
                }
            } catch (Exception ex) {
                log.warn(
                        "Failed to update progress for enrollment {} and video lesson {}: {}",
                        enrollmentId,
                        id,
                        ex.getMessage());
            }
        }

        info.getCookies()
                .forEach(
                        (name, value) -> {
                            ResponseCookie cookie =
                                    ResponseCookie.from(name, value)
                                            .domain(siteDomain) // Đặt domain cookie để chia sẻ giữa
                                            // các subdomain
                                            .path("/")
                                            .httpOnly(false)
                                            .sameSite("none")
                                            .secure(true)
                                            .maxAge(Duration.ofHours(1))
                                            .build();
                            servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                        });

        ApiResponse<StreamingInfo> response =
                ApiResponse.<StreamingInfo>builder().code(1000).message("OK").result(info).build();

        return ResponseEntity.ok(response);
    }

    private String extractCloudFrontHost(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost(); // Trả về host để log, không dùng để set cookie
        } catch (Exception e) {
            log.warn("Failed to extract host from URL: {}", url, e);
            return null;
        }
    }

    @GetMapping("/{id}/status")
    public ApiResponse<VideoStatusResponse> status(@PathVariable Long id) {
        VideoLessonResponse video = videoService.getVideoLesson(id);
        return ApiResponse.<VideoStatusResponse>builder()
                .result(
                        VideoStatusResponse.builder()
                                .videoLessonId(id)
                                .status(video.getStatus())
                                .processingProgress(video.getProcessingProgress())
                                .errorMessage(video.getProcessingErrorMessage())
                                .startedAt(null)
                                .completedAt(video.getProcessedAt())
                                .build())
                .build();
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<Page<VideoLessonResponse>> listByLesson(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.<Page<VideoLessonResponse>>builder()
                .result(videoService.getVideosByLesson(lessonId, page, size, sortBy, direction))
                .build();
    }

    @PostMapping("/update-status")
    public ApiResponse<Void> updateStatus(
            @RequestParam Long videoLessonId,
            @RequestParam(required = false) String hlsMasterPlaylistS3Key,
            @RequestParam(required = false) Integer processingProgress,
            @RequestParam String status,
            @RequestParam(required = false) String errorMessage) {
        log.info(
                "Received status update callback for videoLessonId: {}, status: {}, manifestKey: {}",
                videoLessonId,
                status,
                hlsMasterPlaylistS3Key);

        try {
            videoService.updateVideoStatus(
                    videoLessonId,
                    VideoStatus.valueOf(status),
                    hlsMasterPlaylistS3Key,
                    processingProgress,
                    errorMessage);
            log.info("Successfully updated status for videoLessonId: {}", videoLessonId);
            return ApiResponse.<Void>builder().build();
        } catch (IllegalArgumentException e) {
            log.error(
                    "Invalid status value or videoLessonId not found: videoLessonId={}, status={}",
                    videoLessonId,
                    status,
                    e);
            throw e;
        } catch (Exception e) {
            log.error("Error updating video status for videoLessonId: {}", videoLessonId, e);
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}
