package com.sep.educonnect.dto.student;

import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistResponse {
    Long wishlistId;
    CourseInfoResponse course;
    Boolean isAvailable; // true if tutor has available slots that don't overlap with booked sessions
}
