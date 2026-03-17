package com.sep.educonnect.dto.review;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseReviewSummaryDTO {
    Long courseId;
    String courseName;
    Double averageRating;
    Long totalReviews;

    // Rating distribution
    Long fiveStarCount;
    Long fourStarCount;
    Long threeStarCount;
    Long twoStarCount;
    Long oneStarCount;

    // Percentage for each rating
    Double fiveStarPercentage;
    Double fourStarPercentage;
    Double threeStarPercentage;
    Double twoStarPercentage;
    Double oneStarPercentage;

    List<CourseReviewDTO> recentReviews;
}
