package com.sep.educonnect.repository;

import com.sep.educonnect.entity.StudentLikes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentLikesRepository extends JpaRepository<StudentLikes, Long> {
    Optional<StudentLikes> findByStudentIdAndTutor_Id(String studentId, Long tutorProfileId);
    
    boolean existsByStudentIdAndTutor_Id(String studentId, Long tutorProfileId);
    
    void deleteByStudentIdAndTutor_Id(String studentId, Long tutorProfileId);
    
    List<StudentLikes> findByStudentId(String studentId);
    
    @Query("SELECT COUNT(sl) FROM StudentLikes sl WHERE sl.tutor.id = :tutorProfileId")
    Long countByTutorProfileId(@Param("tutorProfileId") Long tutorProfileId);
}

