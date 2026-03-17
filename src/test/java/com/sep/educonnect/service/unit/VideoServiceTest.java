package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.video.StreamingInfo;
import com.sep.educonnect.dto.video.VideoLessonRequest;
import com.sep.educonnect.dto.video.VideoLessonResponse;
import com.sep.educonnect.entity.VideoLesson;
import com.sep.educonnect.enums.VideoStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.VideoLessonMapper;
import com.sep.educonnect.repository.LessonRepository;
import com.sep.educonnect.repository.VideoLessonRepository;
import com.sep.educonnect.service.S3Service;
import com.sep.educonnect.service.VideoService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoService Unit Tests")
class VideoServiceTest {

    @Mock private VideoLessonRepository videoLessonRepository;

    @Mock private LessonRepository lessonRepository;

    @Mock private S3Service s3Service;

    @Mock private VideoLessonMapper videoLessonMapper;

    @InjectMocks private VideoService videoService;

    // ==================== CREATE VIDEO LESSON TEST CASES ====================

    @Test
    @DisplayName("CVL01 - Should create video lesson successfully with valid lessonId and title")
    void should_createVideoLesson_withValidLessonIdAndTitle() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(1L)
                        .title("Introduction to Java")
                        .description("Basic Java concepts")
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Introduction to Java")
                        .description("Basic Java concepts")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Introduction to Java")
                        .status(VideoStatus.UPLOADING)
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getVideoLessonId());
        assertEquals(1L, result.getLessonId());
        assertEquals("Introduction to Java", result.getTitle());
        assertEquals(VideoStatus.UPLOADING, video.getStatus());
        assertNotNull(video.getUploadedAt());
        verify(lessonRepository).existsById(1L);
        verify(videoLessonRepository).save(video);
        verify(videoLessonMapper).toResponse(video);
    }

    @Test
    @DisplayName("CVL02 - Should throw AppException when lessonId does not exist")
    void should_throwAppException_whenLessonIdDoesNotExist() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(999L).title("Test Video").build();

        when(lessonRepository.existsById(999L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> videoService.createVideoLesson(request));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).existsById(999L);
        verify(videoLessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("CVL03 - Should create video lesson without lessonId (null)")
    void should_createVideoLesson_withNullLessonId() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(null)
                        .title("Standalone Video")
                        .description("Video without lesson")
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(null)
                        .title("Standalone Video")
                        .description("Video without lesson")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder().videoLessonId(100L).title("Standalone Video").build();

        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getVideoLessonId());
        assertEquals("Standalone Video", result.getTitle());
        verify(lessonRepository, never()).existsById(anyLong());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL04 - Should create video lesson with lessonId = 0")
    void should_createVideoLesson_withLessonIdZero() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(0L).title("Test Video").build();

        when(lessonRepository.existsById(0L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> videoService.createVideoLesson(request));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).existsById(0L);
        verify(videoLessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("CVL05 - Should create video lesson with different valid lessonId")
    void should_createVideoLesson_withDifferentValidLessonId() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(2L).title("Advanced Topics").build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(101L)
                        .lessonId(2L)
                        .title("Advanced Topics")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(101L)
                        .lessonId(2L)
                        .title("Advanced Topics")
                        .build();

        when(lessonRepository.existsById(2L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getLessonId());
        verify(lessonRepository).existsById(2L);
    }

    @Test
    @DisplayName("CVL06 - Should create video lesson with title 'abcdc'")
    void should_createVideoLesson_withTitleAbcdc() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(1L).title("abcdc").build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("abcdc")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder().videoLessonId(100L).title("abcdc").build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals("abcdc", result.getTitle());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL07 - Should create video lesson with description")
    void should_createVideoLesson_withDescription() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(1L)
                        .title("Test Video")
                        .description("This is a detailed description of the video lesson")
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Test Video")
                        .description("This is a detailed description of the video lesson")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .title("Test Video")
                        .description("This is a detailed description of the video lesson")
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals("This is a detailed description of the video lesson", result.getDescription());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL08 - Should create video lesson without description (null)")
    void should_createVideoLesson_withNullDescription() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(1L)
                        .title("Video Without Description")
                        .description(null)
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Video Without Description")
                        .description(null)
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .title("Video Without Description")
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals("Video Without Description", result.getTitle());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL09 - Should create video lesson with empty description")
    void should_createVideoLesson_withEmptyDescription() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(1L)
                        .title("Video With Empty Description")
                        .description("")
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Video With Empty Description")
                        .description("")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .title("Video With Empty Description")
                        .description("")
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals("", result.getDescription());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL10 - Should set status to UPLOADING when creating video lesson")
    void should_setStatusToUploading_whenCreatingVideoLesson() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(1L).title("Test Video").build();

        VideoLesson video =
                VideoLesson.builder().videoLessonId(100L).lessonId(1L).title("Test Video").build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .title("Test Video")
                        .status(VideoStatus.UPLOADING)
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        videoService.createVideoLesson(request);

        // Then
        assertEquals(VideoStatus.UPLOADING, video.getStatus());
        assertNotNull(video.getUploadedAt());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL11 - Should set uploadedAt timestamp when creating video lesson")
    void should_setUploadedAtTimestamp_whenCreatingVideoLesson() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(1L).title("Test Video").build();

        VideoLesson video =
                VideoLesson.builder().videoLessonId(100L).lessonId(1L).title("Test Video").build();

        VideoLessonResponse response =
                VideoLessonResponse.builder().videoLessonId(100L).title("Test Video").build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        videoService.createVideoLesson(request);

        // Then
        assertNotNull(video.getUploadedAt());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL12 - Should create multiple video lessons with different IDs")
    void should_createMultipleVideoLessons_withDifferentIds() {
        // Given - First video
        VideoLessonRequest request1 =
                VideoLessonRequest.builder().lessonId(1L).title("Video 1").build();

        VideoLesson video1 =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Video 1")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response1 =
                VideoLessonResponse.builder().videoLessonId(100L).title("Video 1").build();

        // Given - Second video
        VideoLessonRequest request2 =
                VideoLessonRequest.builder().lessonId(2L).title("Video 2").build();

        VideoLesson video2 =
                VideoLesson.builder()
                        .videoLessonId(101L)
                        .lessonId(2L)
                        .title("Video 2")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response2 =
                VideoLessonResponse.builder().videoLessonId(101L).title("Video 2").build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(lessonRepository.existsById(2L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request1)).thenReturn(video1);
        when(videoLessonMapper.toEntity(request2)).thenReturn(video2);
        when(videoLessonRepository.save(video1)).thenReturn(video1);
        when(videoLessonRepository.save(video2)).thenReturn(video2);
        when(videoLessonMapper.toResponse(video1)).thenReturn(response1);
        when(videoLessonMapper.toResponse(video2)).thenReturn(response2);

        // When
        VideoLessonResponse result1 = videoService.createVideoLesson(request1);
        VideoLessonResponse result2 = videoService.createVideoLesson(request2);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(100L, result1.getVideoLessonId());
        assertEquals(101L, result2.getVideoLessonId());
        verify(videoLessonRepository, times(2)).save(any(VideoLesson.class));
    }

    @Test
    @DisplayName("CVL13 - Should verify mapper is called correctly")
    void should_verifyMapperCalledCorrectly_whenCreatingVideoLesson() {
        // Given
        VideoLessonRequest request =
                VideoLessonRequest.builder()
                        .lessonId(1L)
                        .title("Test Video")
                        .description("Test Description")
                        .build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title("Test Video")
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder().videoLessonId(100L).title("Test Video").build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        videoService.createVideoLesson(request);

        // Then
        verify(videoLessonMapper, times(1)).toEntity(request);
        verify(videoLessonMapper, times(1)).toResponse(video);
        verify(videoLessonRepository, times(1)).save(video);
    }

    @Test
    @DisplayName("CVL14 - Should create video lesson with long title")
    void should_createVideoLesson_withLongTitle() {
        // Given
        String longTitle =
                "This is a very long title for a video lesson that contains many characters and should still be valid";
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(1L).title(longTitle).build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title(longTitle)
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder().videoLessonId(100L).title(longTitle).build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals(longTitle, result.getTitle());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("CVL15 - Should create video lesson with special characters in title")
    void should_createVideoLesson_withSpecialCharactersInTitle() {
        // Given
        String titleWithSpecialChars = "Introduction to C++ & Java: Part 1 (2023)";
        VideoLessonRequest request =
                VideoLessonRequest.builder().lessonId(1L).title(titleWithSpecialChars).build();

        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(1L)
                        .title(titleWithSpecialChars)
                        .status(VideoStatus.UPLOADING)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(100L)
                        .title(titleWithSpecialChars)
                        .build();

        when(lessonRepository.existsById(1L)).thenReturn(true);
        when(videoLessonMapper.toEntity(request)).thenReturn(video);
        when(videoLessonRepository.save(video)).thenReturn(video);
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.createVideoLesson(request);

        // Then
        assertNotNull(result);
        assertEquals(titleWithSpecialChars, result.getTitle());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("Should generate upload URL")
    void should_generateUploadUrl() {
        // Given
        Long videoLessonId = 100L;
        com.sep.educonnect.dto.file.S3FileResponse s3Response =
                com.sep.educonnect.dto.file.S3FileResponse.builder()
                        .url("https://s3.amazonaws.com/bucket/video/100/original.mp4?presigned")
                        .build();

        when(s3Service.generateVideoUploadUrl(videoLessonId)).thenReturn(s3Response);

        // When
        String url = videoService.generateUploadUrl(videoLessonId);

        // Then
        assertEquals("https://s3.amazonaws.com/bucket/video/100/original.mp4?presigned", url);
        verify(s3Service).generateVideoUploadUrl(videoLessonId);
    }

    @Test
    @DisplayName("Should check if HLS is ready")
    void should_checkIfHlsReady() {
        // Given
        Long videoLessonId = 100L;
        when(s3Service.hlsFilesExist(videoLessonId)).thenReturn(true);

        // When
        boolean isReady = videoService.isHlsReady(videoLessonId);

        // Then
        assertTrue(isReady);
        verify(s3Service).hlsFilesExist(videoLessonId);
    }

    @Test
    @DisplayName("Should get streaming URL when video is ready")
    void should_getStreamingUrl_whenVideoReady() {
        // Given
        Long videoLessonId = 100L;
        String userId = "user-1";
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.READY)
                        .build();

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(s3Service.getHlsManifestSignedUrl(videoLessonId))
                .thenReturn("https://cdn.example.com/video/100/master.m3u8");

        // When
        String url = videoService.getStreamingUrl(userId, videoLessonId);

        // Then
        assertEquals("https://cdn.example.com/video/100/master.m3u8", url);
        verify(videoLessonRepository).findById(videoLessonId);
        verify(s3Service).getHlsManifestSignedUrl(videoLessonId);
    }

    @Test
    @DisplayName("Should throw when video not found for streaming")
    void should_throw_when_videoNotFound_forStreaming() {
        // Given
        Long videoLessonId = 999L;
        String userId = "user-1";
        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> videoService.getStreamingUrl(userId, videoLessonId));
        assertEquals(ErrorCode.VIDEO_LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when video not ready for streaming")
    void should_throw_when_videoNotReady_forStreaming() {
        // Given
        Long videoLessonId = 100L;
        String userId = "user-1";
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.PROCESSING)
                        .build();

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> videoService.getStreamingUrl(userId, videoLessonId));
        assertEquals(ErrorCode.VIDEO_NOT_READY_FOR_STREAMING, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get streaming info when video is ready")
    void should_getStreamingInfo_whenVideoReady() throws Exception {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.READY)
                        .build();

        Map<String, String> cookies =
                Map.of(
                        "CloudFront-Policy", "policy",
                        "CloudFront-Signature", "signature",
                        "CloudFront-Key-Pair-Id", "key-pair-id");

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(s3Service.getHlsManifestSignedUrl(videoLessonId))
                .thenReturn("https://cdn.example.com/video/100/master.m3u8");
        when(s3Service.getHlsSignedCookies(videoLessonId)).thenReturn(cookies);

        // When
        StreamingInfo info = videoService.getStreamingInfo(videoLessonId);

        // Then
        assertNotNull(info);
        assertEquals("https://cdn.example.com/video/100/master.m3u8", info.getManifestUrl());
        assertEquals(3600L, info.getExpiresInSeconds());
        assertEquals(cookies, info.getCookies());
        verify(s3Service).getHlsManifestSignedUrl(videoLessonId);
        verify(s3Service).getHlsSignedCookies(videoLessonId);
    }

    @Test
    @DisplayName("Should throw when video not found for streaming info")
    void should_throw_when_videoNotFound_forStreamingInfo() {
        // Given
        Long videoLessonId = 999L;
        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> videoService.getStreamingInfo(videoLessonId));
        assertEquals(ErrorCode.VIDEO_LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when video not ready for streaming info")
    void should_throw_when_videoNotReady_forStreamingInfo() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.PROCESSING)
                        .build();

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> videoService.getStreamingInfo(videoLessonId));
        assertEquals(ErrorCode.VIDEO_NOT_READY_FOR_STREAMING, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should update video status")
    void should_updateVideoStatus() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.UPLOADING)
                        .processingProgress(0)
                        .build();

        String hlsKey = "video/100/master.m3u8";
        Integer progress = 50;
        String errorMessage = null;

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(videoLessonRepository.save(video)).thenReturn(video);

        // When
        videoService.updateVideoStatus(
                videoLessonId, VideoStatus.PROCESSING, hlsKey, progress, errorMessage);

        // Then
        assertEquals(VideoStatus.PROCESSING, video.getStatus());
        assertEquals(hlsKey, video.getHlsMasterPlaylistS3Key());
        assertEquals(progress, video.getProcessingProgress());
        assertNotNull(video.getProcessedAt());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("Should update video status with error message")
    void should_updateVideoStatus_withErrorMessage() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.PROCESSING)
                        .build();

        String errorMessage = "Processing failed";

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(videoLessonRepository.save(video)).thenReturn(video);

        // When
        videoService.updateVideoStatus(videoLessonId, VideoStatus.FAILED, null, null, errorMessage);

        // Then
        assertEquals(VideoStatus.FAILED, video.getStatus());
        assertEquals(errorMessage, video.getProcessingErrorMessage());
        verify(videoLessonRepository).save(video);
    }

    @Test
    @DisplayName("Should throw when video not found for status update")
    void should_throw_when_videoNotFound_forStatusUpdate() {
        // Given
        Long videoLessonId = 999L;
        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                videoService.updateVideoStatus(
                                        videoLessonId, VideoStatus.READY, null, null, null));
        assertEquals(ErrorCode.VIDEO_LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get video lesson by ID")
    void should_getVideoLesson() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .title("Test Video")
                        .status(VideoStatus.READY)
                        .build();

        VideoLessonResponse response =
                VideoLessonResponse.builder()
                        .videoLessonId(videoLessonId)
                        .title("Test Video")
                        .status(VideoStatus.READY)
                        .build();

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(videoLessonMapper.toResponse(video)).thenReturn(response);

        // When
        VideoLessonResponse result = videoService.getVideoLesson(videoLessonId);

        // Then
        assertNotNull(result);
        assertEquals(videoLessonId, result.getVideoLessonId());
        assertEquals("Test Video", result.getTitle());
        verify(videoLessonRepository).findById(videoLessonId);
    }

    @Test
    @DisplayName("Should throw when video lesson not found")
    void should_throw_when_videoLessonNotFound() {
        // Given
        Long videoLessonId = 999L;
        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> videoService.getVideoLesson(videoLessonId));
        assertEquals(ErrorCode.VIDEO_LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get videos by lesson with pagination")
    void should_getVideosByLesson_withPagination() {
        // Given
        Long lessonId = 1L;
        VideoLesson video1 =
                VideoLesson.builder()
                        .videoLessonId(100L)
                        .lessonId(lessonId)
                        .title("Video 1")
                        .build();

        VideoLesson video2 =
                VideoLesson.builder()
                        .videoLessonId(101L)
                        .lessonId(lessonId)
                        .title("Video 2")
                        .build();

        Page<VideoLesson> videoPage = new PageImpl<>(List.of(video1, video2));
        when(videoLessonRepository.findByLessonId(eq(lessonId), any(Pageable.class)))
                .thenReturn(videoPage);

        VideoLessonResponse response1 =
                VideoLessonResponse.builder().videoLessonId(100L).title("Video 1").build();

        VideoLessonResponse response2 =
                VideoLessonResponse.builder().videoLessonId(101L).title("Video 2").build();

        when(videoLessonMapper.toResponse(video1)).thenReturn(response1);
        when(videoLessonMapper.toResponse(video2)).thenReturn(response2);

        // When
        Page<VideoLessonResponse> result =
                videoService.getVideosByLesson(lessonId, 0, 10, "videoLessonId", "asc");

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(videoLessonRepository).findByLessonId(eq(lessonId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should update video status to READY with HLS key")
    void should_updateVideoStatusToReady_withHlsKey() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.PROCESSING)
                        .build();

        String hlsKey = "video/100/master.m3u8";

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(videoLessonRepository.save(video)).thenReturn(video);

        // When
        videoService.updateVideoStatus(videoLessonId, VideoStatus.READY, hlsKey, 100, null);

        // Then
        assertEquals(VideoStatus.READY, video.getStatus());
        assertEquals(hlsKey, video.getHlsMasterPlaylistS3Key());
        assertEquals(100, video.getProcessingProgress());
        assertNotNull(video.getProcessedAt());
    }

    @Test
    @DisplayName("Should update video status without HLS key")
    void should_updateVideoStatus_withoutHlsKey() {
        // Given
        Long videoLessonId = 100L;
        VideoLesson video =
                VideoLesson.builder()
                        .videoLessonId(videoLessonId)
                        .status(VideoStatus.UPLOADING)
                        .build();

        when(videoLessonRepository.findById(videoLessonId)).thenReturn(Optional.of(video));
        when(videoLessonRepository.save(video)).thenReturn(video);

        // When
        videoService.updateVideoStatus(videoLessonId, VideoStatus.PROCESSING, null, 25, null);

        // Then
        assertEquals(VideoStatus.PROCESSING, video.getStatus());
        assertEquals(25, video.getProcessingProgress());
        assertNull(video.getHlsMasterPlaylistS3Key());
        assertNull(video.getProcessedAt());
    }
}
