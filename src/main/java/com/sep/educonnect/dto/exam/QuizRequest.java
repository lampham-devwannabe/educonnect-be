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
public class QuizRequest {
    private Long examId;
    private String text;
    private Integer orderNo;
    private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, etc.
    private String validAnswer;
    private String explanation;
    private List<QuizOptionRequest> options;
}
