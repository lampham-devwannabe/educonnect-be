package com.sep.educonnect.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.sep.educonnect.dto.exam.ExamForStudentResponse;
import com.sep.educonnect.dto.exam.QuizForStudentResponse;
import com.sep.educonnect.entity.Exam;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.entity.QuizOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ExamMapperTest {

    private final ExamMapper examMapper = Mappers.getMapper(ExamMapper.class);

    @Test
    void toExamForStudentResponse_shouldHideAnswerFlags() {
        Exam exam = new Exam();
        exam.setExamId(17L);
        exam.setLessonId(4L);

        Quiz quiz = new Quiz();
        quiz.setQuizId(45L);
        quiz.setExamId(17L);
        quiz.setText("Sample question");
        quiz.setType("SINGLE_CHOICE");

        QuizOption option1 = new QuizOption();
        option1.setOptionId(1L);
        option1.setQuizId(45L);
        option1.setText("A");
        option1.setIsCorrect(true);

        QuizOption option2 = new QuizOption();
        option2.setOptionId(2L);
        option2.setQuizId(45L);
        option2.setText("B");
        option2.setIsCorrect(false);

        quiz.setOptions(List.of(option1, option2));
        exam.setQuizzes(List.of(quiz));

        ExamForStudentResponse response = examMapper.toExamForStudentResponse(exam);
        assertNotNull(response);
        assertEquals(1, response.getQuizzes().size());
        QuizForStudentResponse quizResponse = response.getQuizzes().get(0);
        assertNotNull(quizResponse.getOptions());
        assertEquals(2, quizResponse.getOptions().size());
        quizResponse.getOptions().forEach(option -> assertNull(option.getIsCorrect()));
    }
}

