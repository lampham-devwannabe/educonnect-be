package com.sep.educonnect.repository;

import com.sep.educonnect.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    List<QuizOption> findByQuizId(Long quizId);

    @Query("SELECT qo FROM QuizOption qo WHERE qo.quizId = :quizId AND (qo.isDeleted = false OR qo.isDeleted IS NULL)")
    List<QuizOption> findByQuizIdAndNotDeleted(@Param("quizId") Long quizId);

    List<QuizOption> findByQuizIdOrderByOptionIdAsc(Long quizId);

    @Query("SELECT qo FROM QuizOption qo WHERE qo.quizId = :quizId AND (qo.isDeleted = false OR qo.isDeleted IS NULL) ORDER BY qo.optionId ASC")
    List<QuizOption> findByQuizIdOrderByOptionIdAscAndNotDeleted(@Param("quizId") Long quizId);

    Optional<QuizOption> findByOptionId(Long optionId);

    List<QuizOption> findByQuizIdAndIsCorrect(Long quizId, Boolean isCorrect);

    @Query("SELECT qo FROM QuizOption qo WHERE qo.quizId = :quizId AND qo.isCorrect = :isCorrect AND (qo.isDeleted = false OR qo.isDeleted IS NULL)")
    List<QuizOption> findByQuizIdAndIsCorrectAndNotDeleted(@Param("quizId") Long quizId,
            @Param("isCorrect") Boolean isCorrect);

    @Modifying
    @Query("DELETE FROM QuizOption qo WHERE qo.quizId = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}
