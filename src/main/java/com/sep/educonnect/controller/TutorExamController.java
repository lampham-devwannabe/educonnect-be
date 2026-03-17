package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.exam.ExamAttemptsResponse;
import com.sep.educonnect.dto.exam.ExamResultResponse;
import com.sep.educonnect.dto.exam.ExamResultsResponse;
import com.sep.educonnect.dto.exam.ExamStatisticsResponse;
import com.sep.educonnect.dto.tutorclass.TeacherClassExamResponse;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.TutorClassService;
import com.sep.educonnect.service.TutorExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tutor/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TUTOR')")
public class TutorExamController {

    private final TutorExamService tutorExamService;
    private final TutorClassService tutorClassService;
    private final UserRepository userRepository;

    @GetMapping("/{examId}/results")
    public ApiResponse<ExamResultsResponse> getExamResults(@PathVariable Long examId) {
        String tutorId = getCurrentUserId();
        return ApiResponse.<ExamResultsResponse>builder()
                .result(tutorExamService.getExamResults(examId, tutorId))
                .build();
    }

    @GetMapping("/{examId}/statistics")
    public ApiResponse<ExamStatisticsResponse> getExamStatistics(@PathVariable Long examId) {
        String tutorId = getCurrentUserId();
        return ApiResponse.<ExamStatisticsResponse>builder()
                .result(tutorExamService.getExamStatistics(examId, tutorId))
                .build();
    }

    @GetMapping("/class/{classId}")
    public ApiResponse<Page<TeacherClassExamResponse>> getExamsByClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<TeacherClassExamResponse>>builder()
                .result(tutorClassService.getClassExams(classId, page, size))
                .build();
    }

    @GetMapping("/{examId}/attempts")
    public ApiResponse<ExamAttemptsResponse> getExamAttempts(
            @PathVariable Long examId, @RequestParam(required = false) String studentId) {
        String tutorId = getCurrentUserId();
        return ApiResponse.<ExamAttemptsResponse>builder()
                .result(tutorExamService.getExamAttempts(examId, tutorId, studentId))
                .build();
    }

    @GetMapping("/{examId}/attempts/{submissionId}")
    public ApiResponse<ExamResultResponse> getExamAttemptDetail(
            @PathVariable Long examId, @PathVariable Long submissionId) {
        String tutorId = getCurrentUserId();
        return ApiResponse.<ExamResultResponse>builder()
                .result(tutorExamService.getExamAttemptDetail(examId, submissionId, tutorId))
                .build();
    }

    private String getCurrentUserId() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository
                .findByUsernameAndNotDeleted(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED))
                .getUserId();
    }
}
