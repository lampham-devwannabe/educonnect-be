package com.sep.educonnect.service;

import com.sep.educonnect.dto.rating.CreateTutorRatingRequest;
import com.sep.educonnect.dto.rating.TutorRatingDTO;
import com.sep.educonnect.dto.rating.TutorRatingSummaryDTO;
import com.sep.educonnect.dto.rating.UpdateTutorRatingRequest;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.entity.TutorRating;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassEnrollmentRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import com.sep.educonnect.repository.TutorRatingRepository;
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
public class TutorRatingService {
    final TutorRatingRepository ratingRepository;
    final UserRepository userRepository;
    final ClassEnrollmentRepository classEnrollmentRepository;
    final TutorProfileRepository tutorProfileRepository;

    // Tạo rating mới
    @Transactional
    public TutorRatingDTO createRating(CreateTutorRatingRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User tutor =
                userRepository
                        .findById(request.getTutorId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.info("Creating rating for tutor: " + tutor.getUsername());

        // Kiểm tra student đã từng học với tutor này chưa
        boolean hasStudied =
                classEnrollmentRepository.existsByStudentUserIdAndTutorUserId(
                        student.getUserId(), request.getTutorId());
        if (!hasStudied) {
            throw new AppException(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR);
        }

        // Kiểm tra đã rating tutor này chưa
        if (ratingRepository.existsByStudent_UserIdAndTutor_UserId(
                student.getUserId(), request.getTutorId())) {
            throw new AppException(ErrorCode.ALREADY_RATED);
        }

        TutorRating rating =
                TutorRating.builder()
                        .tutor(tutor)
                        .student(student)
                        .rating(request.getRating())
                        .content(request.getContent())
                        .build();

        rating = ratingRepository.save(rating);

        updateTutorProfileRating(rating.getTutor().getUserId());
        return convertToDTO(rating);
    }

    @Transactional
    public TutorRatingDTO updateRating(Long ratingId, UpdateTutorRatingRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorRating rating =
                ratingRepository
                        .findById(ratingId)
                        .orElseThrow(() -> new AppException(ErrorCode.RATING_NOT_FOUND));

        // Chỉ cho phép student chỉnh sửa rating của mình
        if (!rating.getStudent().getUserId().equals(student.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getRating() != null) {
            rating.setRating(request.getRating());
        }
        if (request.getContent() != null) {
            rating.setContent(request.getContent());
        }

        rating = ratingRepository.save(rating);
        updateTutorProfileRating(rating.getTutor().getUserId());
        return convertToDTO(rating);
    }

    @Transactional
    public void deleteRating(Long ratingId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorRating rating =
                ratingRepository
                        .findById(ratingId)
                        .orElseThrow(() -> new AppException(ErrorCode.RATING_NOT_FOUND));

        // Only owner can delete
        if (!rating.getStudent().getUserId().equals(student.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String tutorId = rating.getTutor().getUserId();
        ratingRepository.delete(rating);

        // Force flush to ensure delete completes before recalculation
        ratingRepository.flush();

        updateTutorProfileRating(tutorId);
    }

    /** Recalculates and updates the tutor's average rating and review count */
    private void updateTutorProfileRating(String tutorId) {
        try {
            Double avgRating = ratingRepository.getAverageRatingByTutorId(tutorId);
            long reviewCount = ratingRepository.countByTutor_UserId(tutorId);

            TutorProfile profile =
                    tutorProfileRepository
                            .findByUserUserId(tutorId)
                            .orElseThrow(
                                    () -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

            profile.setRating(avgRating != null ? avgRating : 0.0);
            profile.setReviewCount((int) reviewCount);

            tutorProfileRepository.save(profile);

            log.info(
                    "Updated tutor {} rating: {} ({} reviews)",
                    tutorId,
                    profile.getRating(),
                    profile.getReviewCount());

        } catch (Exception e) {
            log.error("Failed to update tutor rating for user {}", tutorId, e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /** Manually trigger rating recalculation (useful for data fixes) */
    @Transactional
    public void recalculateTutorRating(String tutorId) {
        updateTutorProfileRating(tutorId);
    }

    // Lấy rating có phân trang
    public Page<TutorRatingDTO> getTutorRatings(String tutorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TutorRating> ratingPage =
                ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc(tutorId, pageable);

        return ratingPage.map(this::convertToDTO);
    }

    // Lấy rating summary của tutor
    public TutorRatingSummaryDTO getTutorRatingSummary(String tutorId) {
        User tutor =
                userRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<TutorRating> allRatings =
                ratingRepository.findByTutor_UserIdOrderByCreatedAtDesc(tutorId);

        Double avgRating = ratingRepository.getAverageRatingByTutorId(tutorId);
        long totalRatings = ratingRepository.countByTutor_UserId(tutorId);

        // Đếm số lượng mỗi loại rating
        Map<Integer, Long> ratingDistribution =
                allRatings.stream()
                        .collect(
                                Collectors.groupingBy(
                                        TutorRating::getRating, Collectors.counting()));

        long fiveStarCount = ratingDistribution.getOrDefault(5, 0L);
        long fourStarCount = ratingDistribution.getOrDefault(4, 0L);
        long threeStarCount = ratingDistribution.getOrDefault(3, 0L);
        long twoStarCount = ratingDistribution.getOrDefault(2, 0L);
        long oneStarCount = ratingDistribution.getOrDefault(1, 0L);

        // Tính phần trăm
        double fiveStarPercentage = totalRatings > 0 ? (fiveStarCount * 100.0 / totalRatings) : 0;
        double fourStarPercentage = totalRatings > 0 ? (fourStarCount * 100.0 / totalRatings) : 0;
        double threeStarPercentage = totalRatings > 0 ? (threeStarCount * 100.0 / totalRatings) : 0;
        double twoStarPercentage = totalRatings > 0 ? (twoStarCount * 100.0 / totalRatings) : 0;
        double oneStarPercentage = totalRatings > 0 ? (oneStarCount * 100.0 / totalRatings) : 0;

        // Lấy 5 rating gần nhất
        List<TutorRatingDTO> recentRatings =
                ratingRepository.findTop5ByTutor_UserIdOrderByCreatedAtDesc(tutorId).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

        return TutorRatingSummaryDTO.builder()
                .tutorId(tutorId)
                .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatings)
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
                .recentRatings(recentRatings)
                .build();
    }

    // Lấy rating của student
    public List<TutorRatingDTO> getMyRatings() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<TutorRating> ratings =
                ratingRepository.findByStudent_UserIdOrderByCreatedAtDesc(student.getUserId());

        return ratings.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Lấy rating của student cho 1 tutor cụ thể
    public TutorRatingDTO getMyRatingForTutor(String tutorId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorRating rating =
                ratingRepository
                        .findByStudent_UserIdAndTutor_UserId(student.getUserId(), tutorId)
                        .orElse(null);

        return rating != null ? convertToDTO(rating) : null;
    }

    // Convert entity to DTO
    private TutorRatingDTO convertToDTO(TutorRating rating) {
        return TutorRatingDTO.builder()
                .ratingId(rating.getRatingId())
                .tutorId(rating.getTutor().getUserId())
                .tutorName(rating.getTutor().getFirstName() + " " + rating.getTutor().getLastName())
                .tutorAvatar(rating.getTutor().getAvatar())
                .studentId(rating.getStudent().getUserId())
                .studentName(
                        rating.getStudent().getFirstName()
                                + " "
                                + rating.getStudent().getLastName())
                .studentAvatar(rating.getStudent().getAvatar())
                .rating(rating.getRating())
                .content(rating.getContent())
                .createdAt(rating.getCreatedAt())
                .modifiedAt(rating.getModifiedAt())
                .createdBy(rating.getCreatedBy())
                .modifiedBy(rating.getModifiedBy())
                .build();
    }
}
