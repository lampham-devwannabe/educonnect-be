package com.sep.educonnect.repository;

import com.sep.educonnect.entity.TutorRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRatingRepository extends JpaRepository<TutorRating, Long> {

    List<TutorRating> findByTutor_UserIdOrderByCreatedAtDesc(String tutorId);

    List<TutorRating> findByStudent_UserIdOrderByCreatedAtDesc(String studentId);

    boolean existsByStudent_UserIdAndTutor_UserId(String studentId, String tutorId);

    Optional<TutorRating> findByStudent_UserIdAndTutor_UserId(String studentId, String tutorId);

    long countByTutor_UserId(String tutorId);

    @Query("SELECT AVG(tr.rating) FROM TutorRating tr WHERE tr.tutor.userId = :tutorId")
    Double getAverageRatingByTutorId(@Param("tutorId") String tutorId);

    Page<TutorRating> findByTutor_UserIdOrderByCreatedAtDesc(String tutorId, Pageable pageable);

    List<TutorRating> findTop5ByTutor_UserIdOrderByCreatedAtDesc(String tutorId);

    Integer countByTutorUserId(String tutorId);
}
