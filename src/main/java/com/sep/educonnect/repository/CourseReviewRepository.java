package com.sep.educonnect.repository;

import com.sep.educonnect.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    
    boolean existsByStudentIdAndCourse_Id(String studentId, Long courseId);
    
    Optional<CourseReview> findByStudentIdAndCourse_Id(String studentId, Long courseId);
    
    Page<CourseReview> findByCourse_Id(Long courseId, Pageable pageable);
    
    List<CourseReview> findByStudentId(String studentId);
    
    List<CourseReview> findByCourse_Id(Long courseId);
    
    long countByCourse_Id(Long courseId);
    
    @Query(value = "SELECT * FROM course_reviews WHERE course_id = :courseId ORDER BY created_at DESC LIMIT 5", nativeQuery = true)
    List<CourseReview> findTop5ByCourse_Id(@Param("courseId") Long courseId);

    @Query("SELECT COALESCE(AVG(cr.rating), 0.0) FROM CourseReview cr WHERE cr.course.id = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);
}
