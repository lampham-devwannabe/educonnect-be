package com.sep.educonnect.controller;

import com.sep.educonnect.dto.attendance.AttendanceRequest;
import com.sep.educonnect.dto.attendance.AttendanceResponse;
import com.sep.educonnect.dto.booking.BookingResponse;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.exam.StudentExamListItemResponse;
import com.sep.educonnect.dto.exception.request.BatchCreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.CreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.UpdateExceptionRequest;
import com.sep.educonnect.dto.exception.response.ExceptionListResponse;
import com.sep.educonnect.dto.exception.response.ExceptionResponse;
import com.sep.educonnect.dto.invitation.InviteToClassRequest;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.tutor.request.*;
import com.sep.educonnect.dto.tutor.response.ProfileDTO;
import com.sep.educonnect.dto.tutor.response.ScheduleChangeResponse;
import com.sep.educonnect.dto.tutor.response.TutorClassResponse;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.dto.tutorclass.TeacherClassExamResponse;
import com.sep.educonnect.dto.tutorclass.TutorClassDTO;
import com.sep.educonnect.dto.zoom.ZoomMeetingRequest;
import com.sep.educonnect.dto.zoom.ZoomMeetingResponse;
import com.sep.educonnect.entity.TutorAvailability;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.entity.TutorDocument;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.enums.ExceptionStatus;
import com.sep.educonnect.helper.LocalizationHelper;
import com.sep.educonnect.mapper.TutorClassMapper;
import com.sep.educonnect.service.*;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('TUTOR')")
public class TutorController {
    TutorProfileService tutorProfileService;
    TutorDocumentService tutorDocumentService;
    TutorAvailabilityService tutorAvailabilityService;
    TutorClassService tutorClassService;
    TutorClassMapper tutorClassMapper;
    LocalizationHelper localizationHelper;
    ZoomService zoomService;
    TutorAvailabilityExceptionService exceptionService;
    BookingService bookingService;
    I18nService i18nService;

    @GetMapping("/profile")
    public ApiResponse<ProfileDTO> getProfile() {
        return ApiResponse.<ProfileDTO>builder()
                .result(tutorProfileService.getTutorProfile())
                .build();
    }

    @PostMapping("/profile")
    public ApiResponse<TutorProfile> saveProfile(
            @RequestBody SubmitProfileRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String locale) {
        // locale = "vi-VN" hoặc "en-US"
        String language = locale.split("-")[0]; // lấy "vi" hoặc "en"
        return ApiResponse.<TutorProfile>builder()
                .result(tutorProfileService.saveTutorProfile(request, language))
                .build();
    }

    @PutMapping("/profile")
    public ApiResponse<TutorProfile> updateProfile(
            @RequestBody SubmitProfileRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String locale) {
        // locale = "vi-VN" hoặc "en-US"
        String language = locale.split("-")[0]; // lấy "vi" hoặc "en"
        return ApiResponse.<TutorProfile>builder()
                .result(tutorProfileService.updateTutorProfile(request, language))
                .build();
    }

    @GetMapping("/profile/submit-document")
    public ApiResponse<List<TutorDocument>> getSubmitDocument() {
        return ApiResponse.<List<TutorDocument>>builder()
                .result(tutorDocumentService.getSubmitDocument())
                .build();
    }

    @PostMapping("/profile/submit-document")
    public ApiResponse<TutorDocument> submitDocument(@RequestBody SubmitDocumentRequest request) {

        return ApiResponse.<TutorDocument>builder()
                .result(tutorDocumentService.submitTutorDocument(request))
                .build();
    }

    @PutMapping("/profile/update-document")
    public ApiResponse<TutorDocument> updateDocument(@RequestBody UpdateDocumentRequest request) {

        return ApiResponse.<TutorDocument>builder()
                .result(tutorDocumentService.updateTutorDocument(request))
                .build();
    }

    @PostMapping("/profile/submit")
    public ApiResponse<TutorProfile> submitProfile() {
        return ApiResponse.<TutorProfile>builder()
                .result(tutorProfileService.submitProfileForReview())
                .build();
    }

