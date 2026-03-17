package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.exam.ExamForStudentResponse;
import com.sep.educonnect.dto.exam.ExamSubmissionRequest;
import com.sep.educonnect.dto.exam.ExamSubmissionResponse;
import com.sep.educonnect.dto.exam.StudentExamListItemResponse;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.StudentExamService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentExamController {
    StudentExamService studentExamService;
    UserRepository userRepository;

    @GetMapping("/{examId}")
    public ApiResponse<ExamForStudentResponse> getExam(@PathVariable Long examId) {
        String studentId = getCurrentUserId();
        return ApiResponse.<ExamForStudentResponse>builder()
                .result(studentExamService.getExamForStudent(examId, studentId))
                .build();
    }

    @PostMapping("/{examId}/submit")
    public ApiResponse<ExamSubmissionResponse> submitExam(
            @PathVariable Long examId, @RequestBody @Valid ExamSubmissionRequest request) {
        String studentId = getCurrentUserId();
        return ApiResponse.<ExamSubmissionResponse>builder()
                .result(studentExamService.submitExam(examId, studentId, request))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<StudentExamListItemResponse>> getMyExams(
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String studentId = getCurrentUserId();
        return ApiResponse.<Page<StudentExamListItemResponse>>builder()
                .result(studentExamService.getMyExams(studentId, classId, page, size))
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
