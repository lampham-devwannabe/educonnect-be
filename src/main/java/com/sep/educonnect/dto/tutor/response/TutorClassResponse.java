package com.sep.educonnect.dto.tutor.response;


import com.sep.educonnect.dto.classsession.ClassSessionResponse;
import com.sep.educonnect.dto.course.response.CourseGeneralResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorClassResponse {

    Long id;

    UserResponse tutor;

    CourseGeneralResponse course;

    LocalDate startDate;

    LocalDate endDate;

    Integer maxStudents = 5; // Tối đa bao nhiêu học sinh

    Integer currentStudents = 0; // Số học sinh hiện tại

    String title; // Tên buổi học: "Toán lớp 10 - Hàm số"

    String description;
    List<ClassSessionResponse> sessions = new ArrayList<>();
}
