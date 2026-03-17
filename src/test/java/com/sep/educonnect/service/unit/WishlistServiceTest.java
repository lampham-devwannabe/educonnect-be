package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.student.WishlistResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.WishlistService;
import com.sep.educonnect.util.MockHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService Unit Tests")
class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseMapper courseMapper;
    @Mock private TutorAvailabilityRepository tutorAvailabilityRepository;
    @Mock private ClassSessionRepository sessionRepository;

    @InjectMocks private WishlistService wishlistService;

    private User student;
    private User tutor;
    private Course course;
    private Wishlist wishlist;
    private TutorAvailability tutorAvailability;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .userId("student-1")
                .username("student")
                .firstName("John")
                .lastName("Doe")
                .email("student@example.com")
                .build();

        tutor = User.builder()
                .userId("tutor-1")
                .username("tutor")
                .firstName("Jane")
                .lastName("Smith")
                .email("tutor@example.com")
                .build();

        course = Course.builder()
                .id(1L)
                .name("Java Programming")
                .price(new BigDecimal("100.00"))
                .tutor(tutor)
                .build();

        wishlist = Wishlist.builder()
                .id(1L)
                .user(student)
                .course(course)
                .build();

        tutorAvailability = TutorAvailability.builder()
                .id(1L)
                .user(tutor)
                .isWorkOnMonday(true)
                .mondaySlots("1,2,3")
                .build();
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    // ==================== ADD TO WISHLIST TEST CASES ====================

    @Test
    @DisplayName("Should add course to wishlist successfully")
    void should_addToWishlist_successfully() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.of(course));
        when(wishlistRepository.existsByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(false);
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenReturn(wishlist);

        // When
        assertDoesNotThrow(() -> wishlistService.addToWishlist(courseId));

        // Then
        verify(userRepository).findByUsernameAndNotDeleted(username);
        verify(courseRepository).findById(courseId);
        verify(wishlistRepository).existsByUserIdAndCourse_Id(student.getUserId(), courseId);
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should throw exception when course already in wishlist")
    void should_throwException_when_courseAlreadyInWishlist() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.of(course));
        when(wishlistRepository.existsByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(true);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> wishlistService.addToWishlist(courseId));
        
        assertEquals(ErrorCode.ALREADY_IN_WISHLIST, exception.getErrorCode());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void should_throwException_when_userNotFound() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> wishlistService.addToWishlist(courseId));
        
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(courseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when course not found")
    void should_throwException_when_courseNotFound() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> wishlistService.addToWishlist(courseId));
        
        assertEquals(ErrorCode.COURSE_NOT_EXISTED, exception.getErrorCode());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    // ==================== REMOVE FROM WISHLIST TEST CASES ====================

    @Test
    @DisplayName("Should remove course from wishlist successfully")
    void should_removeFromWishlist_successfully() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.findByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(Optional.of(wishlist));

        // When
        assertDoesNotThrow(() -> wishlistService.removeFromWishlist(courseId));

        // Then
        verify(userRepository).findByUsernameAndNotDeleted(username);
        verify(wishlistRepository).findByUserIdAndCourse_Id(student.getUserId(), courseId);
        verify(wishlistRepository).delete(wishlist);
    }

    @Test
    @DisplayName("Should throw exception when course not in wishlist")
    void should_throwException_when_courseNotInWishlist() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.findByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> wishlistService.removeFromWishlist(courseId));
        
        assertEquals(ErrorCode.NOT_IN_WISHLIST, exception.getErrorCode());
        verify(wishlistRepository, never()).delete(any());
    }

    // ==================== GET WISHLIST TEST CASES ====================

    @Test
    @DisplayName("Should get wishlist successfully with available tutor")
    void should_getWishlist_successfully_withAvailableTutor() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);

        CourseInfoResponse courseResponse = CourseInfoResponse.builder()
                .id(1L)
                .name("Java Programming")
                .build();

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.findByUserId(student.getUserId()))
                .thenReturn(Arrays.asList(wishlist));
        when(tutorAvailabilityRepository.findByUserUserId(tutor.getUserId()))
                .thenReturn(Optional.of(tutorAvailability));
        when(sessionRepository.findByTutorAndDateRange(eq(tutor.getUserId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList()); // No booked sessions
        when(courseMapper.toCourseInfoResponse(course))
                .thenReturn(courseResponse);

        // When
        List<WishlistResponse> result = wishlistService.getWishlist();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getWishlistId());
        assertTrue(result.get(0).getIsAvailable());
        verify(wishlistRepository).findByUserId(student.getUserId());
    }

    @Test
    @DisplayName("Should get wishlist successfully with unavailable tutor")
    void should_getWishlist_successfully_withUnavailableTutor() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);

        CourseInfoResponse courseResponse = CourseInfoResponse.builder()
                .id(1L)
                .name("Java Programming")
                .build();

        TutorAvailability unavailableAvailability = TutorAvailability.builder()
                .id(1L)
                .user(tutor)
                .isWorkOnMonday(false)
                .build();

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.findByUserId(student.getUserId()))
                .thenReturn(Arrays.asList(wishlist));
        when(tutorAvailabilityRepository.findByUserUserId(tutor.getUserId()))
                .thenReturn(Optional.of(unavailableAvailability));
        when(sessionRepository.findByTutorAndDateRange(eq(tutor.getUserId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(courseRepository.existsByIdAndIsDeletedFalse(course.getId()))
                .thenReturn(false);
        when(courseMapper.toCourseInfoResponse(course))
                .thenReturn(courseResponse);

        // When
        List<WishlistResponse> result = wishlistService.getWishlist();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsAvailable());
    }

    @Test
    @DisplayName("Should return empty list when wishlist is empty")
    void should_returnEmptyList_when_wishlistIsEmpty() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.findByUserId(student.getUserId()))
                .thenReturn(Collections.emptyList());

        // When
        List<WishlistResponse> result = wishlistService.getWishlist();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wishlistRepository).findByUserId(student.getUserId());
    }

    // ==================== IS IN WISHLIST TEST CASES ====================

    @Test
    @DisplayName("Should return true when course is in wishlist")
    void should_returnTrue_when_courseInWishlist() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.existsByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(true);

        // When
        boolean result = wishlistService.isInWishlist(courseId);

        // Then
        assertTrue(result);
        verify(wishlistRepository).existsByUserIdAndCourse_Id(student.getUserId(), courseId);
    }

    @Test
    @DisplayName("Should return false when course is not in wishlist")
    void should_returnFalse_when_courseNotInWishlist() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.of(student));
        when(wishlistRepository.existsByUserIdAndCourse_Id(student.getUserId(), courseId))
                .thenReturn(false);

        // When
        boolean result = wishlistService.isInWishlist(courseId);

        // Then
        assertFalse(result);
        verify(wishlistRepository).existsByUserIdAndCourse_Id(student.getUserId(), courseId);
    }

    @Test
    @DisplayName("Should return false when user not found")
    void should_returnFalse_when_userNotFound() {
        // Given
        String username = "student";
        MockHelper.mockSecurityContext(username);
        Long courseId = 1L;

        when(userRepository.findByUsernameAndNotDeleted(username))
                .thenReturn(Optional.empty());

        // When
        boolean result = wishlistService.isInWishlist(courseId);

        // Then
        assertFalse(result);
        verify(wishlistRepository, never()).existsByUserIdAndCourse_Id(anyString(), anyLong());
    }
}
