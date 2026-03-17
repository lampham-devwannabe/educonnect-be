package com.sep.educonnect.dto.video;

import com.sep.educonnect.enums.ContentAccessType;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.enums.VideoType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLessonResponse {
    private Long videoLessonId;
    private Long lessonId;
    private Long sessionId;

    private String title;        // localized
    private String description;  // localized

    private VideoType videoType;
    private Integer durationSeconds;
    private String thumbnailS3Key;

    private String hlsMasterPlaylistS3Key;

    private VideoStatus status;
    private Integer processingProgress;
    private String processingErrorMessage;
    private LocalDateTime processedAt;

    private ContentAccessType accessType;
    private Boolean isPreview;

    private Long viewCount;
    private Long likeCount;

    private LocalDateTime uploadedAt;
    private LocalDateTime publishedAt;
}
