package com.sep.educonnect.dto.rating;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorRatingSummaryDTO {
    String tutorId;
    String tutorName;
    Double averageRating;
    Long totalRatings;

    // Phân bố rating
    Long fiveStarCount;
    Long fourStarCount;
    Long threeStarCount;
    Long twoStarCount;
    Long oneStarCount;

    // Phần trăm mỗi loại rating
    Double fiveStarPercentage;
    Double fourStarPercentage;
    Double threeStarPercentage;
    Double twoStarPercentage;
    Double oneStarPercentage;

    List<TutorRatingDTO> recentRatings;
}
