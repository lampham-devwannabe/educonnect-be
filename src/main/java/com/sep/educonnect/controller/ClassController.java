package com.sep.educonnect.controller;

import com.sep.educonnect.dto.classenrollment.ClassStudentResponse;
import com.sep.educonnect.dto.classenrollment.StudentClassResponse;
import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.service.TutorClassService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/class")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassController {
    TutorClassService tutorClassService;

    @GetMapping("/{classId}/students")
    public ApiResponse<List<ClassStudentResponse>> getClassStudents(@PathVariable Long classId) {
        return ApiResponse.<List<ClassStudentResponse>>builder()
                .result(tutorClassService.getClassStudents(classId))
                .build();
    }

    @GetMapping("/my-classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StudentClassResponse>> getMyClasses(
            @RequestParam(required = false) String classTitle,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) String tutorName) {
        return ApiResponse.<List<StudentClassResponse>>builder()
                .result(tutorClassService.getStudentClasses(classTitle, startDate, endDate, tutorName))
                .build();
    }
}
