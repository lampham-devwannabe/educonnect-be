package com.sep.educonnect.dto.course.request;

import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseUpdateRequest {

    @Size(min = 3, max = 255, message = "Course name must be between 3 and 255 characters")
    String name;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    BigDecimal price;

    Boolean isCombo;

    CourseStatus status;

    CourseType type;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description;

    Long syllabusId;

    String tutorId;

    String pictureUrl;

    @Min(value = 0, message = "Total lessons must be greater than or equal to 0")
    Integer totalLessons;
}
