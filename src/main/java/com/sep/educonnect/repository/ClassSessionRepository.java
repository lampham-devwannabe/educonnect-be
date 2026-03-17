package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ClassSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.tutorClass.tutor.userId = :userId " +
            "AND cs.sessionDate BETWEEN :startDate AND :endDate")
    List<ClassSession> findByTutorAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<ClassSession> findById(Long integer);


    @Query("SELECT cs FROM ClassSession cs " +
            "JOIN cs.tutorClass tc " +
            "JOIN tc.enrollments e " +
            "WHERE e.student.userId = :studentId " +
            "AND cs.sessionDate >= :fromDate " +
            "AND cs.sessionDate <= :toDate " +
            "ORDER BY cs.sessionDate ASC, cs.slotNumber ASC")
    List<ClassSession> findStudentSchedule(
            @Param("studentId") String studentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.tutorClass.id = :classId " +
            "AND cs.sessionDate > :currentDate " +
            "AND cs.isDeleted = false " +
            "ORDER BY cs.sessionDate ASC, cs.slotNumber ASC"
    )
    List<ClassSession> findUpcomingSessionsByClassId(
            @Param("classId") Long classId,
            @Param("currentDate") LocalDate currentDate, Pageable pageable
    );

    @Query("SELECT cs.tutorClass.id, COUNT(cs) " +
            "FROM ClassSession cs " +
            "WHERE cs.tutorClass.id IN :classIds " +
            "AND cs.sessionDate > :currentDate " +
            "AND cs.isDeleted = false " +
            "GROUP BY cs.tutorClass.id")
    List<Object[]> countUpcomingSessionsByClassIds(
            @Param("classIds") List<Long> classIds,
            @Param("currentDate") LocalDate currentDate
    );

    Optional<ClassSession> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT s FROM ClassSession s " +
            "WHERE s.tutorClass.tutor.userId = :tutorId " +
            "AND s.endTime < :now")
    List<ClassSession> findCompletedSessionsByTutor(
            @Param("tutorId") String tutorId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT s FROM ClassSession s " +
            "WHERE s.tutorClass.tutor.userId = :tutorId " +
            "AND s.startTime > :now")
    List<ClassSession> findUpcomingSessionsByTutor(
            @Param("tutorId") String tutorId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(s) FROM ClassSession s " +
            "WHERE s.tutorClass.tutor.userId = :tutorId " +
            "AND EXISTS (SELECT 1 FROM TutorAvailabilityException e " +
            "WHERE e.session.id = s.id AND e.status = 'APPROVED')")
    Integer countCancelledSessionsByTutor(@Param("tutorId") String tutorId);

    @Query("SELECT s FROM ClassSession s " +
            "WHERE s.tutorClass.tutor.userId = :tutorId " +
            "ORDER BY s.startTime ASC")
    List<ClassSession> findAllSessionsByTutorOrderByDate(@Param("tutorId") String tutorId);

    @Query("SELECT COUNT(cs) FROM ClassSession cs")
    Long countTotalSessions();

    Integer countByTutorClassId(Long classId);


    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.tutorClass.tutor.userId = :tutorId " +
            "AND cs.sessionDate = :sessionDate AND cs.slotNumber = :slotNumber"
    )
    Optional<ClassSession> findBySessionDateAndSlotNumber(@Param("tutorId") String tutorId, @Param("sessionDate") LocalDate sessionDate, @Param("slotNumber") Integer slotNumber);

    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.tutorClass.tutor.userId = :tutorId " +
            "AND cs.startTime >= :startDate " +
            "AND cs.startTime < :endDate " +
            "AND cs.endTime IS NOT NULL")
    List<ClassSession> findCompletedSessionsByTutorInDateRange(
            @Param("tutorId") String tutorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.startTime >= :startDate " +
            "AND cs.startTime < :endDate " +
            "AND cs.endTime IS NOT NULL")
    List<ClassSession> findSessionsInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(cs) FROM ClassSession cs " +
            "JOIN cs.tutorClass tc " +
            "JOIN tc.enrollments e " +
            "WHERE e.student.userId IN :studentIds " +
            "AND cs.sessionDate = :date " +
            "AND cs.slotNumber = :slot " +
            "AND tc.id <> :excludeClassId")
    long countStudentConflicts(
            @Param("studentIds") List<String> studentIds,
            @Param("date") LocalDate date,
            @Param("slot") int slot,
            @Param("excludeClassId") Long excludeClassId
    );

    @Query("SELECT cs FROM ClassSession cs " +
            "WHERE cs.tutorClass.tutor.userId IN :tutorIds " +  // Changed = to IN
            "AND cs.sessionDate BETWEEN :startDate AND :endDate"
    )
    List<ClassSession> findCompletedSessionsByTutorsInDateRange(
            @Param("tutorIds") List<String> tutorIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
