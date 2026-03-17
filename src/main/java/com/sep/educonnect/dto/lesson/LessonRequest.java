package com.sep.educonnect.dto.lesson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
    private Long syllabusId;
    private Long moduleId; // optional
    private String title;        // chung
    private String description;  // chung
    private Integer orderNumber;
    private Integer durationMinutes;
    private String objectives;   // chung
    private String status;
}
