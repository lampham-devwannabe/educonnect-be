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
public class QuizForStudentResponse {
    private Long quizId;
    private Long examId;
    private String text;
    private Integer orderNo;
    private String type;
    private List<QuizOptionResponse> options;
    // validAnswer và explanation bị ẩn
}

