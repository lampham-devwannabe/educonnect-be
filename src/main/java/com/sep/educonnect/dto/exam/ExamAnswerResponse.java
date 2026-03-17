package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswerResponse {
    private Long quizId;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private String explanation;
}

