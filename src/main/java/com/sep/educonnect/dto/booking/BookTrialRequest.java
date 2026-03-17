package com.sep.educonnect.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookTrialRequest {
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Class ID is required")
    private Long classId;
    
    @NotNull(message = "Session ID is required")
    private Long sessionId;
}

