package com.sep.educonnect.controller.admin;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.exception.request.ApproveExceptionRequest;
import com.sep.educonnect.dto.exception.response.ExceptionResponse;
import com.sep.educonnect.dto.tutor.response.ScheduleChangeResponse;
import com.sep.educonnect.enums.ExceptionStatus;
import com.sep.educonnect.service.TutorAvailabilityExceptionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminExceptionController {
    TutorAvailabilityExceptionService exceptionService;

    @GetMapping("/exceptions/pending")
    public ApiResponse<Page<ExceptionResponse>> getPendingExceptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy
    ) {
        Page<ExceptionResponse> responses = exceptionService.getPendingExceptions(page, size, sortBy);
        return ApiResponse.<Page<ExceptionResponse>>builder()
                .result(responses)
                .build();
    }

    @PostMapping("/exceptions/approve")
    public ApiResponse<ExceptionResponse> approveException(
            @RequestBody @Valid ApproveExceptionRequest request) {

        ExceptionResponse response = exceptionService.approveException(request);
        return ApiResponse.<ExceptionResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/exceptions/all")
    public ApiResponse<Page<ExceptionResponse>> getAllExceptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ExceptionStatus status,
            @RequestParam(required = false) String sortBy) {

        Page<ExceptionResponse> responses = exceptionService.getAllExceptions(status, page, size, sortBy);
        return ApiResponse.<Page<ExceptionResponse>>builder()
                .result(responses)
                .build();
    }

    @PostMapping("/schedule-change/{scheduleChangeId}/approve")
    public ApiResponse<ScheduleChangeResponse> approveScheduleChange(
            @PathVariable Long scheduleChangeId,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String rejectionReason) {
        return ApiResponse.<ScheduleChangeResponse>builder()
                .result(exceptionService.approveScheduleChange(scheduleChangeId, approved, rejectionReason))
                .build();
    }

    @GetMapping("/schedule-change/pending")
    public ApiResponse<Page<ScheduleChangeResponse>> getPendingScheduleChanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String sortBy
    ) {
        return ApiResponse.<Page<ScheduleChangeResponse>>builder()
                .result(exceptionService.getPendingScheduleChanges(className, page, size, sortBy))
                .build();
    }

    @GetMapping("/schedule-change/all")
    public ApiResponse<Page<ScheduleChangeResponse>> getAllScheduleChanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.<Page<ScheduleChangeResponse>>builder()
                .result(exceptionService.getAllScheduleChanges(status, className, page, size, sortBy))
                .build();
    }
}
