package com.sep.educonnect.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCourseReviewRequest {
    @Min(value = 1, message = "error.rating.must.higher.than.one")
    @Max(value = 5, message = "error.rating.must.lower.than.five")
    Integer rating;

    @Size(max = 2000, message = "error.reason.length")
    String content;
}
