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
public class QuizResponse {
    private Long quizId;
    private Long examId;
    private String text;
    private Integer orderNo;
    private String type;
    private String validAnswer;
    private String explanation;
    private List<QuizOptionResponse> options;
}
