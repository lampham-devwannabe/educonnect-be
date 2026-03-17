package com.sep.educonnect.dto.video;

import com.sep.educonnect.enums.ContentAccessType;
import com.sep.educonnect.enums.VideoType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLessonRequest {
    private Long lessonId; // optional
    private Long sessionId; // optional
    private String title;        // chung
    private String description;  // chung
    private VideoType videoType;
    private ContentAccessType accessType;
    private Boolean isPreview;
}
