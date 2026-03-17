package com.sep.educonnect.repository;

import com.sep.educonnect.dto.admin.statistic.TutorSessionStatsProjection;
import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.entity.TutorClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TutorClassRepository extends JpaRepository<TutorClass, Long> {
    Optional<TutorClass> findByIdAndTutorUserId(Long classId, String tutorId);

    Page<TutorClass> findByTutorUserId(String tutorId, Pageable pageable);

    @Query("SELECT t FROM TutorClass t " +
            "JOIN FETCH t.sessions s " +
            "WHERE t.tutor.userId = :tutorId ")
    List<TutorClass> findByTutorUserId(String tutorId);

    Optional<TutorClass> findByIdAndIsDeletedFalse(Integer classId);
    List<TutorClass> findByCourse_IdAndIsDeletedFalse(Long courseId);

    @Query("SELECT tc FROM TutorClass tc " +
            "WHERE tc.tutor.userId = :tutorId " +
            "AND tc.endDate >= :now")
    List<TutorClass> findActiveClassesByTutor(
            @Param("tutorId") String tutorId,
            @Param("now") LocalDate now
    );
    @Query("SELECT tc FROM TutorClass tc " +
            "WHERE tc.tutor.userId = :tutorId " +
            "AND tc.endDate < :now")
    List<TutorClass> findCompletedClassesByTutor(
            @Param("tutorId") String tutorId,
            @Param("now") LocalDate now
    );

    @Query("SELECT COUNT(tc) FROM TutorClass tc")
    Long countTotalClasses();

    @Query("SELECT COUNT(tc) FROM TutorClass tc WHERE tc.endDate >= :now")
    Long countActiveClasses(@Param("now") LocalDate now);

    @Query("SELECT COUNT(tc) FROM TutorClass tc WHERE tc.endDate < :now")
    Long countCompletedClasses(@Param("now") LocalDate now);

    @Query("SELECT COALESCE(AVG(tc.currentStudents), 0) FROM TutorClass tc")
    Double calculateAverageClassSize();

    @Query(value = """
    SELECT 
        tp.id as id,
        tp.user_id as tutorId,
        CONCAT(u.first_name, ' ', u.last_name) as tutorName,
        COALESCE(SUM(TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time)) / 60.0, 0) as teachingHours,
        COUNT(cs.id) as sessionsCompleted
    FROM tutor_profile tp
    INNER JOIN users u ON tp.user_id = u.user_id
    LEFT JOIN course c ON c.tutor_id = u.user_id
    LEFT JOIN tutor_class tc ON tc.course_id = c.id
    LEFT JOIN class_session cs ON cs.class_id = tc.id
        AND cs.start_time >= :startDate 
        AND cs.end_time <= :endDate
    WHERE tp.submission_status = :status
    GROUP BY tp.id, tp.user_id, u.first_name, u.last_name
    ORDER BY
        CASE WHEN :sortBy = 'teachingHours' THEN teachingHours END DESC,
        CASE WHEN :sortBy = 'sessionsCompleted' THEN sessionsCompleted END DESC,
        CASE WHEN :sortBy = 'tutorName' THEN tutorName END
    """,
            countQuery = """
        SELECT COUNT(DISTINCT tp.user_id)
        FROM tutor_profile tp
        WHERE tp.submission_status = :status
    """,
            nativeQuery = true)
    Page<TutorSessionStatsProjection> findTutorSessionStats(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") Sort.Direction sortDirection,
            Pageable pageable
    );
}
