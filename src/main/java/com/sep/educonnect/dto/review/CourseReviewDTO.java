package com.sep.educonnect.dto.review;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewDTO {
    Long id;
    String studentId;
    String studentName;
    String studentAvatar;
    Long courseId;
    String courseName;
    Integer rating;
    String content;
    LocalDateTime createdAt;
    LocalDateTime modifiedAt;
    String createdBy;
    String modifiedBy;
}
