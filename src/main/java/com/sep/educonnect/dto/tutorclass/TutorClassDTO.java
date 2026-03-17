package com.sep.educonnect.dto.tutorclass;

import com.sep.educonnect.dto.course.response.CourseGeneralResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorClassDTO {
    Long id;

    CourseGeneralResponse course;

    LocalDate startDate;

    LocalDate endDate;

    Integer maxStudents = 5;

    Integer currentStudents = 0;

    String title;

    String description;
}
