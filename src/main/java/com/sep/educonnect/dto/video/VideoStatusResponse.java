package com.sep.educonnect.dto.video;

import com.sep.educonnect.enums.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatusResponse {
    private Long videoLessonId;
    private VideoStatus status;
    private Integer processingProgress;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
