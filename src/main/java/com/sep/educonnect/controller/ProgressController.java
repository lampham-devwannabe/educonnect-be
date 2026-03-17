package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.progress.CourseProgressResponse;
import com.sep.educonnect.dto.progress.LessonProgressResponse;
import com.sep.educonnect.dto.progress.UpdateLessonProgressRequest;
import com.sep.educonnect.entity.CourseProgress;
import com.sep.educonnect.entity.LessonProgress;
import com.sep.educonnect.mapper.ProgressMapper;
import com.sep.educonnect.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final ProgressMapper progressMapper;

    @GetMapping("/enrollments/{enrollmentId}")
    public ApiResponse<CourseProgressResponse> getCourseProgress(@PathVariable Long enrollmentId) {
        CourseProgress courseProgress = progressService.getCourseProgress(enrollmentId);
        CourseProgressResponse response = progressMapper.toCourseProgressResponse(courseProgress);
        List<LessonProgressResponse> lessonResponses = progressMapper.toLessonProgressResponses(
                progressService.getLessonProgresses(enrollmentId));
        response.setLessons(lessonResponses);
        return ApiResponse.<CourseProgressResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/enrollments/{enrollmentId}/lessons")
    public ApiResponse<List<LessonProgressResponse>> getLessonProgresses(@PathVariable Long enrollmentId) {
        List<LessonProgress> progresses = progressService.getLessonProgresses(enrollmentId);
        List<LessonProgressResponse> responses = progressMapper.toLessonProgressResponses(progresses);
        return ApiResponse.<List<LessonProgressResponse>>builder()
                .result(responses)
                .build();
    }

    @PutMapping("/lessons")
    public ApiResponse<LessonProgressResponse> updateLessonProgress(@Valid @RequestBody UpdateLessonProgressRequest request) {
        LessonProgress updated = progressService.updateLessonProgress(
                request.getEnrollmentId(),
                request.getLessonId(),
                request.getStatus(),
                request.getResult(),
                request.getSkipOptional());

        LessonProgressResponse response = progressMapper.toLessonProgressResponse(updated);
        return ApiResponse.<LessonProgressResponse>builder()
                .result(response)
                .build();
    }
}

