package com.sep.educonnect.dto.classenrollment;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassStudentResponse {
    Long enrollmentId;
    StudentInfo student;
    LocalDateTime enrolledAt;
    String notes;
    Boolean hasJoined;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentInfo {
        String userId;
        String firstName;
        String lastName;
        String email;
        String avatar;
    }
}

