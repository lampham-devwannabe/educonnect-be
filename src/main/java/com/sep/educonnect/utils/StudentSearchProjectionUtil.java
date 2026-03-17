package com.sep.educonnect.utils;

import com.sep.educonnect.entity.*;
import com.sep.educonnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for building student documents for OpenSearch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentSearchProjectionUtil {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final TutorRatingRepository tutorRatingRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final SubjectRepository subjectRepository;

    /**
     * Builds full student document - used when student is created (email verified + role STUDENT).
     */
    public Map<String, Object> buildFullDocument(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("User email must be verified: " + userId);
        }

        if (user.getRole() == null || !"STUDENT".equals(user.getRole().getName())) {
            throw new IllegalStateException("User must have STUDENT role: " + userId);
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", userId);

        // Name
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                " " + (user.getLastName() != null ? user.getLastName() : "");
        doc.put("name", fullName.trim());

        // Age (calculated from dob, default to 17 if null)
        Integer age = calculateAge(user.getDob());
        doc.put("age", age);

        // School Level (based on age, default to "High School")
        doc.put("schoolLevel", determineSchoolLevel(age));

        // Preferred Teaching Styles (from preferences tag IDs)
        doc.put("preferredTeachingStyles", buildPreferredTeachingStylesList(user.getPreferences()));

        // Enrollments
        doc.put("enrollments", buildEnrollments(userId));

        // Tutor Ratings
        doc.put("tutorRatings", buildTutorRatings(userId));

        return doc;
    }

    /**
     * Builds preferred teaching styles list from user preferences (tag IDs).
     */
    public List<String> buildPreferredTeachingStylesList(List<Long> preferenceTagIds) {
        if (preferenceTagIds == null || preferenceTagIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> tags = tagRepository.findAllById(preferenceTagIds);
        return tags.stream()
                .filter(tag -> tag != null && (tag.getIsDeleted() == null || !tag.getIsDeleted()))
                .map(Tag::getNameEn)
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Builds preferred teaching styles map for partial updates.
     */
    public Map<String, Object> buildPreferredTeachingStyles(List<Long> preferenceTagIds) {
        return Map.of("preferredTeachingStyles", buildPreferredTeachingStylesList(preferenceTagIds));
    }

    /**
     * Builds enrollments array from ClassEnrollment entities.
     */
    public Map<String, Object> buildEnrollments(String userId) {
        List<ClassEnrollment> enrollments = classEnrollmentRepository
                .findByStudentUserIdOrderByEnrolledAtDesc(userId);

        List<Map<String, Object>> enrollmentList = new ArrayList<>();

        for (ClassEnrollment enrollment : enrollments) {
            TutorClass tutorClass = enrollment.getTutorClass();
            if (tutorClass == null || tutorClass.getCourse() == null) {
                continue;
            }

            Course course = tutorClass.getCourse();
            Syllabus syllabus = course.getSyllabus();
            if (syllabus == null) {
                continue;
            }

            // Get tutor profile ID
            Long tutorProfileId = null;
            if (tutorClass.getTutor() != null) {
                TutorProfile tutorProfile = tutorProfileRepository
                        .findByUserUserId(tutorClass.getTutor().getUserId())
                        .orElse(null);
                if (tutorProfile != null) {
                    tutorProfileId = tutorProfile.getId();
                }
            }

            if (tutorProfileId == null) {
                continue;
            }

            // Get subject ID (hash from subject name)
            Long subjectId = null;
            if (syllabus.getSubjectId() != null) {
                Subject subject = subjectRepository.findById(syllabus.getSubjectId()).orElse(null);
                if (subject != null && subject.getNameEn() != null) {
                    subjectId = (long) Math.abs(subject.getNameEn().hashCode()) & 0xffffffffL;
                }
            }

            // Get level ID (hash from level name)
            Long levelId = null;
            if (syllabus.getLevelEn() != null && !syllabus.getLevelEn().isEmpty()) {
                levelId = (long) syllabus.getLevelEn().hashCode() & 0xffffffffL;
            }

            // Get status from CourseProgress
            String status = "not_started";
            CourseProgress courseProgress = courseProgressRepository
                    .findByEnrollmentId(enrollment.getId())
                    .orElse(null);
            if (courseProgress != null && courseProgress.getStatus() != null) {
                status = mapCourseProgressStatus(courseProgress.getStatus().name());
            }

            Map<String, Object> enrollmentMap = new HashMap<>();
            enrollmentMap.put("tutorId", tutorProfileId);
            if (subjectId != null) {
                enrollmentMap.put("subjectId", String.valueOf(subjectId));
            }
            if (levelId != null) {
                enrollmentMap.put("levelId", levelId);
            }
            enrollmentMap.put("status", status);

            enrollmentList.add(enrollmentMap);
        }

        return Map.of("enrollments", enrollmentList);
    }

    /**
     * Builds tutor ratings array from TutorRating entities.
     */
    public Map<String, Object> buildTutorRatings(String userId) {
        List<TutorRating> ratings = tutorRatingRepository
                .findByStudent_UserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> ratingList = new ArrayList<>();

        for (TutorRating rating : ratings) {
            if (rating.getTutor() == null) {
                continue;
            }

            // Get tutor profile ID
            TutorProfile tutorProfile = tutorProfileRepository
                    .findByUserUserId(rating.getTutor().getUserId())
                    .orElse(null);

            if (tutorProfile == null) {
                continue;
            }

            Map<String, Object> ratingMap = new HashMap<>();
            ratingMap.put("tutorId", tutorProfile.getId());
            ratingMap.put("rating", rating.getRating() != null ? rating.getRating() : 0);

            ratingList.add(ratingMap);
        }

        return Map.of("tutorRatings", ratingList);
    }

    /**
     * Builds basic student fields: name, age, schoolLevel.
     */
    public Map<String, Object> buildBasicFields(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Map<String, Object> fields = new HashMap<>();

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                " " + (user.getLastName() != null ? user.getLastName() : "");
        fields.put("name", fullName.trim());

        Integer age = calculateAge(user.getDob());
        fields.put("age", age);
        fields.put("schoolLevel", determineSchoolLevel(age));

        return fields;
    }

    // ========== Helper Methods ==========

    private Integer calculateAge(LocalDate dob) {
        if (dob == null) {
            return 17; // Default age
        }
        return (int) ChronoUnit.YEARS.between(dob, LocalDate.now());
    }

    private String determineSchoolLevel(Integer age) {
        if (age == null) {
            return "High School"; // Default
        }

        if (age >= 6 && age <= 11) {
            return "Elementary";
        } else if (age >= 12 && age <= 14) {
            return "Middle School";
        } else if (age >= 15 && age <= 18) {
            return "High School";
        } else {
            return "College";
        }
    }

    private String mapCourseProgressStatus(String status) {
        if (status == null) {
            return "not_started";
        }

        return switch (status.toUpperCase()) {
            case "COMPLETED" -> "completed";
            case "IN_PROGRESS" -> "in_progress";
            case "NOT_STARTED" -> "not_started";
            case "FAILED" -> "failed";
            default -> "not_started";
        };
    }
}
