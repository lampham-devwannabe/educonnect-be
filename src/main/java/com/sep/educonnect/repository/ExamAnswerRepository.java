package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
    
    List<ExamAnswer> findBySubmissionId(Long submissionId);
    
    List<ExamAnswer> findBySubmission_SubmissionId(Long submissionId);
}

