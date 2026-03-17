package com.sep.educonnect.repository;

import com.sep.educonnect.entity.VideoLesson;
import com.sep.educonnect.enums.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoLessonRepository extends JpaRepository<VideoLesson, Long> {
    Page<VideoLesson> findByLessonId(Long lessonId, Pageable pageable);

    Page<VideoLesson> findBySessionId(Long sessionId, Pageable pageable);

    Page<VideoLesson> findByStatus(VideoStatus status, Pageable pageable);

    Optional<VideoLesson> findByHlsMasterPlaylistS3Key(String s3Key);

    Optional<VideoLesson> findByOriginalVideoS3Key(String s3Key);

    @Query("SELECT COUNT(v) FROM VideoLesson v")
    Long countTotalVideos();

    @Query("SELECT COUNT(v) FROM VideoLesson v WHERE v.status = :status")
    Long countVideosByStatus(@Param("status") VideoStatus status);
}
