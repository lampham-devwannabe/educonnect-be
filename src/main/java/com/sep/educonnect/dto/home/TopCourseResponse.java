package com.sep.educonnect.dto.home;

import com.sep.educonnect.enums.CourseType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopCourseResponse {
    Long id;
    String name;
    BigDecimal price;
    String pictureUrl;
    CourseType type;
    Integer totalLessons;
    String tutorName;
    String tutorAvatar;
    Double tutorRating;
    Long paidBookingsCount;
    Long reviewsCount;
    Double averageRating;
}

