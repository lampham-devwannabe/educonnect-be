package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamForStudentResponse {
    private Long examId;
    private Long lessonId;
    private String status;
    private String field;
    private List<QuizForStudentResponse> quizzes;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}

