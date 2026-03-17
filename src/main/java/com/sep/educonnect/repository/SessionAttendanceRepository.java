package com.sep.educonnect.repository;

import com.sep.educonnect.entity.SessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, Long> {
    List<SessionAttendance> findBySessionId(Long sessionId);
    @Query("SELECT sa FROM SessionAttendance sa " +
            "JOIN sa.enrollment e " +
            "JOIN sa.session s " +
            "WHERE e.student.userId = :studentId " +
            "AND s.tutorClass.id = :classId " +
            "ORDER BY s.sessionNumber ASC")
    List<SessionAttendance> findByStudentAndClass(
            @Param("studentId") String studentId,
            @Param("classId") Long classId
    );

    List<SessionAttendance> findByEnrollmentId(Long enrollmentId);
}

