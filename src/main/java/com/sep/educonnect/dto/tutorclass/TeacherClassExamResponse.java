package com.sep.educonnect.dto.tutorclass;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherClassExamResponse {
    Long examId;
    Long lessonId;
    String lessonTitle;
    String examTitle;
    String status;
    Integer totalStudents;
    Integer submittedCount;
    Integer notSubmittedCount;
    Double averageScore;
    AttendanceSummary attendanceSummary;
    List<StudentExamDetail> studentDetails;
}
