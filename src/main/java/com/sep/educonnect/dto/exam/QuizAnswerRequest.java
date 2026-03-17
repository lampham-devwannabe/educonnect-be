package com.sep.educonnect.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {
    @NotNull(message = "Quiz ID is required")
    private Long quizId;
    
    private String answer; // A, B, C, D hoặc A,B,C cho multiple choice
}

