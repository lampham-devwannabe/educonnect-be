package com.sep.educonnect.dto.course.request;

import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseCreationRequest {

    @NotBlank(message = "Course name is required")
    @Size(min = 3, max = 255, message = "Course name must be between 3 and 255 characters")
    String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    BigDecimal price;

    Boolean isCombo;

    CourseStatus status;

    CourseType type;

    @Min(value = 0, message = "Total lessons must be non-negative")
    Integer totalLessons;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description;

    @NotNull(message = "Syllabus ID is required")
    Long syllabusId;

    @NotBlank(message = "Tutor ID is required")
    String tutorId;

    String pictureUrl;
}
