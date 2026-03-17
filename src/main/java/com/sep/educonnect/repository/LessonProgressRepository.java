package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.LessonProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    List<LessonProgress> findByCourseProgressId(Long courseProgressId);

    Optional<LessonProgress> findByCourseProgressIdAndLesson_LessonId(Long courseProgressId, Long lessonId);

    @Query("SELECT lp.lesson FROM LessonProgress lp " +
           "WHERE lp.courseProgress.id = :courseProgressId " +
           "AND lp.status = 'COMPLETED' " +
           "ORDER BY lp.completedAt DESC NULLS FIRST, lp.lesson.orderNumber ASC NULLS LAST, lp.lesson.lessonId ASC NULLS LAST")
    Page<Lesson> findLatestCompletedLessonByCourseProgressId(@Param("courseProgressId") Long courseProgressId, Pageable pageable);

    @Query("SELECT lp.lesson FROM LessonProgress lp " +
           "WHERE lp.courseProgress.id = :courseProgressId " +
           "AND lp.status != 'COMPLETED' " +
           "ORDER BY lp.lesson.orderNumber ASC NULLS LAST, lp.lesson.lessonId ASC NULLS LAST")
    // Return a (paged) list so callers can request only the first result (limit 1)
    List<Lesson> findNextPendingLessonByCourseProgressId(@Param("courseProgressId") Long courseProgressId, Pageable pageable);

    List<LessonProgress> findByLesson_LessonId(Long lessonId);
}
