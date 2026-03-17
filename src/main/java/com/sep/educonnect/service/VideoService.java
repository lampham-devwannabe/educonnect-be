package com.sep.educonnect.service;

import com.sep.educonnect.dto.video.VideoLessonRequest;
import com.sep.educonnect.dto.video.VideoLessonResponse;
import com.sep.educonnect.entity.VideoLesson;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.VideoLessonMapper;
import com.sep.educonnect.repository.LessonRepository;
import com.sep.educonnect.repository.VideoLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoLessonRepository videoLessonRepository;
    private final LessonRepository lessonRepository;
    private final S3Service s3Service;
    private final VideoLessonMapper videoLessonMapper;

    @Transactional
    public VideoLessonResponse createVideoLesson(VideoLessonRequest request) {
        // Validate lessonId if provided
        if (request.getLessonId() != null) {
            boolean lessonExists = lessonRepository.existsById(request.getLessonId());
            if (!lessonExists) {
                log.error("Lesson not found: {}", request.getLessonId());
                throw new AppException(ErrorCode.LESSON_NOT_EXISTED);
            }
        }

        VideoLesson video = videoLessonMapper.toEntity(request);
        video.setStatus(VideoStatus.UPLOADING);
        video.setUploadedAt(LocalDateTime.now());
        VideoLesson saved = videoLessonRepository.save(video);

        log.info(
                "Created VideoLesson with ID: {}, lessonId: {}",
                saved.getVideoLessonId(),
                saved.getLessonId());
        return videoLessonMapper.toResponse(saved);
    }

    public String generateUploadUrl(Long videoLessonId) {
        return s3Service.generateVideoUploadUrl(videoLessonId).getUrl();
    }

    public boolean isHlsReady(Long videoLessonId) {
        return s3Service.hlsFilesExist(videoLessonId);
    }

    public String getStreamingUrl(String userId, Long videoLessonId) {
        VideoLesson video =
                videoLessonRepository
                        .findById(videoLessonId)
                        .orElseThrow(() -> new AppException(ErrorCode.VIDEO_LESSON_NOT_FOUND));

        if (video.getStatus() != VideoStatus.READY) {
            throw new AppException(ErrorCode.VIDEO_NOT_READY_FOR_STREAMING);
        }

        try {
            return s3Service.getHlsManifestSignedUrl(videoLessonId);
        } catch (Exception e) {
            log.error(
                    "Failed to get HLS manifest signed URL for videoLessonId: {}",
                    videoLessonId,
                    e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public com.sep.educonnect.dto.video.StreamingInfo getStreamingInfo(Long videoLessonId) {
        VideoLesson video =
                videoLessonRepository
                        .findById(videoLessonId)
                        .orElseThrow(() -> new AppException(ErrorCode.VIDEO_LESSON_NOT_FOUND));

        if (video.getStatus() != VideoStatus.READY) {
            throw new AppException(ErrorCode.VIDEO_NOT_READY_FOR_STREAMING);
        }

        try {
            String manifestUrl = s3Service.getHlsManifestSignedUrl(videoLessonId);
            Map<String, String> cookies = s3Service.getHlsSignedCookies(videoLessonId);

            return com.sep.educonnect.dto.video.StreamingInfo.builder()
                    .manifestUrl(manifestUrl)
                    .expiresInSeconds(3600L)
                    .cookies(cookies)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get streaming info for videoLessonId: {}", videoLessonId, e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public void updateVideoStatus(
            Long videoLessonId,
            VideoStatus status,
            String hlsMasterPlaylistS3Key,
            Integer processingProgress,
            String errorMessage) {
        VideoLesson video =
                videoLessonRepository
                        .findById(videoLessonId)
                        .orElseThrow(() -> new AppException(ErrorCode.VIDEO_LESSON_NOT_FOUND));

        video.setStatus(status);
        if (hlsMasterPlaylistS3Key != null) {
            video.setHlsMasterPlaylistS3Key(hlsMasterPlaylistS3Key);
            video.setProcessedAt(LocalDateTime.now());
        }
        if (processingProgress != null) video.setProcessingProgress(processingProgress);
        if (errorMessage != null) video.setProcessingErrorMessage(errorMessage);
        videoLessonRepository.save(video);
    }

    public VideoLessonResponse getVideoLesson(Long id) {
        VideoLesson video =
                videoLessonRepository
                        .findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.VIDEO_LESSON_NOT_FOUND));
        return videoLessonMapper.toResponse(video);
    }

    public Page<VideoLessonResponse> getVideosByLesson(
            Long lessonId, int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<VideoLesson> videoPage = videoLessonRepository.findByLessonId(lessonId, pageable);
        return videoPage.map(videoLessonMapper::toResponse);
    }
}
