package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatisticsResponse {
    private String studentId;
    private String studentName;
    private Integer attemptCount;
    private Double bestScore;
    private Double averageScore;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime firstAttemptAt;
}

