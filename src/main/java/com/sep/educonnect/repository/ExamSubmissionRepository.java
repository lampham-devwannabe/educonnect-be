package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {
    
    List<ExamSubmission> findByExamIdAndStudentId(Long examId, String studentId);
    
    List<ExamSubmission> findByExamId(Long examId);
    
    List<ExamSubmission> findByStudentId(String studentId);
    
    List<ExamSubmission> findByExamIdAndStudentIdOrderBySubmittedAtDesc(Long examId, String studentId);
    
    @Query("SELECT MAX(es.score) FROM ExamSubmission es WHERE es.examId = :examId AND es.studentId = :studentId")
    Optional<Double> findBestScoreByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT COUNT(es) FROM ExamSubmission es WHERE es.examId = :examId AND es.studentId = :studentId")
    Long countByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT es FROM ExamSubmission es LEFT JOIN FETCH es.student WHERE es.examId = :examId")
    List<ExamSubmission> findByExamIdWithStudent(@Param("examId") Long examId);
    
    @Query("SELECT es FROM ExamSubmission es LEFT JOIN FETCH es.student WHERE es.examId = :examId AND es.studentId = :studentId ORDER BY es.submittedAt DESC")
    List<ExamSubmission> findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT es FROM ExamSubmission es LEFT JOIN FETCH es.student WHERE es.submissionId = :submissionId")
    Optional<ExamSubmission> findBySubmissionIdWithStudent(@Param("submissionId") Long submissionId);
    
    @Query("SELECT AVG(es.score) FROM ExamSubmission es WHERE es.examId = :examId AND es.studentId = :studentId")
    Optional<Double> findAverageScoreByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT MIN(es.submittedAt) FROM ExamSubmission es WHERE es.examId = :examId AND es.studentId = :studentId")
    Optional<java.time.LocalDateTime> findFirstAttemptAtByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT MAX(es.submittedAt) FROM ExamSubmission es WHERE es.examId = :examId AND es.studentId = :studentId")
    Optional<java.time.LocalDateTime> findLastAttemptAtByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") String studentId);
    
    @Query("SELECT DISTINCT es.studentId FROM ExamSubmission es WHERE es.examId = :examId")
    List<String> findDistinctStudentIdsByExamId(@Param("examId") Long examId);
}

