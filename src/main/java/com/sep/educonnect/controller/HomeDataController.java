package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.home.TopCourseResponse;
import com.sep.educonnect.dto.home.TopTutorResponse;
import com.sep.educonnect.service.CourseService;
import com.sep.educonnect.service.TutorProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api")
public class HomeDataController {
    TutorProfileService tutorProfileService;
    CourseService courseService;

    @GetMapping("/top/tutors")
    public ApiResponse<List<TopTutorResponse>> getTopTutors(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<List<TopTutorResponse>>builder()
                .result(tutorProfileService.getTopTutors(Math.min(limit, 10)))
                .build();
    }

    @GetMapping("/top/courses")
    public ApiResponse<List<TopCourseResponse>> getTopCourses(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<List<TopCourseResponse>>builder()
                .result(courseService.getTopCourses(Math.min(limit, 10)))
                .build();
    }
}
