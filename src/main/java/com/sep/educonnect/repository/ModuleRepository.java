package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Module;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findBySyllabusIdOrderByOrderNumberAsc(Long syllabusId);

    @Query("SELECT m FROM Module m WHERE m.syllabusId = :syllabusId ORDER BY m.orderNumber ASC")
    Page<Module> findBySyllabusIdOrderByOrderNumberAscWithPaging(@Param("syllabusId") Long syllabusId, Pageable pageable);

    // Returns the first module (by orderNumber asc) for the given syllabus id
    Optional<Module> findFirstBySyllabusIdOrderByOrderNumberAsc(Long syllabusId);
}
