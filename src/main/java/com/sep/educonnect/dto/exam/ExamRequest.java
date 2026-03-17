package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRequest {
    private Long lessonId;
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    private String field;
    private Long tutorClassId; // Optional: if set, exam belongs to a specific tutor class (class-scoped exam)
}
