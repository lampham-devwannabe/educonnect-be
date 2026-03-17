package com.sep.educonnect.repository;

import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.enums.ProfileStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long> {
    Optional<TutorProfile> findByUserUserId(String tutorId);

    Optional<TutorProfile> findByUserUserIdAndSubmissionStatus(String tutorId, ProfileStatus submissionStatus);


    @Query(
            "SELECT tp FROM TutorProfile tp "
                    + "JOIN FETCH tp.user u "
                    + "LEFT JOIN FETCH tp.tags "
                    + "WHERE u.userId = :userId")
    Optional<TutorProfile> findByUserIdWithUser(@Param("userId") String userId);

    @Query(
            "SELECT p FROM TutorProfile p WHERE p.submissionStatus = :status ORDER BY p.createdAt DESC")
    Page<TutorProfile> findByStatus(ProfileStatus status, Pageable pageable);

    @Query("SELECT COUNT(p) FROM TutorProfile p WHERE p.submissionStatus = :status")
    long countByStatus(ProfileStatus status);

    @Query("SELECT p FROM TutorProfile p LEFT JOIN FETCH p.user WHERE p.user.userId IN :userIds")
    List<TutorProfile> findAllByUserUserIdIn(@Param("userIds") List<String> userIds);

    @Query(
            "SELECT DISTINCT t FROM TutorProfile t "
                    + "LEFT JOIN FETCH t.tags "
                    + "WHERE t.id = :id AND t.submissionStatus = :status")
    Optional<TutorProfile> findByIdAndStatus(Long id, ProfileStatus status);

    @Query("SELECT t FROM TutorProfile t JOIN t.subjects s WHERE s.subjectId = :subjectId")
    List<TutorProfile> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("""
            SELECT tp FROM TutorProfile tp
            JOIN tp.user u
            WHERE tp.submissionStatus = 'APPROVED'
              AND EXISTS (
                  SELECT 1 FROM Course c
                  WHERE c.tutor.userId = u.userId
                    AND c.status = 'ONGOING'
                    AND c.isDeleted = false
              )
            ORDER BY tp.rating DESC NULLS LAST,
                     tp.reviewCount DESC NULLS LAST,
                     tp.studentCount DESC NULLS LAST
            """)
    List<TutorProfile> findTopTutors(Pageable pageable);

    @Query("""
            SELECT COUNT(tp) FROM TutorProfile tp
            WHERE tp.createdAt IS NOT NULL
              AND YEAR(tp.createdAt) = :year
              AND MONTH(tp.createdAt) = :month
              AND tp.submissionStatus = 'APPROVED'
            """)
    long countTutorsCreatedInMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT tp FROM TutorProfile tp JOIN FETCH tp.user u WHERE u.userId IN :userIds")
    List<TutorProfile> findByUserIds(@Param("userIds") List<String> userIds);
}
