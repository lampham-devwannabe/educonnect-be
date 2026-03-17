package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.review.CreateCourseReviewRequest;
import com.sep.educonnect.dto.review.CourseReviewDTO;
import com.sep.educonnect.dto.review.CourseReviewSummaryDTO;
import com.sep.educonnect.dto.review.UpdateCourseReviewRequest;
import com.sep.educonnect.entity.ClassEnrollment;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.CourseReview;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassEnrollmentRepository;
import com.sep.educonnect.repository.CourseRepository;
import com.sep.educonnect.repository.CourseReviewRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.CourseReviewService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseReviewService Unit Tests")
class CourseReviewServiceTest {

    @Mock private CourseReviewRepository reviewRepository;

    @Mock private CourseRepository courseRepository;

    @Mock private UserRepository userRepository;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @InjectMocks private CourseReviewService courseReviewService;

    private User student;
    private Course course;
    private CourseReview review;
    private ClassEnrollment enrollment;

    @BeforeEach
    void setUp() {
        student =
                User.builder()
                        .userId("student-1")
                        .username("student")
                        .firstName("John")
                        .lastName("Doe")
                        .avatar("student-avatar.jpg")
                        .build();

        course =
                Course.builder()
                        .id(1L)
                        .name("Java Programming")
                        .build();

        review =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(5)
                        .content("Excellent course!")
                        .build();
        review.setCreatedAt(LocalDateTime.now());
        review.setModifiedAt(LocalDateTime.now());
        review.setCreatedBy("student-1");
        review.setModifiedBy("student-1");

        enrollment =
                ClassEnrollment.builder()
                        .id(1L)
                        .student(student)
                        .build();
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    // ==================== CREATE REVIEW TEST CASES ====================

    @Test
    @DisplayName("CRD01 - Should create review successfully with rating 5 and content")
    void should_createReview_withRating5AndContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(1L)
                        .rating(5)
                        .content("Great course!")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id(
                        "student-1", 1L))
                .thenReturn(Optional.of(enrollment));
        when(reviewRepository.existsByStudentIdAndCourse_Id("student-1", 1L))
                .thenReturn(false);
        when(reviewRepository.save(any(CourseReview.class))).thenAnswer(invocation -> {
            CourseReview savedReview = invocation.getArgument(0);
            savedReview.setId(1L);
            savedReview.setCreatedAt(LocalDateTime.now());
            savedReview.setModifiedAt(LocalDateTime.now());
            savedReview.setCreatedBy("student-1");
            savedReview.setModifiedBy("student-1");
            return savedReview;
        });

