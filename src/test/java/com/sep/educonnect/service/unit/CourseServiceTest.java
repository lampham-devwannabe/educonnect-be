package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.course.request.CourseCreationRequest;
import com.sep.educonnect.dto.course.request.CourseUpdateRequest;
import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.Syllabus;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.mapper.SyllabusMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.CourseService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
class CourseServiceTest {

  @Mock private CourseRepository courseRepository;

  @Mock private ClassEnrollmentRepository classEnrollmentRepository;

  @Mock private SyllabusRepository syllabusRepository;

  @Mock private UserRepository userRepository;

  @Mock private TutorProfileRepository tutorProfileRepository;

  @Mock private CourseMapper courseMapper;

  @Mock private TutorClassRepository tutorClassRepository;

  @Mock private SyllabusMapper syllabusMapper;

  @InjectMocks private CourseService courseService;

  @Test
  @DisplayName("Should create course successfully")
  void should_createCourse_successfully() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .syllabusId(1L)
            .tutorId("tutor1")
            .status(CourseStatus.ONGOING)
            .type(CourseType.SELF_PACED)
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();
    User tutor = User.builder().userId("tutor1").username("tutor1").build();

    Course courseToSave =
        Course.builder()
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .status(CourseStatus.ONGOING)
            .type(CourseType.SELF_PACED)
            .build();

    Course savedCourse =
        Course.builder()
            .id(1L)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .syllabus(syllabus)
            .tutor(tutor)
            .status(CourseStatus.ONGOING)
            .type(CourseType.SELF_PACED)
            .build();

    CourseInfoResponse.TutorInfo tutorInfo =
        CourseInfoResponse.TutorInfo.builder().userId("tutor1").username("tutor1").build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(1L)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .status(CourseStatus.ONGOING)
            .type(CourseType.SELF_PACED)
            .tutor(tutorInfo)
            .totalEnrolled(0L)
            .build();

