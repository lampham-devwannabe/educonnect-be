package com.sep.educonnect.dto.classenrollment;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentClassResponse {
    Long enrollmentId;
    LocalDateTime enrolledAt;
    Boolean hasJoined;

    // Lightweight class info for list view
    Long classId;
    String classTitle;
    LocalDate startDate;
    LocalDate endDate;
    Integer currentStudents;
    Integer maxStudents;

    // Tutor info
    String tutorId;
    String tutorName;
    String tutorAvatar;

    // Course info
    Long courseId;
    String courseName;
}

