package com.sep.educonnect.service;

import com.sep.educonnect.dto.course.request.CourseCreationRequest;
import com.sep.educonnect.dto.course.request.CourseUpdateRequest;
import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.home.TopCourseResponse;
import com.sep.educonnect.dto.syllabus.response.SyllabusResponse;
import com.sep.educonnect.dto.tutor.response.TutorGeneralResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.helper.LocalizationHelper;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.mapper.LessonMapper;
import com.sep.educonnect.mapper.ModuleMapper;
import com.sep.educonnect.mapper.SyllabusMapper;
import com.sep.educonnect.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
  private final CourseRepository courseRepository;
  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final SyllabusRepository syllabusRepository;
  private final UserRepository userRepository;
  private final TutorProfileRepository tutorProfileRepository;
  private final CourseMapper courseMapper;
  private final SyllabusMapper syllabusMapper;
  private final ModuleMapper moduleMapper;
  private final LessonMapper lessonMapper;
  private final LocalizationHelper localizationHelper;
  private final BookingRepository bookingRepository;
  private final CourseReviewRepository courseReviewRepository;
  private final TutorClassRepository tutorClassRepository;

  @Transactional
  public CourseInfoResponse createCourse(CourseCreationRequest request) {
    log.info("Creating new course: {}", request.getName());

    // Validate syllabus exists
    Syllabus syllabus =
        syllabusRepository
            .findByIdAndNotDeleted(request.getSyllabusId())
            .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));

    // Validate tutor exists
    User tutor =
        userRepository
            .findById(request.getTutorId())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    Course course = courseMapper.toCourse(request);
    course.setSyllabus(syllabus);
    course.setTutor(tutor);

    course = courseRepository.save(course);
    log.info("Course created successfully with ID: {}", course.getId());

    if (request.getType() == CourseType.SELF_PACED) {

      TutorClass tutorClass =
          TutorClass.builder()
              .course(course)
              .tutor(tutor)
              .currentStudents(0)
              .maxStudents(Integer.MAX_VALUE)
              .title(request.getName() + " - Self Paced Class")
              .build();
      tutorClassRepository.save(tutorClass);
    }

    return getCourseInfo(course.getId());
  }

  public Page<CourseInfoResponse> getAllCoursesPublic(
      int page,
      int size,
      String sortBy,
      String direction,
      String name,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String tutorName) {
    log.info("Fetching all courses with pagination - page: {}, size: {}", page, size);
    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    Page<Course> coursePage =
        courseRepository.searchPublicCourses(
            nullableText(name), minPrice, maxPrice, nullableText(tutorName), pageable);
    Page<CourseInfoResponse> responsePage = coursePage.map(courseMapper::toCourseInfoResponse);

    setTutorProfileIds(responsePage.getContent());

    return responsePage;
  }

  public Page<CourseInfoResponse> getAllCoursesPrivate(
      int page,
      int size,
      String sortBy,
      String direction,
      String name,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String tutorName,
      Long syllabusId,
      CourseType type) {
    log.info("Fetching all courses full info with pagination - page: {}, size: {}", page, size);
    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    Page<Course> coursePage =
        courseRepository.searchAdminCourses(
            nullableText(name),
            minPrice,
            maxPrice,
            nullableText(tutorName),
            syllabusId,
            type,
            pageable);
    Page<CourseInfoResponse> responsePage = coursePage.map(courseMapper::toCourseInfoResponse);

    setTutorProfileIds(responsePage.getContent());

    return responsePage;
  }

  public CourseInfoResponse getCourseInfo(Long courseId) {
    Course course =
        courseRepository
            .findByIdAndIsDeletedFalse(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

    CourseInfoResponse response = courseMapper.toCourseInfoResponse(course);

    // Map syllabus using SyllabusMapper
    Syllabus syllabus = course.getSyllabus();
    if (syllabus != null) {
      SyllabusResponse syllabusResponse =
          syllabusMapper.toSyllabusResponse(syllabus, localizationHelper);
      List<Module> modules = syllabus.getModules();
      if (modules == null) {
        modules = List.of();
      }
      response.setSyllabus(
          courseMapper.toSyllabusInfo(
              syllabusResponse, syllabus.getSyllabusId(), moduleMapper, lessonMapper, modules));
    }

    response.setTotalEnrolled(countTotalEnrolledStudents(courseId));

    setTutorProfileIds(List.of(response));

    return response;
  }

  private Long countTotalEnrolledStudents(Long courseId) {
    Set<String> enrolledStudentIds =
        classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId);
    return (long) enrolledStudentIds.size();
  }

  public List<CourseInfoResponse> getCoursesByTutor(String tutorId) {
    List<Course> courses = courseRepository.findByTutor_UserIdAndIsDeletedFalse(tutorId);

    List<CourseInfoResponse> list =
        courses.stream().map(c -> courseMapper.toCourseInfoResponse(c)).toList();

    list.stream().forEach(c -> c.setTotalEnrolled(countTotalEnrolledStudents(c.getId())));

    return list;
  }

  @Transactional
  public CourseInfoResponse updateCourse(Long courseId, CourseUpdateRequest request) {
    log.info("Updating course with ID: {}", courseId);

    Course course =
        courseRepository
            .findByIdAndIsDeletedFalse(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

    // Update syllabus if provided
    if (request.getSyllabusId() != null) {
      Syllabus syllabus =
          syllabusRepository
              .findByIdAndNotDeleted(request.getSyllabusId())
              .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));
      course.setSyllabus(syllabus);
    }

    // Update tutor if provided
    if (request.getTutorId() != null) {
      User tutor =
          userRepository
              .findById(request.getTutorId())
              .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
      course.setTutor(tutor);
    }

    // Update other fields
    courseMapper.updateCourse(course, request);

    course = courseRepository.save(course);
    log.info("Course updated successfully with ID: {}", course.getId());

    return getCourseInfo(course.getId());
  }

  @Transactional
  public void deleteCourse(Long courseId) {
    log.info("Soft deleting course with ID: {}", courseId);

    Course course =
        courseRepository
            .findByIdAndIsDeletedFalse(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

    course.setIsDeleted(true);
    courseRepository.save(course);

    log.info("Course soft deleted successfully with ID: {}", courseId);
  }

  private void setTutorProfileIds(List<CourseInfoResponse> responses) {
    // Fetch tutor profile IDs in batch
    List<String> userIds =
        responses.stream()
            .map(r -> r.getTutor().getUserId())
            .distinct()
            .collect(Collectors.toList());
    List<TutorProfile> profiles = tutorProfileRepository.findAllByUserUserIdIn(userIds);
    Map<String, Long> userIdToProfileId =
        profiles.stream()
            .collect(Collectors.toMap(p -> p.getUser().getUserId(), TutorProfile::getId));

    // Set tutorProfileId for each response
    responses.forEach(
        response -> {
          if (response.getTutor() != null) {
            Long profileId = userIdToProfileId.get(response.getTutor().getUserId());
            response.getTutor().setTutorProfileId(profileId);
          }
        });
  }

  public List<TutorGeneralResponse> getTutorsBySubject(Long id) {
    List<TutorProfile> tutorProfiles = tutorProfileRepository.findBySubjectId(id);
    log.info("Found {} tutors for subject ID: {}", tutorProfiles.size(), id);
    return tutorProfiles.stream()
        .map(
            tp -> {
              User user = tp.getUser();
              return TutorGeneralResponse.builder()
                  .id(user.getUserId())
                  .tutorName(user.getFirstName() + " " + user.getLastName())
                  .avatar(user.getAvatar())
                  .email(user.getEmail())
                  .hourlyRate(tp.getHourlyRate())
                  .build();
            })
        .toList();
  }

  public String uploadCoursePicture(Long courseId, String file) {
    Course course =
        courseRepository
            .findByIdAndIsDeletedFalse(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
    course.setPictureUrl(file);
    courseRepository.save(course);
    return file;
  }

  private String nullableText(String value) {
    return value != null && !value.isBlank() ? value : null;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "topCourses", key = "#limit")
  public List<TopCourseResponse> getTopCourses(int limit) {
    if (limit >= 20) limit = 20;
    Pageable pageable = PageRequest.of(0, limit);
    List<Course> courses = courseRepository.findTopCourses(pageable);

    return courses.stream()
        .map(
            course -> {
              User tutor = course.getTutor();
              TutorProfile tutorProfile =
                  tutor != null
                      ? tutorProfileRepository.findByUserUserId(tutor.getUserId()).orElse(null)
                      : null;

              Long paidBookingsCount =
                  bookingRepository.countByCourseIdAndBookingStatus(
                      course.getId(), BookingStatus.PAID);
              Long reviewsCount = courseReviewRepository.countByCourse_Id(course.getId());
              Double averageRating =
                  courseReviewRepository.findAverageRatingByCourseId(course.getId());

              return TopCourseResponse.builder()
                  .id(course.getId())
                  .name(course.getName())
                  .price(course.getPrice())
                  .pictureUrl(course.getPictureUrl())
                  .type(course.getType())
                  .totalLessons(course.getTotalLessons())
                  .tutorName(
                      tutor != null ? tutor.getFirstName() + " " + tutor.getLastName() : null)
                  .tutorAvatar(tutor != null ? tutor.getAvatar() : null)
                  .tutorRating(tutorProfile != null ? tutorProfile.getRating() : null)
                  .paidBookingsCount(paidBookingsCount != null ? paidBookingsCount : 0L)
                  .reviewsCount(reviewsCount != null ? reviewsCount : 0L)
                  .averageRating(averageRating != null ? averageRating : 0.0)
                  .build();
            })
        .toList();
  }
}