    // Mock
    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor1")).thenReturn(Optional.of(tutor));
    when(courseMapper.toCourse(request)).thenReturn(courseToSave);
    when(courseRepository.save(courseToSave)).thenReturn(savedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(savedCourse));
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(1L)).thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());
    when(courseMapper.toCourseInfoResponse(savedCourse)).thenReturn(expectedResponse);

    // When
    CourseInfoResponse result = courseService.createCourse(request);

    // Then
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("Java Basics", result.getName());
    assertEquals(CourseType.SELF_PACED, result.getType());
    assertEquals(BigDecimal.valueOf(100), result.getPrice());

    // Verify basic flow
    verify(syllabusRepository).findByIdAndNotDeleted(1L);
    verify(userRepository).findById("tutor1");
    verify(courseMapper).toCourse(request);
    verify(courseRepository).save(courseToSave);

    // Verify TutorClass creation
    ArgumentCaptor<TutorClass> tutorClassCaptor = ArgumentCaptor.forClass(TutorClass.class);

    verify(tutorClassRepository).save(tutorClassCaptor.capture());

    TutorClass tutorClass = tutorClassCaptor.getValue();
    assertEquals(savedCourse, tutorClass.getCourse());
    assertEquals(tutor, tutorClass.getTutor());
    assertEquals(0, tutorClass.getCurrentStudents());
    assertEquals(Integer.MAX_VALUE, tutorClass.getMaxStudents());
    assertEquals("Java Basics - Self Paced Class", tutorClass.getTitle());
  }

  @Test
  @DisplayName("Should throw AppException when syllabus not found for create")
  void should_throwAppException_when_syllabusNotFoundForCreate() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Java Basics")
            .syllabusId(999L)
            .tutorId("tutor1")
            .build();

    when(syllabusRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(
            AppException.class,
            () -> {
              courseService.createCourse(request);
            });

    assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
    verify(syllabusRepository).findByIdAndNotDeleted(999L);
    verify(userRepository, never()).findById(anyString());
  }

  @Test
  @DisplayName("Should throw AppException when tutor not found for create")
  void should_throwAppException_when_tutorNotFoundForCreate() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Java Basics")
            .syllabusId(1L)
            .tutorId("tutor999")
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();

    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor999")).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(
            AppException.class,
            () -> {
              courseService.createCourse(request);
            });

    assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    verify(syllabusRepository).findByIdAndNotDeleted(1L);
    verify(userRepository).findById("tutor999");
  }

  // ==================== Additional tests for createCourse ====================

  @Test
  @DisplayName("Should create course with ONLINE type")
  void should_createCourse_withOnlineType() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Live Online Course")
            .price(BigDecimal.valueOf(200))
            .syllabusId(1L)
            .tutorId("tutor1")
            .status(CourseStatus.PLANNED)
            .type(CourseType.ONLINE)
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();
    User tutor = User.builder().userId("tutor1").username("tutor1").build();
    Course course = Course.builder().name("Live Online Course").type(CourseType.ONLINE).build();
    Course savedCourse =
        Course.builder()
            .id(2L)
            .name("Live Online Course")
            .type(CourseType.ONLINE)
            .syllabus(syllabus)
            .tutor(tutor)
            .build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(2L)
            .name("Live Online Course")
            .type(CourseType.ONLINE)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("tutor1").build())
            .totalEnrolled(0L)
            .build();

    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor1")).thenReturn(Optional.of(tutor));
    when(courseMapper.toCourse(request)).thenReturn(course);
    when(courseRepository.save(course)).thenReturn(savedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(savedCourse));
    when(courseMapper.toCourseInfoResponse(savedCourse)).thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(2L)).thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.createCourse(request);

    // Then
    assertNotNull(result);
    assertEquals(CourseType.ONLINE, result.getType());
    verify(courseRepository).save(course);
  }

  @Test
  @DisplayName("Should create course with PLANNED status")
  void should_createCourse_withPlannedStatus() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Future Course")
            .price(BigDecimal.valueOf(150))
            .syllabusId(1L)
            .tutorId("tutor1")
            .status(CourseStatus.PLANNED)
            .type(CourseType.SELF_PACED)
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();
    User tutor = User.builder().userId("tutor1").build();
    Course course = Course.builder().status(CourseStatus.PLANNED).build();
    Course savedCourse =
        Course.builder()
            .id(3L)
            .status(CourseStatus.PLANNED)
            .syllabus(syllabus)
            .tutor(tutor)
            .build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(3L)
            .status(CourseStatus.PLANNED)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("tutor1").build())
            .totalEnrolled(0L)
            .build();

    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor1")).thenReturn(Optional.of(tutor));
    when(courseMapper.toCourse(request)).thenReturn(course);
    when(courseRepository.save(course)).thenReturn(savedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(3L)).thenReturn(Optional.of(savedCourse));
    when(courseMapper.toCourseInfoResponse(savedCourse)).thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(3L)).thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.createCourse(request);

    // Then
    assertNotNull(result);
    assertEquals(CourseStatus.PLANNED, result.getStatus());
  }

  @Test
  @DisplayName("Should create course with INACTIVE status")
  void should_createCourse_withInactiveStatus() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Inactive Course")
            .syllabusId(1L)
            .tutorId("tutor1")
            .status(CourseStatus.INACTIVE)
            .type(CourseType.SELF_PACED)
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();
    User tutor = User.builder().userId("tutor1").build();
    Course course = Course.builder().status(CourseStatus.INACTIVE).build();
    Course savedCourse =
        Course.builder()
            .id(4L)
            .status(CourseStatus.INACTIVE)
            .syllabus(syllabus)
            .tutor(tutor)
            .build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(4L)
            .status(CourseStatus.INACTIVE)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("tutor1").build())
            .totalEnrolled(0L)
            .build();

    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor1")).thenReturn(Optional.of(tutor));
    when(courseMapper.toCourse(request)).thenReturn(course);
    when(courseRepository.save(course)).thenReturn(savedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(savedCourse));
    when(courseMapper.toCourseInfoResponse(savedCourse)).thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(4L)).thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.createCourse(request);

    // Then
    assertNotNull(result);
    assertEquals(CourseStatus.INACTIVE, result.getStatus());
  }

  @Test
  @DisplayName("Should create course with null price")
  void should_createCourse_withNullPrice() {
    // Given
    CourseCreationRequest request =
        CourseCreationRequest.builder()
            .name("Free Course")
            .price(null) // Null price
            .syllabusId(1L)
            .tutorId("tutor1")
            .status(CourseStatus.ONGOING)
            .type(CourseType.SELF_PACED)
            .build();

    Syllabus syllabus = Syllabus.builder().syllabusId(1L).build();
    User tutor = User.builder().userId("tutor1").build();
    Course course = Course.builder().price(null).build();
    Course savedCourse =
        Course.builder().id(5L).price(null).syllabus(syllabus).tutor(tutor).build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(5L)
            .price(null)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("tutor1").build())
            .totalEnrolled(0L)
            .build();

    when(syllabusRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(syllabus));
    when(userRepository.findById("tutor1")).thenReturn(Optional.of(tutor));
    when(courseMapper.toCourse(request)).thenReturn(course);
    when(courseRepository.save(course)).thenReturn(savedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(savedCourse));
    when(courseMapper.toCourseInfoResponse(savedCourse)).thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(5L)).thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.createCourse(request);

    // Then
    assertNotNull(result);
    assertNull(result.getPrice());
  }

  @Test
  @DisplayName("Should get course info successfully")
  void should_getCourseInfo_successfully() {
    // Given
    Long courseId = 1L;
    Course course =
        Course.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .status(CourseStatus.ONGOING)
            .syllabus(null) // No syllabus for simplicity
            .build();

    CourseInfoResponse.TutorInfo tutorInfo =
        CourseInfoResponse.TutorInfo.builder().userId("tutor1").username("tutor1").build();

    CourseInfoResponse response =
        CourseInfoResponse.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .status(CourseStatus.ONGOING)
            .tutor(tutorInfo)
            .build();

    when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.of(course));
    when(courseMapper.toCourseInfoResponse(course)).thenReturn(response);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
        .thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.getCourseInfo(courseId);

    // Then
    assertNotNull(result);
    assertEquals(courseId, result.getId());
    assertEquals("Java Basics", result.getName());
    assertEquals(0L, result.getTotalEnrolled());
    verify(courseRepository).findByIdAndIsDeletedFalse(courseId);
    verify(classEnrollmentRepository).findDistinctStudentUserIdsByCourseId(courseId);
  }

  @Test
  @DisplayName("Should throw AppException when course not found for getCourseInfo")
  void should_throwAppException_when_courseNotFoundForGetCourseInfo() {
    // Given
    Long courseId = 999L;
    when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(
            AppException.class,
            () -> {
              courseService.getCourseInfo(courseId);
            });

    assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    verify(courseRepository).findByIdAndIsDeletedFalse(courseId);
  }

  @Test
  @DisplayName("Should get courses by tutor successfully")
  void should_getCoursesByTutor_successfully() {
    // Given
    String tutorId = "tutor1";
    Course course1 = Course.builder().id(1L).name("Java Basics").build();
    Course course2 = Course.builder().id(2L).name("Python Basics").build();
    List<Course> courses = List.of(course1, course2);

    when(courseRepository.findByTutor_UserIdAndIsDeletedFalse(tutorId)).thenReturn(courses);

    // Mock the courseMapper
    CourseInfoResponse response1 = CourseInfoResponse.builder().id(1L).name("Java Basics").build();
    CourseInfoResponse response2 =
        CourseInfoResponse.builder().id(2L).name("Python Basics").build();

    when(courseMapper.toCourseInfoResponse(course1)).thenReturn(response1);
    when(courseMapper.toCourseInfoResponse(course2)).thenReturn(response2);

    // When
    List<CourseInfoResponse> result = courseService.getCoursesByTutor(tutorId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(1L, result.get(0).getId());
    assertEquals("Java Basics", result.get(0).getName());
    assertEquals(2L, result.get(1).getId());
    assertEquals("Python Basics", result.get(1).getName());

    verify(courseRepository).findByTutor_UserIdAndIsDeletedFalse(tutorId);
    verify(courseMapper).toCourseInfoResponse(course1);
    verify(courseMapper).toCourseInfoResponse(course2);
  }

  @Test
  @DisplayName("Should update course successfully")
  void should_updateCourse_successfully() {
    // Given
    Long courseId = 1L;
    CourseUpdateRequest request =
        CourseUpdateRequest.builder()
            .name("Updated Java Basics")
            .price(BigDecimal.valueOf(150))
            .status(CourseStatus.ONGOING)
            .totalLessons(20)
            .build();

    Course existingCourse =
        Course.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .status(CourseStatus.INACTIVE)
            .totalLessons(10)
            .build();

    Course updatedCourse =
        Course.builder()
            .id(courseId)
            .name("Updated Java Basics")
            .price(BigDecimal.valueOf(150))
            .status(CourseStatus.ONGOING)
            .totalLessons(20)
            .build();

    CourseInfoResponse.TutorInfo tutorInfo =
        CourseInfoResponse.TutorInfo.builder().userId("tutor1").username("tutor1").build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(courseId)
            .name("Updated Java Basics")
            .price(BigDecimal.valueOf(150))
            .status(CourseStatus.ONGOING)
            .totalLessons(20)
            .tutor(tutorInfo)
            .totalEnrolled(0L)
            .build();

    Course updatedCourseWithoutSyllabus =
        Course.builder()
            .id(courseId)
            .name("Updated Java Basics")
            .price(BigDecimal.valueOf(150))
            .status(CourseStatus.ONGOING)
            .totalLessons(20)
            .syllabus(null) // No syllabus for simplicity
            .build();

    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(existingCourse));
    doNothing().when(courseMapper).updateCourse(any(Course.class), any(CourseUpdateRequest.class));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(updatedCourseWithoutSyllabus));
    when(courseMapper.toCourseInfoResponse(updatedCourseWithoutSyllabus))
        .thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
        .thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.updateCourse(courseId, request);

    // Then
    assertNotNull(result);
    assertEquals(courseId, result.getId());
    assertEquals("Updated Java Basics", result.getName());
    assertEquals(20, result.getTotalLessons());
    verify(courseRepository, times(2))
        .findByIdAndIsDeletedFalse(courseId); // Once in updateCourse, once in
    verify(courseMapper).updateCourse(any(Course.class), any(CourseUpdateRequest.class));
    verify(courseRepository).save(any(Course.class));
  }

  @Test
  @DisplayName("Should update course totalLessons successfully")
  void should_updateCourse_totalLessons_successfully() {
    // Given
    Long courseId = 1L;
    CourseUpdateRequest request = CourseUpdateRequest.builder().totalLessons(30).build();

    Course existingCourse =
        Course.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .totalLessons(10)
            .build();

    Course updatedCourse =
        Course.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .totalLessons(30)
            .build();

    CourseInfoResponse.TutorInfo tutorInfo =
        CourseInfoResponse.TutorInfo.builder().userId("tutor1").username("tutor1").build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .totalLessons(30)
            .tutor(tutorInfo)
            .totalEnrolled(0L)
            .build();

    Course updatedCourseWithoutSyllabus =
        Course.builder()
            .id(courseId)
            .name("Java Basics")
            .price(BigDecimal.valueOf(100))
            .totalLessons(30)
            .syllabus(null)
            .build();

    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(existingCourse));
    doNothing().when(courseMapper).updateCourse(any(Course.class), any(CourseUpdateRequest.class));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(updatedCourseWithoutSyllabus));
    when(courseMapper.toCourseInfoResponse(updatedCourseWithoutSyllabus))
        .thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
        .thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.updateCourse(courseId, request);

    // Then
    assertNotNull(result);
    assertEquals(courseId, result.getId());
    assertEquals(30, result.getTotalLessons());
    verify(courseRepository, times(2)).findByIdAndIsDeletedFalse(courseId);
    verify(courseMapper).updateCourse(any(Course.class), any(CourseUpdateRequest.class));
    verify(courseRepository).save(any(Course.class));
  }

  @Test
  @DisplayName("Should update course with new syllabus successfully")
  void should_updateCourse_withNewSyllabus_successfully() {
    // Given
    Long courseId = 1L;
    CourseUpdateRequest request =
        CourseUpdateRequest.builder().name("Updated Course").syllabusId(2L).build();

    Syllabus newSyllabus = Syllabus.builder().syllabusId(2L).build();

    Course existingCourse = Course.builder().id(courseId).name("Original Course").build();

    CourseInfoResponse.TutorInfo tutorInfo =
        CourseInfoResponse.TutorInfo.builder().userId("tutor1").username("tutor1").build();

    CourseInfoResponse expectedResponse =
        CourseInfoResponse.builder()
            .id(courseId)
            .name("Updated Course")
            .tutor(tutorInfo)
            .totalEnrolled(0L)
            .build();

    Course existingCourseWithNewSyllabus =
        Course.builder().id(courseId).name("Updated Course").syllabus(newSyllabus).build();

    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(existingCourse));
    when(syllabusRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(newSyllabus));
    doNothing().when(courseMapper).updateCourse(any(Course.class), any(CourseUpdateRequest.class));
    when(courseRepository.save(any(Course.class))).thenReturn(existingCourse);
    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(existingCourseWithNewSyllabus));
    when(courseMapper.toCourseInfoResponse(existingCourseWithNewSyllabus))
        .thenReturn(expectedResponse);
    when(classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId))
        .thenReturn(Set.of());
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    CourseInfoResponse result = courseService.updateCourse(courseId, request);

    // Then
    assertNotNull(result);
    verify(syllabusRepository).findByIdAndNotDeleted(2L);
    verify(courseRepository).save(any(Course.class));
    verify(courseRepository, times(2))
        .findByIdAndIsDeletedFalse(courseId); // Once in updateCourse, once in
  }

  @Test
  @DisplayName("Should throw AppException when syllabus not found for update")
  void should_throwAppException_when_syllabusNotFoundForUpdate() {
    // Given
    Long courseId = 1L;
    CourseUpdateRequest request = CourseUpdateRequest.builder().syllabusId(999L).build();

    Course existingCourse = Course.builder().id(courseId).build();

    when(courseRepository.findByIdAndIsDeletedFalse(courseId))
        .thenReturn(Optional.of(existingCourse));
    when(syllabusRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(
            AppException.class,
            () -> {
              courseService.updateCourse(courseId, request);
            });

    assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
    verify(syllabusRepository).findByIdAndNotDeleted(999L);
  }

  @Test
  @DisplayName("Should delete course successfully (soft delete)")
  void should_deleteCourse_successfully() {
    // Given
    Long courseId = 1L;
    Course course = Course.builder().id(courseId).name("Course to Delete").build();
    course.setIsDeleted(false);

    when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(course)).thenReturn(course);

    // When
    courseService.deleteCourse(courseId);

    // Then
    assertTrue(course.getIsDeleted());
    verify(courseRepository).findByIdAndIsDeletedFalse(courseId);
    verify(courseRepository).save(course);
  }

  @Test
  @DisplayName("Should throw AppException when course not found for delete")
  void should_throwAppException_when_courseNotFoundForDelete() {
    // Given
    Long courseId = 999L;
    when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(
            AppException.class,
            () -> {
              courseService.deleteCourse(courseId);
            });

    assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    verify(courseRepository).findByIdAndIsDeletedFalse(courseId);
    verify(courseRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get all courses public")
  void should_getAllCoursesPublic() {
    // Given
    PageRequest pageable =
        PageRequest.of(
            0,
            10,
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "name"));
    Course course = Course.builder().id(1L).name("Course 1").build();
    Page<Course> coursePage = new PageImpl<>(List.of(course));
    CourseInfoResponse response =
        CourseInfoResponse.builder()
            .id(1L)
            .name("Course 1")
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u1").build())
            .build();

    when(courseRepository.searchPublicCourses(any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(coursePage);
    when(courseMapper.toCourseInfoResponse(course)).thenReturn(response);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result = courseService.getAllCoursesPublic(0, 10, "name", "asc", null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  // ==================== Additional tests for getAllCoursesPublic ====================

  @Test
  @DisplayName("Should get all courses public with DESC direction")
  void should_getAllCoursesPublic_withDescDirection() {
    // Given
    Course course = Course.builder().id(1L).name("Course 1").build();
    Page<Course> coursePage = new PageImpl<>(List.of(course));
    CourseInfoResponse response =
        CourseInfoResponse.builder()
            .id(1L)
            .name("Course 1")
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u1").build())
            .build();

    when(courseRepository.searchPublicCourses(any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(coursePage);
    when(courseMapper.toCourseInfoResponse(course)).thenReturn(response);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result = courseService.getAllCoursesPublic(0, 10, "name", "desc", null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());

    // Verify DESC direction was used
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(courseRepository)
        .searchPublicCourses(any(), any(), any(), any(), pageableCaptor.capture());
    Pageable capturedPageable = pageableCaptor.getValue();
    assertEquals(
        Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("name").getDirection());
  }

  @Test
  @DisplayName("Should get all courses public with empty result")
  void should_getAllCoursesPublic_withEmptyResult() {
    // Given
    Page<Course> emptyPage = new PageImpl<>(List.of());

    when(courseRepository.searchPublicCourses(any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(emptyPage);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result = courseService.getAllCoursesPublic(0, 10, "name", "asc", null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertTrue(result.getContent().isEmpty());
  }

  @Test
  @DisplayName("Should get all courses private")
  void should_getAllCoursesPrivate() {
    // Given
    PageRequest pageable =
        PageRequest.of(
            0,
            10,
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "name"));
    Course course = Course.builder().id(1L).name("Course 1").build();
    Page<Course> coursePage = new PageImpl<>(List.of(course));
    CourseInfoResponse response =
        CourseInfoResponse.builder()
            .id(1L)
            .name("Course 1")
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u1").build())
            .build();

    when(courseRepository.searchAdminCourses(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(coursePage);
    when(courseMapper.toCourseInfoResponse(course)).thenReturn(response);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result =
        courseService.getAllCoursesPrivate(
            0, 10, "name", "asc", null, null, null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  // ==================== Additional tests for getAllCoursesPrivate ====================

  @Test
  @DisplayName("Should get all courses private with DESC direction")
  void should_getAllCoursesPrivate_withDescDirection() {
    // Given
    Course course = Course.builder().id(1L).name("Course 1").build();
    Page<Course> coursePage = new PageImpl<>(List.of(course));
    CourseInfoResponse response =
        CourseInfoResponse.builder()
            .id(1L)
            .name("Course 1")
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u1").build())
            .build();

    when(courseRepository.searchAdminCourses(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(coursePage);
    when(courseMapper.toCourseInfoResponse(course)).thenReturn(response);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result =
        courseService.getAllCoursesPrivate(
            0, 10, "name", "desc", null, null, null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());

    // Verify DESC direction was used
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(courseRepository)
        .searchAdminCourses(any(), any(), any(), any(), any(), any(), pageableCaptor.capture());
    Pageable capturedPageable = pageableCaptor.getValue();
    assertEquals(
        Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("name").getDirection());
  }

  @Test
  @DisplayName("Should get all courses private with empty result")
  void should_getAllCoursesPrivate_withEmptyResult() {
    // Given
    Page<Course> emptyPage = new PageImpl<>(List.of());

    when(courseRepository.searchAdminCourses(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(emptyPage);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result =
        courseService.getAllCoursesPrivate(
            0, 10, "name", "asc", null, null, null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertTrue(result.getContent().isEmpty());
  }

  @Test
  @DisplayName("Should get all courses private including deleted courses")
  void should_getAllCoursesPrivate_includingDeletedCourses() {
    // Given
    Course course1 = Course.builder().id(1L).name("Active Course").build();
    Course course2 = Course.builder().id(2L).name("Deleted Course").build();

    Page<Course> coursePage = new PageImpl<>(List.of(course1, course2));

    CourseInfoResponse response1 =
        CourseInfoResponse.builder()
            .id(1L)
            .isDeleted(false)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u1").build())
            .build();
    CourseInfoResponse response2 =
        CourseInfoResponse.builder()
            .id(2L)
            .isDeleted(true)
            .tutor(CourseInfoResponse.TutorInfo.builder().userId("u2").build())
            .build();

    when(courseRepository.searchAdminCourses(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(coursePage);
    when(courseMapper.toCourseInfoResponse(course1)).thenReturn(response1);
    when(courseMapper.toCourseInfoResponse(course2)).thenReturn(response2);
    when(tutorProfileRepository.findAllByUserUserIdIn(anyList())).thenReturn(List.of());

    // When
    var result =
        courseService.getAllCoursesPrivate(
            0, 10, "name", "asc", null, null, null, null, null, null);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    // Verify both active and deleted courses are returned
    assertTrue(result.getContent().stream().anyMatch(c -> Boolean.FALSE.equals(c.getIsDeleted())));
    assertTrue(result.getContent().stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsDeleted())));
  }

  @Test
  @DisplayName("Should get tutors by subject")
  void should_getTutorsBySubject() {
    // Given
    User user = User.builder().userId("u1").firstName("John").lastName("Doe").build();
    com.sep.educonnect.entity.TutorProfile profile =
        com.sep.educonnect.entity.TutorProfile.builder().id(1L).user(user).build();

    when(tutorProfileRepository.findBySubjectId(1L)).thenReturn(List.of(profile));

    // When
    var result = courseService.getTutorsBySubject(1L);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getId());
  }

  @Test
  @DisplayName("Should upload course picture")
  void should_uploadCoursePicture() {
    // Given
    Course course = Course.builder().id(1L).build();
    when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
    when(courseRepository.save(course)).thenReturn(course);

    // When
    var result = courseService.uploadCoursePicture(1L, "url");

    // Then
    assertEquals("url", result);
    assertEquals("url", course.getPictureUrl());
  }

  @Test
  @DisplayName("Should throw COURSE_NOT_EXISTED when uploading picture for non-existent course")
  void should_throwCourseNotExisted_when_uploadCoursePicture() {
    // Given
    Long courseId = 999L;
    when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.empty());

    // When & Then
    AppException exception =
        assertThrows(AppException.class, () -> courseService.uploadCoursePicture(courseId, "url"));
    assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    verify(courseRepository).findByIdAndIsDeletedFalse(courseId);
  }
}
