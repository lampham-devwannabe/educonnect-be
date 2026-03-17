package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByExamId(Long examId);

    Page<Quiz> findByExamIdOrderByOrderNoAsc(Long examId, Pageable pageable);

    Optional<Quiz> findByQuizId(Long quizId);

    Optional<Quiz> findByExamIdAndText(Long examId, String text);
}
