package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.rating.CreateTutorRatingRequest;
import com.sep.educonnect.dto.rating.TutorRatingDTO;
import com.sep.educonnect.dto.rating.TutorRatingSummaryDTO;
import com.sep.educonnect.dto.rating.UpdateTutorRatingRequest;
import com.sep.educonnect.entity.TutorRating;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassEnrollmentRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import com.sep.educonnect.repository.TutorRatingRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.TutorRatingService;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TutorRatingService Unit Tests")
class TutorRatingServiceTest {

    @Mock private TutorRatingRepository ratingRepository;

    @Mock private UserRepository userRepository;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @InjectMocks private TutorRatingService tutorRatingService;

    @Mock private TutorProfileRepository tutorProfileRepository;

    private User student;
    private User tutor;
    private TutorRating rating;

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

        tutor =
                User.builder()
                        .userId("tutor-1")
                        .username("tutor")
                        .firstName("Alice")
                        .lastName("Smith")
                        .avatar("tutor-avatar.jpg")
                        .build();

        rating = new TutorRating();
        rating.setRatingId(1L);
        rating.setTutor(tutor);
        rating.setStudent(student);
        rating.setRating(5);
        rating.setContent("Excellent tutor!");
        rating.setCreatedAt(LocalDateTime.now());
        rating.setModifiedAt(LocalDateTime.now());

        // By default, provide a TutorProfile for any tutor id so service's profile update won't throw
        when(tutorProfileRepository.findByUserUserId(anyString()))
                .thenAnswer(invocation -> {
                    String tutorId = invocation.getArgument(0);
                    TutorProfile profile = TutorProfile.builder().user(User.builder().userId(tutorId).build()).build();
                    return Optional.of(profile);
                });
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    // ==================== CREATE RATING TEST CASES ====================

