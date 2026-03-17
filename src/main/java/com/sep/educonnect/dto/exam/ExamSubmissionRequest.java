package com.sep.educonnect.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ExamSubmissionRequest {
    @NotEmpty(message = "Answers list cannot be empty")
    private List<QuizAnswerRequest> answers;
    
    @NotNull(message = "Started at time is required")
    private LocalDateTime startedAt;
}

