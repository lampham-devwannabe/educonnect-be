package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Course;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
  Optional<Course> findByIdAndIsDeletedFalse(Long id);

  List<Course> findByTutor_UserIdAndIsDeletedFalse(String tutorId);

  @Query(
      "SELECT COUNT(c) > 0 FROM Course c "
          + "JOIN c.syllabus s "
          + "JOIN Module m ON m.syllabusId = s.syllabusId "
          + "JOIN Lesson l ON l.moduleId = m.moduleId "
          + "WHERE c.tutor.userId = :tutorId AND l.lessonId = :lessonId")
  boolean existsByTutorIdAndLessonId(
      @Param("tutorId") String tutorId, @Param("lessonId") Long lessonId);

  @Query("SELECT COUNT(c) FROM Course c")
  Long countTotalCourses();

  @Query("SELECT COUNT(c) FROM Course c WHERE c.status = :status")
  Long countCoursesByStatus(@Param("status") CourseStatus status);

  @Query("SELECT COUNT(c) FROM Course c WHERE c.isCombo = :isCombo")
  Long countCoursesByComboType(@Param("isCombo") Boolean isCombo);

  @Query("SELECT COUNT(c) FROM Course c WHERE c.createdAt >= :startDate")
  Long countNewCoursesSince(@Param("startDate") LocalDateTime startDate);

  @Query(
      """
      SELECT c FROM Course c
      LEFT JOIN c.tutor t
      WHERE c.isDeleted = false
        AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:minPrice IS NULL OR c.price >= :minPrice)
        AND (:maxPrice IS NULL OR c.price <= :maxPrice)
        AND (:tutorName IS NULL OR LOWER(CONCAT(COALESCE(t.firstName, ''), ' ', COALESCE(t.lastName, ''))) LIKE LOWER(CONCAT('%', :tutorName, '%')))
      """)
  Page<Course> searchPublicCourses(
      @Param("name") String name,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("tutorName") String tutorName,
      Pageable pageable);

  @Query(
      """
      SELECT c FROM Course c
      LEFT JOIN c.tutor t
      LEFT JOIN c.syllabus s
      WHERE c.isDeleted = false
        AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:minPrice IS NULL OR c.price >= :minPrice)
        AND (:maxPrice IS NULL OR c.price <= :maxPrice)
        AND (:tutorName IS NULL OR LOWER(CONCAT(COALESCE(t.firstName, ''), ' ', COALESCE(t.lastName, ''))) LIKE LOWER(CONCAT('%', :tutorName, '%')))
        AND (:syllabusId IS NULL OR s.syllabusId = :syllabusId)
        AND (:type IS NULL OR c.type = :type)
      """)
  Page<Course> searchAdminCourses(
      @Param("name") String name,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("tutorName") String tutorName,
      @Param("syllabusId") Long syllabusId,
      @Param("type") CourseType type,
      Pageable pageable);

  boolean existsByIdAndIsDeletedFalse(Long id);

  @Query(
      """
      SELECT c FROM Course c
      WHERE c.isDeleted = false
      ORDER BY
        (SELECT COUNT(*) FROM Booking b WHERE b.course.id = c.id AND b.bookingStatus = 'PAID') DESC,
        (SELECT COUNT(*) FROM CourseReview cr WHERE cr.course.id = c.id) DESC,
        (SELECT COALESCE(AVG(cr.rating), 0) FROM CourseReview cr WHERE cr.course.id = c.id) DESC,
        (SELECT COALESCE(tp.rating, 0) FROM TutorProfile tp
         WHERE tp.user.userId = c.tutor.userId) DESC,
        c.totalLessons DESC NULLS LAST
      """)
  List<Course> findTopCourses(Pageable pageable);
}
