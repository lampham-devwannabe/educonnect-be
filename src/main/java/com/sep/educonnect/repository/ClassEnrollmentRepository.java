package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    List<ClassEnrollment> findByTutorClassId(Long classId);
    Optional<ClassEnrollment> findByStudent_UserIdAndTutorClass_Course_Id(String userId, Long courseId);
    @Query("SELECT DISTINCT ce.student.userId FROM ClassEnrollment ce WHERE ce.tutorClass.course.id = :courseId")
    Set<String> findDistinctStudentUserIdsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT ce.tutorClass.course.id, ce.student.userId FROM ClassEnrollment ce " +
           "WHERE ce.tutorClass.course.id IN :courseIds")
    List<Object[]> findDistinctStudentUserIdsByCourseIds(@Param("courseIds") List<Long> courseIds);
    
    @EntityGraph(attributePaths = {"student"})
    List<ClassEnrollment> findByTutorClassIdOrderByEnrolledAtAsc(Long classId);
    
    @EntityGraph(attributePaths = {"tutorClass", "tutorClass.course", "tutorClass.tutor"})
    List<ClassEnrollment> findByStudentUserIdOrderByEnrolledAtDesc(String userId);

    @EntityGraph(attributePaths = {"tutorClass", "tutorClass.course", "tutorClass.tutor"})
    @Query("SELECT ce FROM ClassEnrollment ce " +
           "LEFT JOIN CourseProgress cp ON cp.enrollment.id = ce.id " +
           "WHERE ce.student.userId = :userId " +
           "AND (:status IS NULL OR :status = 'ALL' OR " +
           "     (:status = 'IN_PROGRESS' AND (cp IS NULL OR cp.status = 'IN_PROGRESS' OR cp.status = 'NOT_STARTED')) OR " +
           "     (:status = 'COMPLETED' AND cp.status = 'COMPLETED')) " +
           "ORDER BY ce.enrolledAt DESC")
    List<ClassEnrollment> findByStudentUserIdAndStatusOrderByEnrolledAtDesc(
            @Param("userId") String userId,
            @Param("status") String status);
    
    @Query("SELECT COUNT(ce) > 0 FROM ClassEnrollment ce " +
           "JOIN ce.tutorClass tc " +
           "JOIN tc.course c " +
           "JOIN c.syllabus s " +
           "JOIN Module m ON m.syllabusId = s.syllabusId " +
           "JOIN Lesson l ON l.moduleId = m.moduleId " +
           "WHERE ce.student.userId = :studentId AND l.lessonId = :lessonId")
    boolean existsByStudentIdAndLessonId(@Param("studentId") String studentId, @Param("lessonId") Long lessonId);

    @Query("SELECT COUNT(ce) > 0 FROM ClassEnrollment ce WHERE ce.student.userId = :studentId AND ce.tutorClass.tutor.userId = :tutorUserId")
    boolean existsByStudentUserIdAndTutorUserId(@Param("studentId") String studentId, @Param("tutorUserId") String tutorUserId);

    @Query("SELECT COUNT(ce) FROM ClassEnrollment ce")
    Long countTotalEnrollments();

    @Query("SELECT COUNT(ce.student) FROM ClassEnrollment ce WHERE ce.tutorClass.id = :classId")
    Integer countStudentsInClass(@Param("classId") Long classId);

    Optional<ClassEnrollment> findByTutorClassIdAndStudentUserId(Long classId, String studentId);
}
