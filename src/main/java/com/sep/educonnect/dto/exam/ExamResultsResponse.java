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
public class ExamResultsResponse {
    private Long examId;
    private String examTitle;
    private String lessonTitle;
    private Integer totalSubmissions;
    private List<ExamResultResponse> results;
}

