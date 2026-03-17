package com.sep.educonnect.dto.rating;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorRatingDTO {
    Long ratingId;
    String tutorId;
    String tutorName;
    String tutorAvatar;
    String studentId;
    String studentName;
    String studentAvatar;
    Integer rating;
    String content;
    LocalDateTime createdAt;
    LocalDateTime modifiedAt;
    String createdBy;
    String modifiedBy;
}
