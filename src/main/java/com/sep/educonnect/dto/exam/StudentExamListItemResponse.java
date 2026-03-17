package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamListItemResponse {
    private Long examId;
    private Long lessonId;
    private String lessonTitle;
    private String examTitle; // Có thể lấy từ field hoặc lesson title
    private String status;
    private Boolean submitted;
    private Double bestScore;
    private Integer submissionCount;
}

