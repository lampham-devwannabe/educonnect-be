package com.sep.educonnect.dto.lesson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Long lessonId;
    private Long syllabusId;
    private Long moduleId;
    private String title;        // localized
    private String description;  // localized
    private Integer orderNumber;
    private Integer durationMinutes;
    private String objectives;   // localized
    private String status;
}
