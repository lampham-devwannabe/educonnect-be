package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CourseBreakdownDTO {
    Long ongoingCourses;
    Long plannedCourses;
    Long inactiveCourses;
    Long comboCourses;
    Long singleCourses;
}
