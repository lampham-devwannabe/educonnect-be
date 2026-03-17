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
public class ExamResponse {
    private Long examId;
    private Long lessonId;
    private String status;
    private String field;
    private Long tutorClassId; // null means global exam for the lesson
    private List<QuizResponse> quizzes;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
