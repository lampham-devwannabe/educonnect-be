package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("SELECT s FROM Subject s WHERE s.isDeleted = false")
    Page<Subject> findAllActiveSubjects(Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE s.subjectId = :id AND s.isDeleted = false")
    Optional<Subject> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT s FROM Subject s WHERE (s.nameVi = :name OR s.nameEn = :name) AND s.isDeleted = false")
    Optional<Subject> findBySubjectNameAndNotDeleted(@Param("name") String subjectName);

    @Query("SELECT s FROM Subject s WHERE (s.nameVi LIKE %:name% OR s.nameEn LIKE %:name%) AND s.isDeleted = false")
    List<Subject> findBySubjectNameContainingAndNotDeleted(@Param("name") String subjectName);

    boolean existsByNameVi(String subjectName);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subject s WHERE (s.nameVi = :name OR s.nameEn = :name) AND s.isDeleted = false")
    boolean existsBySubjectNameAndNotDeleted(@Param("name") String subjectName);

    @Query(
            """
                    SELECT s FROM Subject s
                    WHERE s.isDeleted = false
                      AND (:name IS NULL OR (LOWER(s.nameVi) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(s.nameEn) LIKE LOWER(CONCAT('%', :name, '%'))))
                    """)
    Page<Subject> searchActiveSubjects(@Param("name") String name, Pageable pageable);
}
