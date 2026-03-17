package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModuleIdOrderByOrderNumberAsc(Long moduleId);

    @Query("SELECT l FROM Lesson l WHERE l.moduleId = :moduleId")
    Page<Lesson> findByModuleIdWithPaging(@Param("moduleId") Long moduleId, Pageable pageable);
    
    @Query("SELECT l FROM Lesson l WHERE l.syllabusId = :syllabusId")
    List<Lesson> findBySyllabusId(@Param("syllabusId") Long syllabusId);

    @Query("SELECT COUNT(l) FROM Lesson l")
    Long countTotalLessons();

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.status = :status")
    Long countLessonsByStatus(@Param("status") String status);

    @Query("SELECT l FROM Lesson l WHERE l.syllabusId = :syllabusId " +
           "ORDER BY l.orderNumber ASC NULLS LAST, l.lessonId ASC NULLS LAST")
    Optional<Lesson> findFirstBySyllabusIdOrderByOrderNumberAsc(@Param("syllabusId") Long syllabusId);

    // Returns the first Lesson (by orderNumber asc) for the given module id
    Optional<Lesson> findFirstByModuleIdOrderByOrderNumberAsc(Long moduleId);
}
