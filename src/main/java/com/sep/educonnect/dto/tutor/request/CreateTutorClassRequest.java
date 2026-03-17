package com.sep.educonnect.dto.tutor.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTutorClassRequest {
    Long courseId;
    String title;
    String description;
    Integer maxStudents;

    // Class schedule
    LocalDate startDate;      // Ngày bắt đầu lớp
    Integer totalSessions;    // Tổng số buổi học

    // Weekly schedule - Which days and slots the class will run
    List<WeeklySchedule> weeklySchedules;
}
