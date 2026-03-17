package com.sep.educonnect.service;

import com.sep.educonnect.dto.student.WishlistResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WishlistService {
    WishlistRepository wishlistRepository;
    UserRepository userRepository;
    CourseRepository courseRepository;
    CourseMapper courseMapper;
    TutorAvailabilityRepository tutorAvailabilityRepository;
    ClassSessionRepository sessionRepository;

    @Transactional
    public void addToWishlist(Long courseId) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsernameAndNotDeleted(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndCourse_Id(student.getUserId(), courseId)) {
            throw new AppException(ErrorCode.ALREADY_IN_WISHLIST); // You may need to add this error code
        }

        Wishlist wishlist = Wishlist.builder()
                .user(student)
                .course(course)
                .build();

        wishlistRepository.save(wishlist);
        log.info("Course {} added to wishlist for student {}", courseId, student.getUserId());
    }

    @Transactional
    public void removeFromWishlist(Long courseId) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsernameAndNotDeleted(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Wishlist wishlist = wishlistRepository.findByUserIdAndCourse_Id(student.getUserId(), courseId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_IN_WISHLIST)); // You may need to add this error code

        wishlistRepository.delete(wishlist);
        log.info("Course {} removed from wishlist for student {}", courseId, student.getUserId());
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsernameAndNotDeleted(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Wishlist> wishlists = wishlistRepository.findByUserId(student.getUserId());

        return wishlists.stream()
                .map(wishlist -> {
                    Course course = wishlist.getCourse();
                    boolean isAvailable = checkTutorAvailability(course) || courseRepository.existsByIdAndIsDeletedFalse(course.getId());

                    return WishlistResponse.builder()
                            .wishlistId(wishlist.getId())
                            .course(courseMapper.toCourseInfoResponse(course))
                            .isAvailable(isAvailable)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Simplified availability check - checks if tutor has any available slots
     * in the next 4 weeks that don't overlap with booked sessions
     */
    private boolean checkTutorAvailability(Course course) {
        if (course == null || course.getTutor() == null) {
            return false;
        }

        String tutorId = course.getTutor().getUserId();

        // Get tutor availability
        TutorAvailability availability = tutorAvailabilityRepository
                .findByUserUserId(tutorId)
                .orElse(null);

        if (availability == null) {
            return false;
        }

        // Check next 4 weeks for available slots
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(4);

        // Get all booked sessions in this period
        List<ClassSession> bookedSessions = sessionRepository.findByTutorAndDateRange(tutorId, startDate, endDate);

        // Create a set of booked (date, slot) pairs
        Set<String> bookedSlots = new HashSet<>();
        for (ClassSession session : bookedSessions) {
            String key = session.getSessionDate() + "_" + session.getSlotNumber();
            bookedSlots.add(key);
        }

        // Check each day in the next 4 weeks
        for (int i = 0; i < 28; i++) { // 4 weeks = 28 days
            LocalDate currentDate = startDate.plusDays(i);
            int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7;

            // Check if tutor works on this day
            if (!availability.isWorkOnDay(dayOfWeek)) {
                continue;
            }

            // Get available slots for this day
            List<Integer> availableSlots = availability.getSlotsByDay(dayOfWeek);

            // Check if any slot is not booked
            for (Integer slotNumber : availableSlots) {
                String slotKey = currentDate + "_" + slotNumber;
                if (!bookedSlots.contains(slotKey)) {
                    // Found at least one available slot
                    return true;
                }
            }
        }

        // No available slots found
        return false;
    }

    public boolean isInWishlist(Long courseId) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> student = userRepository.findByUsernameAndNotDeleted(name);
        return student.filter(user -> wishlistRepository.existsByUserIdAndCourse_Id(user.getUserId(), courseId))
                .isPresent();
    }
}
