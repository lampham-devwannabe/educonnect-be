package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.course.request.CourseCreationRequest;
import com.sep.educonnect.dto.course.request.CourseUpdateRequest;
import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.tutor.response.TutorGeneralResponse;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.service.CourseService;
import com.sep.educonnect.service.I18nService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {
  CourseService courseService;
  I18nService i18nService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
  public ApiResponse<CourseInfoResponse> createCourse(
      @RequestBody @Valid CourseCreationRequest request) {
    CourseInfoResponse response = courseService.createCourse(request);
    return ApiResponse.<CourseInfoResponse>builder().result(response).build();
  }

  @GetMapping
  public ApiResponse<Page<CourseInfoResponse>> getAllCourses(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "asc") String direction,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) String tutorName) {
    Page<CourseInfoResponse> response =
        courseService.getAllCoursesPublic(
            page, size, sortBy, direction, name, minPrice, maxPrice, tutorName);
    return ApiResponse.<Page<CourseInfoResponse>>builder().result(response).build();
  }

  @GetMapping("/admin")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
  public ApiResponse<Page<CourseInfoResponse>> getAllCoursesAdmin(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "asc") String direction,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) String tutorName,
      @RequestParam(required = false) Long syllabusId,
      @RequestParam(required = false) CourseType type) {
    Page<CourseInfoResponse> response =
        courseService.getAllCoursesPrivate(
            page, size, sortBy, direction, name, minPrice, maxPrice, tutorName, syllabusId, type);
    return ApiResponse.<Page<CourseInfoResponse>>builder().result(response).build();
  }

  @GetMapping("/{id}")
  public ApiResponse<CourseInfoResponse> getCourseInfo(@PathVariable Long id) {
    CourseInfoResponse response = courseService.getCourseInfo(id);
    return ApiResponse.<CourseInfoResponse>builder().result(response).build();
  }

  @GetMapping("/by-tutor")
  public ApiResponse<List<CourseInfoResponse>> getCourseByTutor(@RequestParam String tutorId) {
    List<CourseInfoResponse> res = courseService.getCoursesByTutor(tutorId);
    return ApiResponse.<List<CourseInfoResponse>>builder().result(res).build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
  public ApiResponse<CourseInfoResponse> updateCourse(
      @PathVariable Long id, @RequestBody @Valid CourseUpdateRequest request) {
    CourseInfoResponse response = courseService.updateCourse(id, request);
    return ApiResponse.<CourseInfoResponse>builder().result(response).build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
  public ApiResponse<String> deleteCourse(@PathVariable Long id) {
    courseService.deleteCourse(id);
    return ApiResponse.<String>builder()
        .result(i18nService.msg("msg.course.deleted.success"))
        .build();
  }

  @GetMapping("/tutor/by-subject/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<List<TutorGeneralResponse>> getTutorsBySubject(@PathVariable Long id) {
    var res = courseService.getTutorsBySubject(id);
    return ApiResponse.<List<TutorGeneralResponse>>builder().result(res).build();
  }

  @PostMapping("/{id}/upload-picture")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TUTOR')")
  public ApiResponse<String> uploadCoursePicture(
      @PathVariable Long id, @RequestParam("file-id") String file) {
    String pictureUrl = courseService.uploadCoursePicture(id, file);
    return ApiResponse.<String>builder().result(pictureUrl).build();
  }
}
