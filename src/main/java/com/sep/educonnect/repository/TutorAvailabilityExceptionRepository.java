package com.sep.educonnect.repository;

import com.sep.educonnect.entity.TutorAvailabilityException;
import com.sep.educonnect.enums.ExceptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TutorAvailabilityExceptionRepository
        extends JpaRepository<TutorAvailabilityException, Long> {

    @Query("SELECT e FROM TutorAvailabilityException e " +
            "JOIN FETCH e.session s " +
            "JOIN FETCH s.tutorClass tc " +
            "WHERE e.tutorProfile.id = :tutorProfileId " +
            "ORDER BY s.sessionDate DESC, s.startTime DESC")
    List<TutorAvailabilityException> findByTutorProfileId(
            @Param("tutorProfileId") Long tutorProfileId);

    @Query("SELECT e FROM TutorAvailabilityException e " +
            "JOIN FETCH e.session s " +
            "WHERE e.tutorProfile.id = :tutorProfileId " +
            "AND e.status = :status " +
            "ORDER BY s.sessionDate DESC")
    List<TutorAvailabilityException> findByTutorProfileIdAndStatus(
            @Param("tutorProfileId") Long tutorProfileId,
            @Param("status") ExceptionStatus status);

    boolean existsBySessionIdAndTutorProfileIdAndStatusNot(Long sessionId, Long tutorProfileId, ExceptionStatus status);

    boolean existsBySessionIdAndTutorProfileId(Long sessionId, Long tutorProfileId);

    @Query("SELECT e FROM TutorAvailabilityException e " +
            "JOIN FETCH e.session s " +
            "JOIN FETCH e.tutorProfile tp " +
            "JOIN FETCH tp.user " +
            "WHERE e.status = 'PENDING' " +
            "ORDER BY e.createdAt ASC")
    Page<TutorAvailabilityException> findPendingExceptions(Pageable pageable);

    // Đếm số lần xin nghỉ của tutor (để tracking)
    @Query("SELECT COUNT(e) FROM TutorAvailabilityException e " +
            "WHERE e.tutorProfile.id = :tutorProfileId " +
            "AND e.createdAt >= :fromDate")
    Long countExceptionsSince(
            @Param("tutorProfileId") Long tutorProfileId,
            @Param("fromDate") LocalDateTime fromDate);

    // Tìm theo status
    @Query("SELECT e FROM TutorAvailabilityException e " +
            "JOIN FETCH e.session s " +
            "JOIN FETCH e.tutorProfile tp " +
            "JOIN FETCH tp.user " +
            "WHERE e.status = :status " +
            "ORDER BY e.createdAt DESC")
    Page<TutorAvailabilityException> findByStatus(@Param("status") ExceptionStatus status,Pageable pageable);

    // Lấy tất cả với details
    @Query("SELECT e FROM TutorAvailabilityException e " +
            "JOIN FETCH e.session s " +
            "JOIN FETCH s.tutorClass tc " +
            "JOIN FETCH e.tutorProfile tp " +
            "JOIN FETCH tp.user " +
            "ORDER BY e.createdAt DESC")
    Page<TutorAvailabilityException> findAllWithDetails(Pageable pageable);

    @Query("SELECT e FROM TutorAvailabilityException e " +
            "WHERE e.session.id = :sessionId AND e.isApproved = :isApproved")
    List<TutorAvailabilityException> findBySessionAndIsApproved(Long sessionId, Boolean isApproved);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
            "FROM TutorAvailabilityException e " +
            "JOIN e.session s " +
            "WHERE e.tutorProfile.user.userId = :tutorId " +
            "AND s.sessionDate = :date " +
            "AND s.slotNumber = :slotNumber " +
            "AND e.status = :status")
    boolean existsByTutorUserIdAndDateAndSlotAndStatus(
            @Param("tutorId") String tutorId,
            @Param("date") LocalDate date,
            @Param("slotNumber") Integer slotNumber,
            @Param("status") ExceptionStatus status);

    // Count students with approved exceptions on specific date/slot
    @Query("SELECT COUNT(DISTINCT e.session.tutorClass.id) " +
            "FROM TutorAvailabilityException e " +
            "JOIN e.session s " +
            "JOIN s.tutorClass tc " +
            "JOIN tc.enrollments en " +
            "WHERE en.student.userId IN :studentIds " +
            "AND s.sessionDate = :date " +
            "AND s.slotNumber = :slotNumber " +
            "AND e.status = :status")
    Long countStudentExceptionConflicts(
            @Param("studentIds") List<String> studentIds,
            @Param("date") LocalDate date,
            @Param("slotNumber") Integer slotNumber,
            @Param("status") ExceptionStatus status);


}
