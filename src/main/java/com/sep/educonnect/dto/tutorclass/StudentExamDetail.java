package com.sep.educonnect.dto.tutorclass;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentExamDetail {
    String studentId;
    String studentName;
    String studentEmail;
    Boolean submitted;
    Double bestScore;
    Integer submissionCount;
    AttendanceInfo attendanceInfo;
}
