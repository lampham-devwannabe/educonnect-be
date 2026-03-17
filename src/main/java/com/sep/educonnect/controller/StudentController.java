package com.sep.educonnect.controller;

import com.sep.educonnect.dto.attendance.SessionAttendanceDTO;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.course.response.MyCourseResponse;
import com.sep.educonnect.dto.student.CheckInviteRequest;
import com.sep.educonnect.dto.student.StudentBookingListItemResponse;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.student.StudentScheduleDTO;
import com.sep.educonnect.dto.subject.request.SetPreferencesRequest;
import com.sep.educonnect.dto.tag.TagImageResponse;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.dto.tutor.TutorStudentDTO;
import com.sep.educonnect.dto.tutor.response.TutorClassResponse;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.enums.GroupType;
import com.sep.educonnect.helper.LocalizationHelper;
import com.sep.educonnect.mapper.TutorClassMapper;
import com.sep.educonnect.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
// @PreAuthorize("hasRole('STUDENT')")
public class StudentController {
    StudentService studentService;
    TutorProfileService tutorProfileService;
    BookingService bookingService;
    TutorClassService tutorClassService;
    TutorClassMapper tutorClassMapper;
    LocalizationHelper localizationHelper;
    I18nService i18nService;
    TagService tagService;

    @GetMapping("/tutor-profile")
    public ApiResponse<TutorStudentDTO> getTutorProfile(
            @RequestParam(name = "profileId") Long profileId,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String locale) {
        String language = locale.split("-")[0];
        return ApiResponse.<TutorStudentDTO>builder()
                .result(tutorProfileService.getTutorProfileById(profileId, language))
                .build();
    }

    @GetMapping
    public ApiResponse<List<StudentGeneralResponse>> getStudentsToInvite() {
        return ApiResponse.<List<StudentGeneralResponse>>builder()
                .result(studentService.getStudentsForInvitation())
                .build();
    }

    @PostMapping("/can-invite")
    public ApiResponse<String> checkCanInviteStudent(@RequestBody CheckInviteRequest request) {
        return ApiResponse.<String>builder()
                .result(studentService.checkCanInviteStudent(request))
                .build();
    }

    @PostMapping("/update-invite/{bookingId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> updateOrRejectInvitation(@PathVariable Long bookingId, @RequestParam boolean accept,
                                                      @RequestParam Long notificationId) {
        bookingService.updateOrRejectInvite(bookingId, accept, notificationId);
        String message = accept ? "msg.invite.success" : "msg.invite.reject";
        return ApiResponse.<Void>builder().message(i18nService.msg(message)).build();
    }

    @GetMapping("/schedule")
    public ApiResponse<List<StudentScheduleDTO>> getSchedule(
            @RequestParam String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ApiResponse.<List<StudentScheduleDTO>>builder()
                .result(tutorClassService.getStudentSchedule(studentId, fromDate, toDate))
                .build();

    }

    @GetMapping("/schedule/{tutorId}")
    public ApiResponse<WeeklyAvailabilityResponse> getTutorSchedule(
            @PathVariable String tutorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        WeeklyAvailabilityResponse schedule = studentService.getWeeklySchedule(tutorId, startDate);
        return ApiResponse.<WeeklyAvailabilityResponse>builder()
                .result(schedule)
                .build();
    }

    @GetMapping("/class")
    public ApiResponse<TutorClassResponse> getTutorClassByClassId(
            @RequestParam(name = "classId") Long classId) {
        TutorClass classes = tutorClassService.getTutorClassById(classId);
        return ApiResponse.<TutorClassResponse>builder()
                .result(tutorClassMapper.toResponse(classes, localizationHelper))
                .build();
    }

    @GetMapping("/attendance")
    public ApiResponse<List<SessionAttendanceDTO>> getAttendance(
            @RequestParam(name = "classId") Long classId) {
        return ApiResponse.<List<SessionAttendanceDTO>>builder()
                .result(tutorClassService.getStudentAttendanceInClass(classId))
                .build();
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Page<StudentBookingListItemResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) GroupType groupType,
            @RequestParam(required = false) BookingStatus bookingStatus,
            @RequestParam(required = false, name = "courseType") CourseType courseType) {
        return ApiResponse.<Page<StudentBookingListItemResponse>>builder()
                .result(
                        studentService.getStudentBookings(
                                page,
                                size,
                                amount,
                                groupType,
                                bookingStatus,
                                courseType))
                .build();
    }

    @PostMapping("/like/{tutorProfileId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> toggleLikeTutorProfile(@PathVariable Long tutorProfileId) {
        studentService.toggleLikeTutorProfile(tutorProfileId);
        return ApiResponse.<Void>builder().message("msg.tutor.like.update").build();
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<MyCourseResponse>> getMyCourses(
            @RequestParam(defaultValue = "ALL") String status) {
        return ApiResponse.<List<MyCourseResponse>>builder()
                .result(studentService.getMyCourses(status))
                .build();

    }

    @PostMapping("/preferences")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> setPreferences(@RequestBody SetPreferencesRequest request) {
        studentService.setPreferences(request);
        return ApiResponse.<Void>builder()
                .message(i18nService.msg("msg.preferences.updated"))
                .build();
    }

    @GetMapping("/tag-images")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TagImageResponse>> getPreferredTagImages() {
        return ApiResponse.<List<TagImageResponse>>builder()
                .result(tagService.getAllTagWithImages())
                .build();
    }

    @GetMapping("/preferred-tags")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TagResponse>> getMyPreferences() {
        return ApiResponse.<List<TagResponse>>builder()
                .result(studentService.getMyPreferredTags())
                .build();
    }
}
