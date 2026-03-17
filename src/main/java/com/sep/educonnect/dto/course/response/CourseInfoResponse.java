package com.sep.educonnect.dto.course.response;

import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseInfoResponse {
    // Course fields
    Long id;
    String name;
    BigDecimal price;
    Boolean isCombo;
    Boolean isDeleted;
    CourseStatus status;
    CourseType type;
    String description;
    String pictureUrl;
    Integer totalLessons;
    // Tutor info
    TutorInfo tutor;

    // Syllabus general info (localized)
    SyllabusInfo syllabus;

    // Statistics
    Long totalEnrolled;

    // Nested DTOs
    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TutorInfo {
        String userId;
        String username;
        String firstName;
        String lastName;
        String avatar;
        Long tutorProfileId;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SyllabusInfo {
        Long syllabusId;
        String name; // localized
        String level; // localized
        String target; // localized
        String description; // localized
        String status;

        // Nested modules with lessons
        List<ModuleInfo> modules;

        // Statistics
        Integer totalModules;
        Integer totalLessons;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModuleInfo {
        Long moduleId;
        String title;
        Integer orderNumber;
        List<LessonInfo> lessons;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LessonInfo {
        Long lessonId;
        String title;
        Integer orderNumber;
    }
}
