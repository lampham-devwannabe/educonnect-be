package com.sep.educonnect.entity;

import com.sep.educonnect.enums.ContentAccessType;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.enums.VideoType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "video_lesson")
public class VideoLesson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_lesson_id")
    Long videoLessonId;

    @Column(name = "lesson_id")
    Long lessonId; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
    Lesson lesson;

    @Column(name = "session_id")
    Long sessionId; // nullable (link tới ClassSession nếu là recording)

    // Metadata
    @Column(name = "title")
    String title;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_type")
    VideoType videoType;

    @Column(name = "duration_seconds")
    Integer durationSeconds;

    @Column(name = "thumbnail_s3_key")
    String thumbnailS3Key;

    // S3 & HLS keys
    @Column(name = "original_video_s3_key")
    String originalVideoS3Key;

    @Column(name = "hls_master_playlist_s3_key")
    String hlsMasterPlaylistS3Key;

    @Column(name = "hls_variant_720p_s3_key")
    String hlsVariant720pS3Key;

    @Column(name = "hls_variant_480p_s3_key")
    String hlsVariant480pS3Key;

    // Processing
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    VideoStatus status = VideoStatus.UPLOADING;

    @Column(name = "processing_progress")
    Integer processingProgress = 0;

    @Column(name = "processing_error_message", columnDefinition = "TEXT")
    String processingErrorMessage;

    @Column(name = "processed_at")
    LocalDateTime processedAt;

    // Access control
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    ContentAccessType accessType = ContentAccessType.ENROLLED_ONLY;

    @Column(name = "is_preview")
    Boolean isPreview = false;

    // Stats
    @Column(name = "view_count")
    Long viewCount = 0L;

    @Column(name = "like_count")
    Long likeCount = 0L;

    // Timestamps
    @Column(name = "uploaded_at")
    LocalDateTime uploadedAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;
}
