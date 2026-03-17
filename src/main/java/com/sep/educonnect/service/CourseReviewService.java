package com.sep.educonnect.service;

import com.sep.educonnect.dto.review.CourseReviewDTO;
import com.sep.educonnect.dto.review.CourseReviewSummaryDTO;
import com.sep.educonnect.dto.review.CreateCourseReviewRequest;
import com.sep.educonnect.dto.review.UpdateCourseReviewRequest;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.CourseReview;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassEnrollmentRepository;
import com.sep.educonnect.repository.CourseRepository;
import com.sep.educonnect.repository.CourseReviewRepository;
import com.sep.educonnect.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseReviewService {
    final CourseReviewRepository reviewRepository;
    final CourseRepository courseRepository;
    final UserRepository userRepository;
    final ClassEnrollmentRepository classEnrollmentRepository;

    @Transactional
    public CourseReviewDTO createReview(CreateCourseReviewRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Course course =
                courseRepository
                        .findByIdAndIsDeletedFalse(request.getCourseId())
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        log.info("Creating review for course: {}", course.getName());

        // Check if student is enrolled in the course
        boolean isEnrolled =
                classEnrollmentRepository
                        .findByStudent_UserIdAndTutorClass_Course_Id(student.getUserId(), request.getCourseId())
                        .isPresent();
        if (!isEnrolled) {
            throw new AppException(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR);
        }

        // Check if student already reviewed this course
        if (reviewRepository.existsByStudentIdAndCourse_Id(student.getUserId(), request.getCourseId())) {
            throw new AppException(ErrorCode.ALREADY_RATED);
        }

        CourseReview review =
                CourseReview.builder()
                        .studentId(student.getUserId())
                        .course(course)
                        .rating(request.getRating())
                        .content(request.getContent())
                        .build();

        review = reviewRepository.save(review);

        return convertToDTO(review);
    }

    @Transactional
    public CourseReviewDTO updateReview(Long reviewId, UpdateCourseReviewRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        CourseReview review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new AppException(ErrorCode.RATING_NOT_FOUND));

        // Only allow student to update their own review
        if (!review.getStudentId().equals(student.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getContent() != null) {
            review.setContent(request.getContent());
        }

        review = reviewRepository.save(review);

        return convertToDTO(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        CourseReview review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new AppException(ErrorCode.RATING_NOT_FOUND));

        if (!review.getStudentId().equals(student.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        reviewRepository.delete(review);
    }

    public Page<CourseReviewDTO> getCourseReviews(Long courseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseReview> reviewPage = reviewRepository.findByCourse_Id(courseId, pageable);

        return reviewPage.map(this::convertToDTO);
    }

    public CourseReviewSummaryDTO getCourseReviewSummary(Long courseId) {
        Course course =
                courseRepository
                        .findByIdAndIsDeletedFalse(courseId)
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        List<CourseReview> allReviews = reviewRepository.findByCourse_Id(courseId);

        long totalReviews = reviewRepository.countByCourse_Id(courseId);

        // Calculate average rating
        Double avgRating =
                allReviews.stream()
                        .mapToInt(CourseReview::getRating)
                        .average()
                        .orElse(0.0);

        // Count rating distribution
        Map<Integer, Long> ratingDistribution =
                allReviews.stream()
                        .collect(
                                Collectors.groupingBy(
                                        CourseReview::getRating, Collectors.counting()));

        long fiveStarCount = ratingDistribution.getOrDefault(5, 0L);
        long fourStarCount = ratingDistribution.getOrDefault(4, 0L);
        long threeStarCount = ratingDistribution.getOrDefault(3, 0L);
        long twoStarCount = ratingDistribution.getOrDefault(2, 0L);
        long oneStarCount = ratingDistribution.getOrDefault(1, 0L);

        // Calculate percentages
        double fiveStarPercentage = totalReviews > 0 ? (fiveStarCount * 100.0 / totalReviews) : 0;
        double fourStarPercentage = totalReviews > 0 ? (fourStarCount * 100.0 / totalReviews) : 0;
        double threeStarPercentage = totalReviews > 0 ? (threeStarCount * 100.0 / totalReviews) : 0;
        double twoStarPercentage = totalReviews > 0 ? (twoStarCount * 100.0 / totalReviews) : 0;
        double oneStarPercentage = totalReviews > 0 ? (oneStarCount * 100.0 / totalReviews) : 0;

        // Get top 5 recent reviews
        List<CourseReviewDTO> recentReviews =
                reviewRepository.findTop5ByCourse_Id(courseId).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

        return CourseReviewSummaryDTO.builder()
                .courseId(courseId)
                .courseName(course.getName())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(totalReviews)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .fiveStarPercentage(Math.round(fiveStarPercentage * 10.0) / 10.0)
                .fourStarPercentage(Math.round(fourStarPercentage * 10.0) / 10.0)
                .threeStarPercentage(Math.round(threeStarPercentage * 10.0) / 10.0)
                .twoStarPercentage(Math.round(twoStarPercentage * 10.0) / 10.0)
                .oneStarPercentage(Math.round(oneStarPercentage * 10.0) / 10.0)
                .recentReviews(recentReviews)
                .build();
    }

    public List<CourseReviewDTO> getMyReviews() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<CourseReview> reviews = reviewRepository.findByStudentId(student.getUserId());

        return reviews.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private CourseReviewDTO convertToDTO(CourseReview review) {
        Course course = review.getCourse();
        User student = userRepository.findById(review.getStudentId()).orElse(null);

        return CourseReviewDTO.builder()
                .id(review.getId())
                .studentId(review.getStudentId())
                .studentName(
                        student != null
                                ? student.getFirstName() + " " + student.getLastName()
                                : null)
                .studentAvatar(student != null ? student.getAvatar() : null)
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .modifiedAt(review.getModifiedAt())
                .createdBy(review.getCreatedBy())
                .modifiedBy(review.getModifiedBy())
                .build();
    }
}