        // When
        CourseReviewDTO result = courseReviewService.createReview(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getCourseId());
        assertEquals(5, result.getRating());
        assertEquals("Great course!", result.getContent());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("CRD02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenStudentNotLoggedIn() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(1L)
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.createReview(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("CRD03 - Should create review successfully with rating 1 and empty content")
    void should_createReview_withRating1AndEmptyContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(1L)
                        .rating(1)
                        .content("")
                        .build();

        CourseReview reviewWith1Star =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(1)
                        .content("")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id(
                        "student-1", 1L))
                .thenReturn(Optional.of(enrollment));
        when(reviewRepository.existsByStudentIdAndCourse_Id("student-1", 1L))
                .thenReturn(false);
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(reviewWith1Star);

        // When
        CourseReviewDTO result = courseReviewService.createReview(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getCourseId());
        assertEquals(1, result.getRating());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("CRD04 - Should throw COURSE_NOT_EXISTED when course not found")
    void should_throwCourseNotExisted_whenCourseNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(999L)
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.createReview(request));
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "CRD05 - Should throw STUDENT_NOT_ENROLLED_WITH_TUTOR when student not enrolled in course")
    void should_throwStudentNotEnrolled_whenNotEnrolledInCourse() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(1L)
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id(
                        "student-1", 1L))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.createReview(request));
        assertEquals(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("CRD06 - Should throw ALREADY_RATED when student already reviewed this course")
    void should_throwAlreadyRated_whenStudentAlreadyReviewedCourse() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateCourseReviewRequest request =
                CreateCourseReviewRequest.builder()
                        .courseId(1L)
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(classEnrollmentRepository.findByStudent_UserIdAndTutorClass_Course_Id(
                        "student-1", 1L))
                .thenReturn(Optional.of(enrollment));
        when(reviewRepository.existsByStudentIdAndCourse_Id("student-1", 1L))
                .thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.createReview(request));
        assertEquals(ErrorCode.ALREADY_RATED, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("URD01 - Should update review successfully with rating 5 and content")
    void should_updateReview_withRating5AndContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(5).content("Updated content").build();

        CourseReview updatedReview =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(5)
                        .content("Updated content")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(updatedReview);

        // When
        CourseReviewDTO result = courseReviewService.updateReview(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Updated content", result.getContent());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("URD02 - Should update review successfully with rating only")
    void should_updateReview_withRatingOnly() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(3).content(null).build();

        CourseReview updatedReview =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(3)
                        .content("Excellent course!")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(updatedReview);

        // When
        CourseReviewDTO result = courseReviewService.updateReview(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getRating());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("URD03 - Should update review successfully with content only")
    void should_updateReview_withContentOnly() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(null).content("New content").build();

        CourseReview updatedReview =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(5)
                        .content("New content")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(updatedReview);

        // When
        CourseReviewDTO result = courseReviewService.updateReview(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("New content", result.getContent());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("URD04 - Should update review successfully with empty request")
    void should_updateReview_withEmptyRequest() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(null).content(null).build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(review);

        // When
        CourseReviewDTO result = courseReviewService.updateReview(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        verify(reviewRepository).save(any(CourseReview.class));
    }

    @Test
    @DisplayName("URD05 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenUpdatingWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(5).content("abc").build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> courseReviewService.updateReview(1L, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("URD06 - Should throw RATING_NOT_FOUND when review does not exist")
    void should_throwRatingNotFound_whenReviewDoesNotExist() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(4).content("abc").build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> courseReviewService.updateReview(999L, request));
        assertEquals(ErrorCode.RATING_NOT_FOUND, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("URD07 - Should throw UNAUTHORIZED when updating another user's review")
    void should_throwUnauthorized_whenUpdatingAnotherUsersReview() {
        // Given
        MockHelper.mockSecurityContext("student2");
        User anotherStudent =
                User.builder()
                        .userId("student-2")
                        .username("student2")
                        .firstName("Jane")
                        .lastName("Smith")
                        .build();
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(5).content("abc").build();

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(anotherStudent));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> courseReviewService.updateReview(1L, request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    // ==================== DELETE REVIEW TEST CASES ====================

    @Test
    @DisplayName("DRD01 - Should delete review successfully when student owns the review")
    void should_deleteReview_successfully() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // When
        courseReviewService.deleteReview(1L);

        // Then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("DRD02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenDeletingWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.deleteReview(1L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DRD03 - Should throw RATING_NOT_FOUND when review does not exist")
    void should_throwRatingNotFound_whenDeletingNonexistentReview() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.deleteReview(999L));
        assertEquals(ErrorCode.RATING_NOT_FOUND, exception.getErrorCode());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DRD04 - Should throw UNAUTHORIZED when deleting another user's review")
    void should_throwUnauthorized_whenDeletingAnotherUsersReview() {
        // Given
        MockHelper.mockSecurityContext("student2");
        User anotherStudent =
                User.builder()
                        .userId("student-2")
                        .username("student2")
                        .firstName("Jane")
                        .lastName("Smith")
                        .build();

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(anotherStudent));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.deleteReview(1L));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(reviewRepository, never()).delete(any());
    }

    // ==================== GET COURSE REVIEWS TEST CASES ====================

    @Test
    @DisplayName("GCRD01 - Should get course reviews with default pagination (page 0, size 10)")
    void should_getCourseReviews_withDefaultPagination() {
        // Given
        List<CourseReview> reviews = Arrays.asList(review);
        Pageable pageable = PageRequest.of(0, 10);
        Page<CourseReview> reviewPage = new PageImpl<>(reviews, pageable, 1);

        when(reviewRepository.findByCourse_Id(1L, pageable)).thenReturn(reviewPage);
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));

        // When
        Page<CourseReviewDTO> result = courseReviewService.getCourseReviews(1L, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getCourseId());
        assertEquals(5, result.getContent().get(0).getRating());
        verify(reviewRepository).findByCourse_Id(1L, pageable);
    }

    @Test
    @DisplayName("GCRD02 - Should get empty page when course has no reviews")
    void should_getCourseReviews_emptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CourseReview> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(reviewRepository.findByCourse_Id(999L, pageable)).thenReturn(emptyPage);

        // When
        Page<CourseReviewDTO> result = courseReviewService.getCourseReviews(999L, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("GCRD03 - Should get course reviews with custom page size")
    void should_getCourseReviews_withCustomPageSize() {
        // Given
        CourseReview review2 =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-2")
                        .course(course)
                        .rating(4)
                        .content("Good")
                        .build();

        CourseReview review3 =
                CourseReview.builder()
                        .id(3L)
                        .studentId("student-3")
                        .course(course)
                        .rating(3)
                        .content("Average")
                        .build();

        List<CourseReview> reviews = Arrays.asList(review, review2, review3);
        Pageable pageable = PageRequest.of(0, 3);
        Page<CourseReview> reviewPage = new PageImpl<>(reviews, pageable, 3);

        when(reviewRepository.findByCourse_Id(1L, pageable)).thenReturn(reviewPage);
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-2")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-3")).thenReturn(Optional.of(student));

        // When
        Page<CourseReviewDTO> result = courseReviewService.getCourseReviews(1L, 0, 3);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
    }

    @Test
    @DisplayName("GCRS01 - Should get course review summary with mixed ratings")
    void should_getCourseReviewSummary_withMixedRatings() {
        // Given
        CourseReview review2 =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-2")
                        .course(course)
                        .rating(4)
                        .content("Good!")
                        .build();

        CourseReview review3 =
                CourseReview.builder()
                        .id(3L)
                        .studentId("student-3")
                        .course(course)
                        .rating(3)
                        .content("Average")
                        .build();

        List<CourseReview> allReviews = Arrays.asList(review, review2, review3);

        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(reviewRepository.findByCourse_Id(1L)).thenReturn(allReviews);
        when(reviewRepository.countByCourse_Id(1L)).thenReturn(3L);
        when(reviewRepository.findTop5ByCourse_Id(1L))
                .thenReturn(Arrays.asList(review, review2, review3));
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-2")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-3")).thenReturn(Optional.of(student));

        // When
        CourseReviewSummaryDTO result = courseReviewService.getCourseReviewSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getCourseId());
        assertEquals("Java Programming", result.getCourseName());
        assertEquals(4.0, result.getAverageRating(), 0.1);
        assertEquals(3L, result.getTotalReviews());
        assertEquals(1, result.getFiveStarCount());
        assertEquals(1, result.getFourStarCount());
        assertEquals(1, result.getThreeStarCount());
        assertEquals(0, result.getTwoStarCount());
        assertEquals(0, result.getOneStarCount());
        assertEquals(3, result.getRecentReviews().size());
        verify(courseRepository).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("GCRS02 - Should get course review summary with no reviews")
    void should_getCourseReviewSummary_withNoReviews() {
        // Given
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(reviewRepository.findByCourse_Id(1L)).thenReturn(Collections.emptyList());
        when(reviewRepository.countByCourse_Id(1L)).thenReturn(0L);
        when(reviewRepository.findTop5ByCourse_Id(1L)).thenReturn(Collections.emptyList());

        // When
        CourseReviewSummaryDTO result = courseReviewService.getCourseReviewSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getCourseId());
        assertEquals("Java Programming", result.getCourseName());
        assertEquals(0.0, result.getAverageRating());
        assertEquals(0L, result.getTotalReviews());
        assertEquals(0, result.getFiveStarCount());
        assertEquals(0, result.getFourStarCount());
        assertEquals(0, result.getThreeStarCount());
        assertEquals(0, result.getTwoStarCount());
        assertEquals(0, result.getOneStarCount());
        assertEquals(0.0, result.getFiveStarPercentage());
        assertEquals(0, result.getRecentReviews().size());
    }

    @Test
    @DisplayName("GCRS03 - Should throw COURSE_NOT_EXISTED when course not found")
    void should_throwCourseNotExisted_whenGettingSummaryForNonexistentCourse() {
        // Given
        when(courseRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> courseReviewService.getCourseReviewSummary(999L));
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("GCRS04 - Should get course review summary with only 5-star ratings")
    void should_getCourseReviewSummary_withOnly5StarRatings() {
        // Given
        CourseReview review2 =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-2")
                        .course(course)
                        .rating(5)
                        .content("Excellent!")
                        .build();

        List<CourseReview> allReviews = Arrays.asList(review, review2);

        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(reviewRepository.findByCourse_Id(1L)).thenReturn(allReviews);
        when(reviewRepository.countByCourse_Id(1L)).thenReturn(2L);
        when(reviewRepository.findTop5ByCourse_Id(1L))
                .thenReturn(Arrays.asList(review, review2));
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-2")).thenReturn(Optional.of(student));

        // When
        CourseReviewSummaryDTO result = courseReviewService.getCourseReviewSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(5.0, result.getAverageRating());
        assertEquals(2L, result.getTotalReviews());
        assertEquals(2, result.getFiveStarCount());
        assertEquals(0, result.getFourStarCount());
        assertEquals(100.0, result.getFiveStarPercentage());
        assertEquals(0.0, result.getFourStarPercentage());
    }

    @Test
    @DisplayName("GCRS05 - Should get course review summary with all star distributions")
    void should_getCourseReviewSummary_withAllStarDistributions() {
        // Given
        CourseReview review2 =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-2")
                        .course(course)
                        .rating(4)
                        .content("Good")
                        .build();

        CourseReview review3 =
                CourseReview.builder()
                        .id(3L)
                        .studentId("student-3")
                        .course(course)
                        .rating(3)
                        .content("Average")
                        .build();

        CourseReview review4 =
                CourseReview.builder()
                        .id(4L)
                        .studentId("student-4")
                        .course(course)
                        .rating(2)
                        .content("Below average")
                        .build();

        CourseReview review5 =
                CourseReview.builder()
                        .id(5L)
                        .studentId("student-5")
                        .course(course)
                        .rating(1)
                        .content("Poor")
                        .build();

        List<CourseReview> allReviews =
                Arrays.asList(review, review2, review3, review4, review5);

        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(reviewRepository.findByCourse_Id(1L)).thenReturn(allReviews);
        when(reviewRepository.countByCourse_Id(1L)).thenReturn(5L);
        when(reviewRepository.findTop5ByCourse_Id(1L))
                .thenReturn(allReviews);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(student));

        // When
        CourseReviewSummaryDTO result = courseReviewService.getCourseReviewSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(3.0, result.getAverageRating());
        assertEquals(5L, result.getTotalReviews());
        assertEquals(1, result.getFiveStarCount());
        assertEquals(1, result.getFourStarCount());
        assertEquals(1, result.getThreeStarCount());
        assertEquals(1, result.getTwoStarCount());
        assertEquals(1, result.getOneStarCount());
        assertEquals(20.0, result.getFiveStarPercentage());
        assertEquals(20.0, result.getFourStarPercentage());
        assertEquals(20.0, result.getThreeStarPercentage());
        assertEquals(20.0, result.getTwoStarPercentage());
        assertEquals(20.0, result.getOneStarPercentage());
        assertEquals(5, result.getRecentReviews().size());
    }

    // ==================== GET MY REVIEWS TEST CASES ====================

    @Test
    @DisplayName("GMRD01 - Should get my reviews successfully with single review")
    void should_getMyReviews_withSingleReview() {
        // Given
        MockHelper.mockSecurityContext("student");
        List<CourseReview> reviews = Arrays.asList(review);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findByStudentId("student-1")).thenReturn(reviews);
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));

        // When
        List<CourseReviewDTO> result = courseReviewService.getMyReviews();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCourseId());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Excellent course!", result.get(0).getContent());
        verify(userRepository).findByUsername("student");
        verify(reviewRepository).findByStudentId("student-1");
    }

    @Test
    @DisplayName("GMRD02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenGettingMyReviewsWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> courseReviewService.getMyReviews());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(reviewRepository, never()).findByStudentId(any());
    }

    @Test
    @DisplayName("GMRD03 - Should get empty list when student has no reviews")
    void should_getMyReviews_emptyList() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findByStudentId("student-1"))
                .thenReturn(Collections.emptyList());

        // When
        List<CourseReviewDTO> result = courseReviewService.getMyReviews();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("GMRD04 - Should get my reviews with multiple reviews")
    void should_getMyReviews_withMultipleReviews() {
        // Given
        MockHelper.mockSecurityContext("student");

        Course course2 = Course.builder().id(2L).name("Python Programming").build();
        CourseReview review2 =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-1")
                        .course(course2)
                        .rating(4)
                        .content("Good course")
                        .build();

        Course course3 = Course.builder().id(3L).name("Web Development").build();
        CourseReview review3 =
                CourseReview.builder()
                        .id(3L)
                        .studentId("student-1")
                        .course(course3)
                        .rating(3)
                        .content("Average")
                        .build();

        List<CourseReview> reviews = Arrays.asList(review, review2, review3);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findByStudentId("student-1")).thenReturn(reviews);
        when(userRepository.findById("student-1")).thenReturn(Optional.of(student));

        // When
        List<CourseReviewDTO> result = courseReviewService.getMyReviews();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getCourseId());
        assertEquals(2L, result.get(1).getCourseId());
        assertEquals(3L, result.get(2).getCourseId());
        assertEquals(5, result.get(0).getRating());
        assertEquals(4, result.get(1).getRating());
        assertEquals(3, result.get(2).getRating());
    }

    @Test
    @DisplayName("GMRD05 - Should handle convertToDTO when student not found in getMyReviews")
    void should_getMyReviews_whenStudentNotFoundInConvert() {
        // Given
        MockHelper.mockSecurityContext("student");
        CourseReview reviewWithoutStudent =
                CourseReview.builder()
                        .id(2L)
                        .studentId("student-1")
                        .course(course)
                        .rating(4)
                        .content("Good")
                        .build();

        List<CourseReview> reviews = Arrays.asList(reviewWithoutStudent);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findByStudentId("student-1")).thenReturn(reviews);
        when(userRepository.findById("student-1")).thenReturn(Optional.empty());

        // When
        List<CourseReviewDTO> result = courseReviewService.getMyReviews();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getStudentName());
        assertNull(result.get(0).getStudentAvatar());
    }

    @Test
    @DisplayName("URD08 - Should update review with empty string content")
    void should_updateReview_withEmptyStringContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateCourseReviewRequest request =
                UpdateCourseReviewRequest.builder().rating(null).content("").build();

        CourseReview updatedReview =
                CourseReview.builder()
                        .id(1L)
                        .studentId("student-1")
                        .course(course)
                        .rating(5)
                        .content("")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(updatedReview);

        // When
        CourseReviewDTO result = courseReviewService.updateReview(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("", result.getContent());
        verify(reviewRepository).save(any(CourseReview.class));
    }

}
