package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStatisticsResponse {
    private Long examId;
    private String examTitle;
    private Long lessonId;
    private String lessonTitle;
    private Integer totalSubmissions;
    private Integer totalStudents;
    private List<StudentStatisticsResponse> studentStatistics;
}
