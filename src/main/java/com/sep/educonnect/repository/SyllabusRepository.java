package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Syllabus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    @Query("SELECT s FROM Syllabus s WHERE s.isDeleted = false")
    Page<Syllabus> findAllActiveSyllabus(Pageable pageable);

    @Query("SELECT s FROM Syllabus s WHERE s.syllabusId = :id AND s.isDeleted = false")
    Optional<Syllabus> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT s FROM Syllabus s WHERE s.subjectId = :subjectId AND s.isDeleted = false")
    List<Syllabus> findBySubjectIdAndNotDeleted(@Param("subjectId") Long subjectId);

    @Query("SELECT s FROM Syllabus s WHERE s.name = :name AND s.isDeleted = false")
    Optional<Syllabus> findByNameAndNotDeleted(@Param("name") String name);

    @Query("SELECT s FROM Syllabus s WHERE s.name LIKE %:name% AND s.isDeleted = false")
    List<Syllabus> findByNameContainingAndNotDeleted(@Param("name") String name);

    @Query("SELECT s FROM Syllabus s WHERE s.levelVi = :level AND s.isDeleted = false")
    List<Syllabus> findByLevelAndNotDeleted(@Param("level") String level);

    @Query("SELECT s FROM Syllabus s WHERE s.status = :status AND s.isDeleted = false")
    List<Syllabus> findByStatusAndNotDeleted(@Param("status") String status);

    @Query("SELECT s FROM Syllabus s WHERE s.subjectId = :subjectId AND s.levelVi = :level AND s.isDeleted = false")
    List<Syllabus> findBySubjectIdAndLevelAndNotDeleted(@Param("subjectId") Long subjectId, @Param("level") String level);

    boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Syllabus s WHERE s.name = :name AND s.isDeleted = false")
    boolean existsByNameAndNotDeleted(@Param("name") String name);

    @Query("SELECT COUNT(s) FROM Syllabus s WHERE s.subjectId = :subjectId AND s.isDeleted = false")
    long countBySubjectIdAndNotDeleted(@Param("subjectId") Long subjectId);

    @Query("SELECT s FROM Syllabus s WHERE s.subjectId IN :subjectIds AND s.isDeleted = false")
    List<Syllabus> findBySubjectIdsAndNotDeleted(@Param("subjectIds") List<Long> subjectIds);

    @Query(
            """
                    SELECT s FROM Syllabus s
                    WHERE s.isDeleted = false
                      AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
                      AND (:level IS NULL OR (LOWER(s.levelVi) LIKE LOWER(CONCAT('%', :level, '%')) OR LOWER(s.levelEn) LIKE LOWER(CONCAT('%', :level, '%'))))
                      AND (:status IS NULL OR LOWER(s.status) LIKE LOWER(CONCAT('%', :status, '%')))
                    """)
    Page<Syllabus> searchActiveSyllabus(
            @Param("name") String name,
            @Param("level") String level,
            @Param("status") String status,
            Pageable pageable);
}
