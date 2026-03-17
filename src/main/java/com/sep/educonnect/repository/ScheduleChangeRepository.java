package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ScheduleChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleChangeRepository extends JpaRepository<ScheduleChange, Long> {
    Page<ScheduleChange> findByStatus(String status, Pageable pageable);

    List<ScheduleChange> findByCreatedBy(String userId);

    List<ScheduleChange> findByCreatedByAndStatus(String userName, String status);

    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END " +
            "FROM ScheduleChange sc WHERE sc.session.id = :sessionId AND sc.status = 'PENDING'")
    boolean existsBySessionIdAndStatusPending(@Param("sessionId") Long sessionId);

    @Query("SELECT sc FROM ScheduleChange sc " +
            "WHERE sc.session.id IN :sessionIds " +
            "AND sc.status = 'APPROVED'")
    List<ScheduleChange> findApprovedChangesBySessionIds(@Param("sessionIds") List<Long> sessionIds);

    // In ScheduleChangeRepository interface
    // In ScheduleChangeRepository interface
    List<ScheduleChange> findByCreatedByAndStatusAndNewDateBetween(
            String createdBy, String status, LocalDate startDate, LocalDate endDate);

    List<ScheduleChange> findByCreatedByAndNewDateBetween(
            String createdBy, LocalDate startDate, LocalDate endDate);

    @Query(
            """
                    SELECT sc FROM ScheduleChange sc
                    JOIN sc.session s
                    JOIN s.tutorClass tc
                    WHERE sc.isDeleted = false
                      AND (:status IS NULL OR sc.status = :status)
                      AND (:className IS NULL OR LOWER(tc.title) LIKE LOWER(CONCAT('%', :className, '%')))
                    """)
    Page<ScheduleChange> searchScheduleChanges(
            @Param("status") String status, @Param("className") String className, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END " +
            "FROM ScheduleChange sc " +
            "JOIN sc.session s " +
            "JOIN s.tutorClass tc " +
            "WHERE tc.tutor.userId = :tutorId " +
            "AND sc.newDate = :newDate " +
            "AND sc.newSLot = :newSlot " +
            "AND sc.status = :status")
    boolean existsByTutorAndNewDateAndNewSlotAndStatus(
            @Param("tutorId") String tutorId,
            @Param("newDate") LocalDate newDate,
            @Param("newSlot") Integer newSlot,
            @Param("status") String status);

    // Count students with approved schedule changes TO this date/slot
    @Query("SELECT COUNT(DISTINCT sc.id) " +
            "FROM ScheduleChange sc " +
            "JOIN sc.session s " +
            "JOIN s.tutorClass tc " +
            "JOIN tc.enrollments en " +
            "WHERE en.student.userId IN :studentIds " +
            "AND sc.newDate = :newDate " +
            "AND sc.newSLot = :newSlot " +
            "AND sc.status = :status")
    Long countStudentScheduleChangeConflicts(
            @Param("studentIds") List<String> studentIds,
            @Param("newDate") LocalDate newDate,
            @Param("newSlot") Integer newSlot,
            @Param("status") String status);
}
