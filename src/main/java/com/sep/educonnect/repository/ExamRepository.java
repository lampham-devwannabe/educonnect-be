package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Exam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    Page<Exam> findByLessonId(Long lessonId, Pageable pageable);

    Optional<Exam> findByExamId(Long examId);

    Page<Exam> findByLessonIdOrderByCreatedAtDesc(Long lessonId, Pageable pageable);

    List<Exam> findByLessonIdInAndStatus(List<Long> lessonIds, String status);

    @Query("SELECT COUNT(e) FROM Exam e")
    Long countTotalExams();

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.status = :status")
    Long countExamsByStatus(@Param("status") String status);

    // Lấy exams thuộc lessonIds, đã PUBLISHED và (global OR thuộc classId)
    @Query(
            "SELECT DISTINCT e FROM Exam e LEFT JOIN e.tutorClass tc "
                    + "WHERE e.lessonId IN :lessonIds "
                    + "AND (:classId IS NULL OR e.tutorClass IS NULL OR tc.id = :classId)")
    List<Exam> findByLessonIdInAndClass(
            @Param("lessonIds") List<Long> lessonIds, @Param("classId") Long classId);

    // Lấy exams thuộc lessonIds, có status (ví dụ PUBLISHED) và (global OR thuộc classId)
    @Query(
            "SELECT DISTINCT e FROM Exam e LEFT JOIN e.tutorClass tc "
                    + "WHERE e.lessonId IN :lessonIds AND e.status = :status "
                    + "AND (:classId IS NULL OR e.tutorClass IS NULL OR tc.id = :classId)")
    List<Exam> findByLessonIdInAndStatusAndClass(
            @Param("lessonIds") List<Long> lessonIds,
            @Param("status") String status,
            @Param("classId") Long classId);
}