    @PostMapping("/profile/resubmit")
    public ApiResponse<TutorProfile> resubmitProfile() {

        TutorProfile profile = tutorProfileService.resubmitProfile();

        return ApiResponse.<TutorProfile>builder()
                .code(1000)
                .message("Profile resubmitted for review successfully")
                .result(profile)
                .build();
    }

    @PostMapping("/profile/upload-video")
    public ApiResponse<String> uploadVideo(@RequestParam("file-id") String file) {
        String videoUrl = tutorProfileService.uploadVideo(file);

        return ApiResponse.<String>builder().result(videoUrl).build();
    }

    @PutMapping("/availability")
    public ApiResponse<TutorAvailability> updateAvailability(
            @RequestBody AvailabilityUpdateRequest request) {
        TutorAvailability result = tutorAvailabilityService.updateAvailability(request);
        return ApiResponse.<TutorAvailability>builder().result(result).build();
    }

    @GetMapping("/availability")
    public ApiResponse<WeeklyAvailabilityResponse> getTutorSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        WeeklyAvailabilityResponse schedule = tutorAvailabilityService.getWeeklySchedule(startDate);
        return ApiResponse.<WeeklyAvailabilityResponse>builder().result(schedule).build();
    }

    @PostMapping("/classes")
    public ApiResponse<TutorClassResponse> createTutorClass(
            @RequestBody CreateTutorClassRequest request) {
        TutorClass result = tutorClassService.createTutorClass(request);
        return ApiResponse.<TutorClassResponse>builder()
                .result(tutorClassMapper.toResponse(result, localizationHelper))
                .build();
    }

    @GetMapping("/class")
    public ApiResponse<TutorClassResponse> getTutorClassByClassId(
            @RequestParam(name = "classId") Long classId) {
        TutorClass classes = tutorClassService.getTutorClassByClassId(classId);
        return ApiResponse.<TutorClassResponse>builder()
                .result(tutorClassMapper.toResponse(classes, localizationHelper))
                .build();
    }

    @GetMapping("/classes-session")
    public ApiResponse<List<TutorClassResponse>> getAllTutorClasses(
            @RequestParam(name = "startDate") LocalDate startDate) {
        return ApiResponse.<List<TutorClassResponse>>builder()
                .result(tutorClassService.getTutorClasses(startDate))
                .build();
    }

    @GetMapping("/classes")
    public ApiResponse<Page<TutorClassDTO>> getAllClass(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<TutorClassDTO>>builder()
                .result(tutorClassService.getAllClass(page, size))
                .build();
    }

    @PostMapping("/classes/invite")
    public ApiResponse<Void> inviteStudentsToClass(@RequestBody InviteToClassRequest request) {
        tutorClassService.inviteStudents(request.getClassId(), request.getStudentIds());
        return ApiResponse.<Void>builder()
                .message(i18nService.msg("msg.student.invite.success"))
                .build();
    }

    @PostMapping("/meet-url")
    public ApiResponse<ZoomMeetingResponse> createMeetUrl(@RequestBody ZoomMeetingRequest request) {
        ZoomMeetingResponse result = zoomService.createMeeting(request);

        return ApiResponse.<ZoomMeetingResponse>builder().result(result).build();
    }

    @GetMapping("/class/attendance")
    public ApiResponse<List<AttendanceResponse>> getAttendance(
            @RequestParam(name = "sessionId") Long sessionId) {
        return ApiResponse.<List<AttendanceResponse>>builder()
                .result(tutorClassService.getAttendanceList(sessionId))
                .build();
    }

    @PostMapping("/class/attendance")
    public ApiResponse<List<AttendanceResponse>> createAttendance(
            @RequestBody List<AttendanceRequest> attendances) {
        return ApiResponse.<List<AttendanceResponse>>builder()
                .result(tutorClassService.createAttendance(attendances))
                .build();
    }

    @PutMapping("/class/attendance")
    public ApiResponse<List<AttendanceResponse>> updateAttendance(
            @RequestBody List<AttendanceRequest> attendances) {
        return ApiResponse.<List<AttendanceResponse>>builder()
                .result(tutorClassService.updateAttendance(attendances))
                .build();
    }

    @PostMapping("/exceptions")
    public ApiResponse<ExceptionResponse> createException(
            @RequestBody @Valid CreateExceptionRequest request) {

        ExceptionResponse response = exceptionService.createException(request);
        return ApiResponse.<ExceptionResponse>builder().result(response).build();
    }

    @PostMapping("/exceptions/batch")
    public ApiResponse<List<ExceptionResponse>> createBatchExceptions(
            @RequestBody @Valid BatchCreateExceptionRequest request) {

        List<ExceptionResponse> responses = exceptionService.createBatchExceptions(request);
        return ApiResponse.<List<ExceptionResponse>>builder().result(responses).build();
    }

    @PutMapping("/exceptions")
    public ApiResponse<ExceptionResponse> updateException(
            @RequestParam(name = "exceptionId") Long exceptionId,
            @RequestBody @Valid UpdateExceptionRequest request) {

        ExceptionResponse response = exceptionService.updateException(exceptionId, request);
        return ApiResponse.<ExceptionResponse>builder().result(response).build();
    }

    @DeleteMapping("/exceptions")
    public ApiResponse<Void> cancelException(@RequestParam(name = "exceptionId") Long exceptionId) {
        exceptionService.cancelException(exceptionId);
        return ApiResponse.<Void>builder()
                .message(i18nService.msg("msg.exception.cancelled"))
                .build();
    }

    @GetMapping("/exceptions")
    public ApiResponse<ExceptionListResponse> getMyExceptions(
            @RequestParam(required = false) ExceptionStatus status) {

        ExceptionListResponse response = exceptionService.getMyExceptions(status);
        return ApiResponse.<ExceptionListResponse>builder().result(response).build();
    }

    @PostMapping("/schedule-changes")
    public ApiResponse<ScheduleChangeResponse> createScheduleChange(
            @Valid @RequestBody CreateScheduleChangeRequest request) {
        return ApiResponse.<ScheduleChangeResponse>builder()
                .result(exceptionService.createScheduleChange(request))
                .build();
    }

    @PutMapping("/schedule-changes/{scheduleChangeId}")
    public ApiResponse<ScheduleChangeResponse> updateScheduleChange(
            @PathVariable Long scheduleChangeId,
            @Valid @RequestBody CreateScheduleChangeRequest request) {
        return ApiResponse.<ScheduleChangeResponse>builder()
                .result(exceptionService.updateScheduleChange(scheduleChangeId, request))
                .build();
    }

    @DeleteMapping("/schedule-changes/{scheduleChangeId}")
    public ApiResponse<Void> cancelScheduleChange(@PathVariable Long scheduleChangeId) {
        exceptionService.cancelScheduleChange(scheduleChangeId);
        return ApiResponse.<Void>builder()
                .message(i18nService.msg("msg.schedule.change.cancelled"))
                .build();
    }

    @GetMapping("/schedule-changes")
    public ApiResponse<List<ScheduleChangeResponse>> getMyScheduleChanges(
            @RequestParam(required = false) String status, @RequestParam LocalDate startDate) {

        return ApiResponse.<List<ScheduleChangeResponse>>builder()
                .result(exceptionService.getMyScheduleChanges(status, startDate))
                .build();
    }

    @GetMapping("/class/to-invite")
    public ApiResponse<List<StudentGeneralResponse>> getStudentsToInvite(
            @RequestParam Long classId) {
        return ApiResponse.<List<StudentGeneralResponse>>builder()
                .result(tutorClassService.getStudentsToInvite(classId))
                .build();
    }

    @GetMapping("/teacher-report/class/{classId}/exams")
    public ApiResponse<Page<TeacherClassExamResponse>> getClassExams(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<TeacherClassExamResponse>>builder()
                .result(tutorClassService.getClassExams(classId, page, size))
                .build();
    }

    @GetMapping("/student-report/class/{classId}/student/{studentId}/exams")
    public ApiResponse<Page<StudentExamListItemResponse>> getStudentExamsInClass(
            @PathVariable Long classId,
            @PathVariable String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<StudentExamListItemResponse>>builder()
                .result(tutorClassService.getStudentExamsInClass(classId, studentId, page, size))
                .build();
    }

    @GetMapping("/booking/missing")
    public ApiResponse<List<BookingResponse>> getMyMissingBookings() {
        return ApiResponse.<List<BookingResponse>>builder()
                .result(bookingService.getMyMissingBookings())
                .build();
    }
}