    @Test
    @DisplayName("UTD01 - Should create rating successfully with rating 5 and content")
    void should_createRating_withRating5AndContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-1")
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student-1", "tutor-1"))
                .thenReturn(true);
        when(ratingRepository.existsByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(false);
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(rating);

        // When
        TutorRatingDTO result = tutorRatingService.createRating(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getRatingId());
        assertEquals("tutor-1", result.getTutorId());
        assertEquals(5, result.getRating());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName("UTD02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenStudentNotLoggedIn() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-1")
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.createRating(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD03 - Should create rating successfully with rating 2 and empty content")
    void should_createRating_withRating2AndEmptyContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder().tutorId("tutor-1").rating(2).content("").build();

        TutorRating ratingWith2Stars = new TutorRating();
        ratingWith2Stars.setRatingId(1L);
        ratingWith2Stars.setTutor(tutor);
        ratingWith2Stars.setStudent(student);
        ratingWith2Stars.setRating(2);
        ratingWith2Stars.setContent("");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student-1", "tutor-1"))
                .thenReturn(true);
        when(ratingRepository.existsByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(false);
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(ratingWith2Stars);

        // When
        TutorRatingDTO result = tutorRatingService.createRating(request);

        // Then
        assertNotNull(result);
        assertEquals("tutor-1", result.getTutorId());
        assertEquals(2, result.getRating());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName("UTD04 - Should create rating successfully with different tutor")
    void should_createRating_withDifferentTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        User tutor2 =
                User.builder()
                        .userId("tutor-2")
                        .username("tutor2")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .avatar("tutor2-avatar.jpg")
                        .build();

        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-2")
                        .rating(3)
                        .content("abc")
                        .build();

        TutorRating ratingForTutor2 = new TutorRating();
        ratingForTutor2.setRatingId(2L);
        ratingForTutor2.setTutor(tutor2);
        ratingForTutor2.setStudent(student);
        ratingForTutor2.setRating(3);
        ratingForTutor2.setContent("abc");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-2")).thenReturn(Optional.of(tutor2));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student-1", "tutor-2"))
                .thenReturn(true);
        when(ratingRepository.existsByStudent_UserIdAndTutor_UserId("student-1", "tutor-2"))
                .thenReturn(false);
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(ratingForTutor2);

        // When
        TutorRatingDTO result = tutorRatingService.createRating(request);

        // Then
        assertNotNull(result);
        assertEquals("tutor-2", result.getTutorId());
        assertEquals(3, result.getRating());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName("UTD05 - Should throw USER_NOT_EXISTED when tutor not found")
    void should_throwUserNotExisted_whenTutorNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-999")
                        .rating(4)
                        .content("")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-999")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.createRating(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "UTD11 - Should throw STUDENT_NOT_ENROLLED_WITH_TUTOR when student never studied with tutor")
    void should_throwStudentNotEnrolled_whenNeverStudiedWithTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-1")
                        .rating(3)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student-1", "tutor-1"))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.createRating(request));
        assertEquals(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD12 - Should throw ALREADY_RATED when student already rated this tutor")
    void should_throwAlreadyRated_whenStudentAlreadyRatedTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        CreateTutorRatingRequest request =
                CreateTutorRatingRequest.builder()
                        .tutorId("tutor-1")
                        .rating(5)
                        .content("abc")
                        .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(classEnrollmentRepository.existsByStudentUserIdAndTutorUserId("student-1", "tutor-1"))
                .thenReturn(true);
        when(ratingRepository.existsByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.createRating(request));
        assertEquals(ErrorCode.ALREADY_RATED, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    // ==================== UPDATE RATING TEST CASES ====================

    @Test
    @DisplayName("UTD01 - Should update rating successfully with rating 5 and content abc")
    void should_updateRating_withRating5AndContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(5).content("abc").build();

        TutorRating updatedRating = new TutorRating();
        updatedRating.setRatingId(1L);
        updatedRating.setTutor(tutor);
        updatedRating.setStudent(student);
        updatedRating.setRating(5);
        updatedRating.setContent("abc");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(updatedRating);

        // When
        TutorRatingDTO result = tutorRatingService.updateRating(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("abc", result.getContent());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName("UTD02 - Should update rating successfully with rating 1 and empty content")
    void should_updateRating_withRating1AndEmptyContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(1).content("").build();

        TutorRating updatedRating = new TutorRating();
        updatedRating.setRatingId(1L);
        updatedRating.setTutor(tutor);
        updatedRating.setStudent(student);
        updatedRating.setRating(1);
        updatedRating.setContent("");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(updatedRating);

        // When
        TutorRatingDTO result = tutorRatingService.updateRating(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRating());
        assertEquals("", result.getContent());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName(
            "UTD03 - Should update rating successfully with rating 2 and content 2000 characters")
    void should_updateRating_withRating2AndContent2000Chars() {
        // Given
        MockHelper.mockSecurityContext("student");
        String content2000 = "a".repeat(2000);
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(2).content(content2000).build();

        TutorRating updatedRating = new TutorRating();
        updatedRating.setRatingId(1L);
        updatedRating.setTutor(tutor);
        updatedRating.setStudent(student);
        updatedRating.setRating(2);
        updatedRating.setContent(content2000);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));
        when(ratingRepository.save(any(TutorRating.class))).thenReturn(updatedRating);

        // When
        TutorRatingDTO result = tutorRatingService.updateRating(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getRating());
        assertEquals(2000, result.getContent().length());
        verify(ratingRepository).save(any(TutorRating.class));
    }

    @Test
    @DisplayName("UTD05 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenUpdatingWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(5).content("abc").build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorRatingService.updateRating(1L, request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD06 - Should throw RATING_NOT_FOUND when rating does not exist")
    void should_throwRatingNotFound_whenRatingDoesNotExist() {
        // Given
        MockHelper.mockSecurityContext("student");
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(4).content("abc").build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorRatingService.updateRating(999L, request));
        assertEquals(ErrorCode.RATING_NOT_FOUND, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTD11 - Should throw UNAUTHORIZED when updating another user's rating")
    void should_throwUnauthorized_whenUpdatingAnotherUsersRating() {
        // Given
        MockHelper.mockSecurityContext("student2");
        User anotherStudent =
                User.builder()
                        .userId("student-2")
                        .username("student2")
                        .firstName("Jane")
                        .lastName("Smith")
                        .build();
        UpdateTutorRatingRequest request =
                UpdateTutorRatingRequest.builder().rating(5).content("abc").build();

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(anotherStudent));
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorRatingService.updateRating(1L, request));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(ratingRepository, never()).save(any());
    }

    // ==================== DELETE RATING TEST CASES ====================

    @Test
    @DisplayName("DRD01 - Should delete rating successfully when student owns the rating")
    void should_deleteRating_successfully() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));

        // When
        tutorRatingService.deleteRating(1L);

        // Then
        verify(ratingRepository).delete(rating);
    }

    @Test
    @DisplayName("DRD02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenDeletingWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.deleteRating(1L));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DRD03 - Should throw RATING_NOT_FOUND when rating does not exist")
    void should_throwRatingNotFound_whenDeletingNonexistentRating() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.deleteRating(999L));
        assertEquals(ErrorCode.RATING_NOT_FOUND, exception.getErrorCode());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DRD04 - Should throw UNAUTHORIZED when deleting another user's rating")
    void should_throwUnauthorized_whenDeletingAnotherUsersRating() {
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
        when(ratingRepository.findById(1L)).thenReturn(Optional.of(rating));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.deleteRating(1L));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(ratingRepository, never()).delete(any());
    }

    // ==================== GET TUTOR RATINGS TEST CASES ====================

    @Test
    @DisplayName("GTR01 - Should get tutor ratings with default pagination (page 0, size 10)")
    void should_getTutorRatings_withDefaultPagination() {
        // Given
        List<TutorRating> ratings = Arrays.asList(rating);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TutorRating> ratingPage = new PageImpl<>(ratings, pageable, 1);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-1", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("tutor-1", result.getContent().get(0).getTutorId());
        assertEquals(5, result.getContent().get(0).getRating());
        verify(ratingRepository).findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable);
    }

    @Test
    @DisplayName("GTR02 - Should get empty page when tutor has no ratings")
    void should_getTutorRatings_emptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<TutorRating> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-999", pageable))
                .thenReturn(emptyPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-999", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("GTR03 - Should get tutor ratings with custom page size")
    void should_getTutorRatings_withCustomPageSize() {
        // Given
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good");

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor);
        rating3.setStudent(student);
        rating3.setRating(3);
        rating3.setContent("Average");

        List<TutorRating> ratings = Arrays.asList(rating, rating2, rating3);
        Pageable pageable = PageRequest.of(0, 3);
        Page<TutorRating> ratingPage = new PageImpl<>(ratings, pageable, 3);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-1", 0, 3);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getRatingId());
        assertEquals(2L, result.getContent().get(1).getRatingId());
        assertEquals(3L, result.getContent().get(2).getRatingId());
    }

    @Test
    @DisplayName("GTR04 - Should get second page of tutor ratings")
    void should_getTutorRatings_secondPage() {
        // Given
        TutorRating rating4 = new TutorRating();
        rating4.setRatingId(4L);
        rating4.setTutor(tutor);
        rating4.setStudent(student);
        rating4.setRating(2);
        rating4.setContent("Below average");

        List<TutorRating> secondPageRatings = Arrays.asList(rating4);
        Pageable pageable = PageRequest.of(1, 3);
        Page<TutorRating> ratingPage = new PageImpl<>(secondPageRatings, pageable, 4);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-1", 1, 3);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(4L, result.getContent().get(0).getRatingId());
        assertEquals(1, result.getNumber()); // Current page number
    }

    @Test
    @DisplayName("GTR05 - Should get tutor ratings with page size of 1")
    void should_getTutorRatings_withPageSize1() {
        // Given
        List<TutorRating> ratings = Arrays.asList(rating);
        Pageable pageable = PageRequest.of(0, 1);
        Page<TutorRating> ratingPage = new PageImpl<>(ratings, pageable, 5);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-1", 0, 1);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(5, result.getTotalPages()); // 5 total elements / 1 per page = 5 pages
    }

    @Test
    @DisplayName("GTR06 - Should get ratings for different tutor")
    void should_getTutorRatings_forDifferentTutor() {
        // Given
        User tutor2 =
                User.builder()
                        .userId("tutor-2")
                        .username("tutor2")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .avatar("tutor2-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor2);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Great teacher");

        List<TutorRating> ratings = Arrays.asList(rating2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TutorRating> ratingPage = new PageImpl<>(ratings, pageable, 1);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-2", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-2", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("tutor-2", result.getContent().get(0).getTutorId());
        assertEquals(4, result.getContent().get(0).getRating());
    }

    @Test
    @DisplayName("GTR07 - Should get tutor ratings with large page size")
    void should_getTutorRatings_withLargePageSize() {
        // Given
        List<TutorRating> ratings = Arrays.asList(rating);
        Pageable pageable = PageRequest.of(0, 100);
        Page<TutorRating> ratingPage = new PageImpl<>(ratings, pageable, 1);

        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1", pageable))
                .thenReturn(ratingPage);

        // When
        Page<TutorRatingDTO> result = tutorRatingService.getTutorRatings("tutor-1", 0, 100);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalPages()); // Only 1 page needed
    }

    // ==================== GET TUTOR RATING SUMMARY TEST CASES ====================

    @Test
    @DisplayName("GTRS01 - Should get tutor rating summary with mixed ratings")
    void should_getTutorRatingSummary_withMixedRatings() {
        // Given
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good!");

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor);
        rating3.setStudent(student);
        rating3.setRating(3);
        rating3.setContent("Average");

        List<TutorRating> allRatings = Arrays.asList(rating, rating2, rating3);

        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(4.0);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(3L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals("tutor-1", result.getTutorId());
        assertEquals("Alice Smith", result.getTutorName());
        assertEquals(4.0, result.getAverageRating());
        assertEquals(3L, result.getTotalRatings());
        assertEquals(1, result.getFiveStarCount());
        assertEquals(1, result.getFourStarCount());
        assertEquals(1, result.getThreeStarCount());
        assertEquals(0, result.getTwoStarCount());
        assertEquals(0, result.getOneStarCount());
        assertEquals(3, result.getRecentRatings().size());
        verify(userRepository).findById("tutor-1");
    }

    @Test
    @DisplayName("GTRS02 - Should get tutor rating summary with no ratings")
    void should_getTutorRatingSummary_withNoRatings() {
        // Given
        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(Collections.emptyList());
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(null);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(0L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(Collections.emptyList());

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals("tutor-1", result.getTutorId());
        assertEquals("Alice Smith", result.getTutorName());
        assertEquals(0.0, result.getAverageRating());
        assertEquals(0L, result.getTotalRatings());
        assertEquals(0, result.getFiveStarCount());
        assertEquals(0, result.getFourStarCount());
        assertEquals(0, result.getThreeStarCount());
        assertEquals(0, result.getTwoStarCount());
        assertEquals(0, result.getOneStarCount());
        assertEquals(0.0, result.getFiveStarPercentage());
        assertEquals(0, result.getRecentRatings().size());
    }

    @Test
    @DisplayName("GTRS03 - Should throw USER_NOT_EXISTED when tutor not found")
    void should_throwUserNotExisted_whenGettingSummaryForNonexistentTutor() {
        // Given
        when(userRepository.findById("tutor-999")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorRatingService.getTutorRatingSummary("tutor-999"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("GTRS04 - Should get tutor rating summary with only 5-star ratings")
    void should_getTutorRatingSummary_withOnly5StarRatings() {
        // Given
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(5);
        rating2.setContent("Excellent!");

        List<TutorRating> allRatings = Arrays.asList(rating, rating2);

        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(5.0);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(2L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(5.0, result.getAverageRating());
        assertEquals(2L, result.getTotalRatings());
        assertEquals(2, result.getFiveStarCount());
        assertEquals(0, result.getFourStarCount());
        assertEquals(0, result.getThreeStarCount());
        assertEquals(0, result.getTwoStarCount());
        assertEquals(0, result.getOneStarCount());
        assertEquals(100.0, result.getFiveStarPercentage());
        assertEquals(0.0, result.getFourStarPercentage());
    }

    @Test
    @DisplayName("GTRS05 - Should get tutor rating summary with all star distributions")
    void should_getTutorRatingSummary_withAllStarDistributions() {
        // Given
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good");

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor);
        rating3.setStudent(student);
        rating3.setRating(3);
        rating3.setContent("Average");

        TutorRating rating4 = new TutorRating();
        rating4.setRatingId(4L);
        rating4.setTutor(tutor);
        rating4.setStudent(student);
        rating4.setRating(2);
        rating4.setContent("Below average");

        TutorRating rating5 = new TutorRating();
        rating5.setRatingId(5L);
        rating5.setTutor(tutor);
        rating5.setStudent(student);
        rating5.setRating(1);
        rating5.setContent("Poor");

        List<TutorRating> allRatings = Arrays.asList(rating, rating2, rating3, rating4, rating5);

        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(3.0);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(5L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(3.0, result.getAverageRating());
        assertEquals(5L, result.getTotalRatings());
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
        assertEquals(5, result.getRecentRatings().size());
    }

    @Test
    @DisplayName("GTRS06 - Should get tutor rating summary with more than 5 ratings (show top 5)")
    void should_getTutorRatingSummary_withMoreThan5Ratings() {
        // Given
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(4);

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor);
        rating3.setStudent(student);
        rating3.setRating(5);

        TutorRating rating4 = new TutorRating();
        rating4.setRatingId(4L);
        rating4.setTutor(tutor);
        rating4.setStudent(student);
        rating4.setRating(3);

        TutorRating rating5 = new TutorRating();
        rating5.setRatingId(5L);
        rating5.setTutor(tutor);
        rating5.setStudent(student);
        rating5.setRating(4);

        TutorRating rating6 = new TutorRating();
        rating6.setRatingId(6L);
        rating6.setTutor(tutor);
        rating6.setStudent(student);
        rating6.setRating(5);

        TutorRating rating7 = new TutorRating();
        rating7.setRatingId(7L);
        rating7.setTutor(tutor);
        rating7.setStudent(student);
        rating7.setRating(5);

        List<TutorRating> allRatings =
                Arrays.asList(rating, rating2, rating3, rating4, rating5, rating6, rating7);
        List<TutorRating> top5Ratings = Arrays.asList(rating, rating2, rating3, rating4, rating5);

        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(4.3);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(7L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(top5Ratings);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(7L, result.getTotalRatings());
        assertEquals(5, result.getRecentRatings().size()); // Should only return 5 most recent
        assertEquals(4, result.getFiveStarCount()); // 4 five-star ratings
        assertEquals(2, result.getFourStarCount()); // 2 four-star ratings
        assertEquals(1, result.getThreeStarCount()); // 1 three-star rating
    }

    @Test
    @DisplayName("GTRS07 - Should calculate percentages correctly")
    void should_getTutorRatingSummary_withCorrectPercentages() {
        // Given: 10 ratings - 5 five-stars (50%), 3 four-stars (30%), 2 three-stars (20%)
        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(student);
        rating2.setRating(5);

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor);
        rating3.setStudent(student);
        rating3.setRating(5);

        TutorRating rating4 = new TutorRating();
        rating4.setRatingId(4L);
        rating4.setTutor(tutor);
        rating4.setStudent(student);
        rating4.setRating(5);

        TutorRating rating5 = new TutorRating();
        rating5.setRatingId(5L);
        rating5.setTutor(tutor);
        rating5.setStudent(student);
        rating5.setRating(5);

        TutorRating rating6 = new TutorRating();
        rating6.setRatingId(6L);
        rating6.setTutor(tutor);
        rating6.setStudent(student);
        rating6.setRating(4);

        TutorRating rating7 = new TutorRating();
        rating7.setRatingId(7L);
        rating7.setTutor(tutor);
        rating7.setStudent(student);
        rating7.setRating(4);

        TutorRating rating8 = new TutorRating();
        rating8.setRatingId(8L);
        rating8.setTutor(tutor);
        rating8.setStudent(student);
        rating8.setRating(4);

        TutorRating rating9 = new TutorRating();
        rating9.setRatingId(9L);
        rating9.setTutor(tutor);
        rating9.setStudent(student);
        rating9.setRating(3);

        TutorRating rating10 = new TutorRating();
        rating10.setRatingId(10L);
        rating10.setTutor(tutor);
        rating10.setStudent(student);
        rating10.setRating(3);

        List<TutorRating> allRatings =
                Arrays.asList(
                        rating, rating2, rating3, rating4, rating5, rating6, rating7, rating8,
                        rating9, rating10);
        List<TutorRating> top5 = Arrays.asList(rating, rating2, rating3, rating4, rating5);

        when(userRepository.findById("tutor-1")).thenReturn(Optional.of(tutor));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-1")).thenReturn(4.2);
        when(ratingRepository.countByTutor_UserId("tutor-1")).thenReturn(10L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-1"))
                .thenReturn(top5);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalRatings());
        assertEquals(5, result.getFiveStarCount());
        assertEquals(3, result.getFourStarCount());
        assertEquals(2, result.getThreeStarCount());
        assertEquals(50.0, result.getFiveStarPercentage());
        assertEquals(30.0, result.getFourStarPercentage());
        assertEquals(20.0, result.getThreeStarPercentage());
        assertEquals(0.0, result.getTwoStarPercentage());
        assertEquals(0.0, result.getOneStarPercentage());
    }

    @Test
    @DisplayName("GTRS08 - Should get summary for different tutor")
    void should_getTutorRatingSummary_forDifferentTutor() {
        // Given
        User tutor2 =
                User.builder()
                        .userId("tutor-2")
                        .username("tutor2")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .avatar("tutor2-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor2);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good");

        List<TutorRating> allRatings = Arrays.asList(rating2);

        when(userRepository.findById("tutor-2")).thenReturn(Optional.of(tutor2));
        when(ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc("tutor-2"))
                .thenReturn(allRatings);
        when(ratingRepository.getAverageRatingByTutorId("tutor-2")).thenReturn(4.0);
        when(ratingRepository.countByTutor_UserId("tutor-2")).thenReturn(1L);
        when(ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc("tutor-2"))
                .thenReturn(allRatings);

        // When
        TutorRatingSummaryDTO result = tutorRatingService.getTutorRatingSummary("tutor-2");

        // Then
        assertNotNull(result);
        assertEquals("tutor-2", result.getTutorId());
        assertEquals("Bob Johnson", result.getTutorName());
        assertEquals(4.0, result.getAverageRating());
        assertEquals(1L, result.getTotalRatings());
        assertEquals(0, result.getFiveStarCount());
        assertEquals(1, result.getFourStarCount());
    }

    // ==================== GET MY RATINGS TEST CASES ====================

    @Test
    @DisplayName("GMR01 - Should get my ratings successfully with single rating")
    void should_getMyRatings_withSingleRating() {
        // Given
        MockHelper.mockSecurityContext("student");
        List<TutorRating> ratings = Arrays.asList(rating);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-1"))
                .thenReturn(ratings);

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("tutor-1", result.get(0).getTutorId());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Excellent tutor!", result.get(0).getContent());
        verify(userRepository).findByUsername("student");
        verify(ratingRepository).findByStudent_UserIdOrderByCreatedAtDesc("student-1");
    }

    @Test
    @DisplayName("GMR02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenGettingMyRatingsWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> tutorRatingService.getMyRatings());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).findByStudent_UserIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("GMR03 - Should get empty list when student has no ratings")
    void should_getMyRatings_emptyList() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-1"))
                .thenReturn(Collections.emptyList());

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("GMR04 - Should get my ratings with multiple ratings")
    void should_getMyRatings_withMultipleRatings() {
        // Given
        MockHelper.mockSecurityContext("student");

        User tutor2 =
                User.builder()
                        .userId("tutor-2")
                        .username("tutor2")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .avatar("tutor2-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor2);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good teacher");

        User tutor3 =
                User.builder()
                        .userId("tutor-3")
                        .username("tutor3")
                        .firstName("Charlie")
                        .lastName("Brown")
                        .avatar("tutor3-avatar.jpg")
                        .build();

        TutorRating rating3 = new TutorRating();
        rating3.setRatingId(3L);
        rating3.setTutor(tutor3);
        rating3.setStudent(student);
        rating3.setRating(3);
        rating3.setContent("Average");

        List<TutorRating> ratings = Arrays.asList(rating, rating2, rating3);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-1"))
                .thenReturn(ratings);

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("tutor-1", result.get(0).getTutorId());
        assertEquals("tutor-2", result.get(1).getTutorId());
        assertEquals("tutor-3", result.get(2).getTutorId());
        assertEquals(5, result.get(0).getRating());
        assertEquals(4, result.get(1).getRating());
        assertEquals(3, result.get(2).getRating());
    }

    @Test
    @DisplayName("GMR05 - Should get my ratings ordered by created date descending")
    void should_getMyRatings_orderedByCreatedDateDesc() {
        // Given
        MockHelper.mockSecurityContext("student");

        TutorRating oldRating = new TutorRating();
        oldRating.setRatingId(2L);
        oldRating.setTutor(tutor);
        oldRating.setStudent(student);
        oldRating.setRating(4);
        oldRating.setContent("Old rating");
        oldRating.setCreatedAt(LocalDateTime.now().minusDays(5));

        TutorRating newRating = new TutorRating();
        newRating.setRatingId(3L);
        newRating.setTutor(tutor);
        newRating.setStudent(student);
        newRating.setRating(5);
        newRating.setContent("New rating");
        newRating.setCreatedAt(LocalDateTime.now());

        // Most recent first
        List<TutorRating> ratings = Arrays.asList(newRating, oldRating);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-1"))
                .thenReturn(ratings);

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getRatingId()); // Most recent first
        assertEquals(2L, result.get(1).getRatingId()); // Older second
        assertEquals("New rating", result.get(0).getContent());
        assertEquals("Old rating", result.get(1).getContent());
    }

    @Test
    @DisplayName("GMR06 - Should get my ratings for different student")
    void should_getMyRatings_forDifferentStudent() {
        // Given
        MockHelper.mockSecurityContext("student2");
        User anotherStudent =
                User.builder()
                        .userId("student-2")
                        .username("student2")
                        .firstName("Jane")
                        .lastName("Doe")
                        .avatar("jane-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(anotherStudent);
        rating2.setRating(4);
        rating2.setContent("Great tutor!");

        List<TutorRating> ratings = Arrays.asList(rating2);

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(anotherStudent));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-2"))
                .thenReturn(ratings);

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getRatingId());
        assertEquals("tutor-1", result.get(0).getTutorId());
        assertEquals(4, result.get(0).getRating());
        verify(userRepository).findByUsername("student2");
        verify(ratingRepository).findByStudent_UserIdOrderByCreatedAtDesc("student-2");
    }

    @Test
    @DisplayName("GMR07 - Should get my ratings with all rating fields populated")
    void should_getMyRatings_withAllFieldsPopulated() {
        // Given
        MockHelper.mockSecurityContext("student");

        rating.setRatingId(1L);
        rating.setRating(5);
        rating.setContent("Excellent tutor!");
        rating.setCreatedAt(LocalDateTime.now());

        List<TutorRating> ratings = Arrays.asList(rating);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc("student-1"))
                .thenReturn(ratings);

        // When
        List<TutorRatingDTO> result = tutorRatingService.getMyRatings();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        TutorRatingDTO dto = result.get(0);
        assertEquals(1L, dto.getRatingId());
        assertEquals("tutor-1", dto.getTutorId());
        assertEquals("Alice Smith", dto.getTutorName());
        assertEquals("student-1", dto.getStudentId());
        assertEquals("John Doe", dto.getStudentName());
        assertEquals(5, dto.getRating());
        assertEquals("Excellent tutor!", dto.getContent());
        assertNotNull(dto.getCreatedAt());
    }

    // ==================== GET MY RATING FOR TUTOR TEST CASES ====================

    @Test
    @DisplayName("GMRFT01 - Should get my rating for specific tutor successfully")
    void should_getMyRatingForTutor_successfully() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(Optional.of(rating));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals("tutor-1", result.getTutorId());
        assertEquals(5, result.getRating());
        assertEquals("Excellent tutor!", result.getContent());
        verify(userRepository).findByUsername("student");
        verify(ratingRepository).findByStudent_UserIdAndTutor_UserId("student-1", "tutor-1");
    }

    @Test
    @DisplayName("GMRFT02 - Should throw USER_NOT_EXISTED when student not logged in")
    void should_throwUserNotExisted_whenGettingMyRatingForTutorWithoutLogin() {
        // Given
        MockHelper.mockSecurityContext("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorRatingService.getMyRatingForTutor("tutor-1"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(ratingRepository, never()).findByStudent_UserIdAndTutor_UserId(any(), any());
    }

    @Test
    @DisplayName("GMRFT03 - Should return null when no rating exists for tutor")
    void should_returnNull_whenNoRatingExistsForTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-999"))
                .thenReturn(Optional.empty());

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-999");

        // Then
        assertNull(result);
        verify(ratingRepository).findByStudent_UserIdAndTutor_UserId("student-1", "tutor-999");
    }

    @Test
    @DisplayName("GMRFT04 - Should get my rating for different tutor")
    void should_getMyRatingForTutor_forDifferentTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        User tutor2 =
                User.builder()
                        .userId("tutor-2")
                        .username("tutor2")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .avatar("tutor2-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor2);
        rating2.setStudent(student);
        rating2.setRating(4);
        rating2.setContent("Good teacher");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-2"))
                .thenReturn(Optional.of(rating2));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-2");

        // Then
        assertNotNull(result);
        assertEquals("tutor-2", result.getTutorId());
        assertEquals("Bob Johnson", result.getTutorName());
        assertEquals(4, result.getRating());
        assertEquals("Good teacher", result.getContent());
    }

    @Test
    @DisplayName("GMRFT05 - Should get my rating with all fields populated")
    void should_getMyRatingForTutor_withAllFieldsPopulated() {
        // Given
        MockHelper.mockSecurityContext("student");
        rating.setRatingId(1L);
        rating.setRating(5);
        rating.setContent("Excellent tutor!");
        rating.setCreatedAt(LocalDateTime.now());
        rating.setModifiedAt(LocalDateTime.now());

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(Optional.of(rating));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getRatingId());
        assertEquals("tutor-1", result.getTutorId());
        assertEquals("Alice Smith", result.getTutorName());
        assertEquals("student-1", result.getStudentId());
        assertEquals("John Doe", result.getStudentName());
        assertEquals(5, result.getRating());
        assertEquals("Excellent tutor!", result.getContent());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getModifiedAt());
    }

    @Test
    @DisplayName("GMRFT06 - Should get rating for different student")
    void should_getMyRatingForTutor_forDifferentStudent() {
        // Given
        MockHelper.mockSecurityContext("student2");
        User anotherStudent =
                User.builder()
                        .userId("student-2")
                        .username("student2")
                        .firstName("Jane")
                        .lastName("Doe")
                        .avatar("jane-avatar.jpg")
                        .build();

        TutorRating rating2 = new TutorRating();
        rating2.setRatingId(2L);
        rating2.setTutor(tutor);
        rating2.setStudent(anotherStudent);
        rating2.setRating(3);
        rating2.setContent("Average teacher");

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(anotherStudent));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-2", "tutor-1"))
                .thenReturn(Optional.of(rating2));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getRatingId());
        assertEquals("student-2", result.getStudentId());
        assertEquals("Jane Doe", result.getStudentName());
        assertEquals(3, result.getRating());
        assertEquals("Average teacher", result.getContent());
        verify(userRepository).findByUsername("student2");
        verify(ratingRepository).findByStudent_UserIdAndTutor_UserId("student-2", "tutor-1");
    }

    @Test
    @DisplayName("GMRFT07 - Should return null when student never rated this tutor")
    void should_returnNull_whenStudentNeverRatedTutor() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-3"))
                .thenReturn(Optional.empty());

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-3");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("GMRFT08 - Should get my rating with minimum rating (1 star)")
    void should_getMyRatingForTutor_withMinimumRating() {
        // Given
        MockHelper.mockSecurityContext("student");
        TutorRating lowRating = new TutorRating();
        lowRating.setRatingId(3L);
        lowRating.setTutor(tutor);
        lowRating.setStudent(student);
        lowRating.setRating(1);
        lowRating.setContent("Poor experience");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(Optional.of(lowRating));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRating());
        assertEquals("Poor experience", result.getContent());
    }

    @Test
    @DisplayName("GMRFT09 - Should get my rating with empty content")
    void should_getMyRatingForTutor_withEmptyContent() {
        // Given
        MockHelper.mockSecurityContext("student");
        TutorRating ratingNoContent = new TutorRating();
        ratingNoContent.setRatingId(4L);
        ratingNoContent.setTutor(tutor);
        ratingNoContent.setStudent(student);
        ratingNoContent.setRating(4);
        ratingNoContent.setContent("");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(ratingRepository.findByStudent_UserIdAndTutor_UserId("student-1", "tutor-1"))
                .thenReturn(Optional.of(ratingNoContent));

        // When
        TutorRatingDTO result = tutorRatingService.getMyRatingForTutor("tutor-1");

        // Then
        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("", result.getContent());
    }
}
