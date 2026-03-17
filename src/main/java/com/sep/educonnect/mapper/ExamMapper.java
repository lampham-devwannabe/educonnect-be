package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.exam.ExamForStudentResponse;
import com.sep.educonnect.dto.exam.ExamRequest;
import com.sep.educonnect.dto.exam.ExamResponse;
import com.sep.educonnect.dto.exam.QuizForStudentResponse;
import com.sep.educonnect.dto.exam.QuizOptionRequest;
import com.sep.educonnect.dto.exam.QuizOptionResponse;
import com.sep.educonnect.dto.exam.QuizRequest;
import com.sep.educonnect.dto.exam.QuizResponse;
import com.sep.educonnect.entity.Exam;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.entity.QuizOption;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    // Exam mappings
    @Mapping(target = "examId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "quizzes", ignore = true)
    Exam toEntity(ExamRequest request);

    @Mapping(target = "quizzes", source = "exam.quizzes", qualifiedByName = "quizListToResponse")
    @Mapping(target = "lessonId", source = "exam.lessonId")
    @Mapping(target = "tutorClassId", source = "exam.tutorClassId")
    ExamResponse toResponse(Exam exam);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "field", source = "request.field")
    @Mapping(target = "tutorClassId", source = "request.tutorClassId")
    void updateEntity(@MappingTarget Exam target, ExamRequest request);

    // Quiz mappings
    @Mapping(target = "quizId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "exam", ignore = true)
    @Mapping(target = "options", ignore = true)
    Quiz toEntity(QuizRequest request);

    @Mapping(target = "options", source = "quiz.options", qualifiedByName = "optionListToResponse")
    @Mapping(target = "examId", source = "quiz.examId")
    QuizResponse toResponse(Quiz quiz);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "text", source = "request.text")
    @Mapping(target = "orderNo", source = "request.orderNo")
    @Mapping(target = "type", source = "request.type")
    @Mapping(target = "validAnswer", source = "request.validAnswer")
    @Mapping(target = "explanation", source = "request.explanation")
    void updateEntity(@MappingTarget Quiz target, QuizRequest request);

    // QuizOption mappings
    @Mapping(target = "optionId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    QuizOption toEntity(QuizOptionRequest request);

    QuizOptionResponse toResponse(QuizOption option);

    // Named methods for collections
    @Named("quizListToResponse")
    default List<QuizResponse> quizListToResponse(List<Quiz> quizzes) {
        if (quizzes == null) {
            return null;
        }
        return quizzes.stream().map(this::toResponse).toList();
    }

    @Named("optionListToResponse")
    default List<QuizOptionResponse> optionListToResponse(List<QuizOption> options) {
        if (options == null) {
            return null;
        }
        return options.stream()
                .filter(option -> option.getIsDeleted() == null || !option.getIsDeleted())
                .map(this::toResponse)
                .toList();
    }

    // Student exam mappings (ẩn đáp án)
    @Mapping(target = "quizzes", source = "exam.quizzes", qualifiedByName = "quizListToStudentResponse")
    @Mapping(target = "lessonId", source = "exam.lessonId")
    ExamForStudentResponse toExamForStudentResponse(Exam exam);

    @Mapping(target = "options", source = "quiz.options", qualifiedByName = "optionListToStudentResponse")
    @Mapping(target = "examId", source = "quiz.examId")
    QuizForStudentResponse toQuizForStudentResponse(Quiz quiz);

    @Named("quizListToStudentResponse")
    default List<QuizForStudentResponse> quizListToStudentResponse(List<Quiz> quizzes) {
        if (quizzes == null) {
            return null;
        }
        return quizzes.stream().map(this::toQuizForStudentResponse).toList();
    }

    @Named("optionListToStudentResponse")
    default List<QuizOptionResponse> optionListToStudentResponse(List<QuizOption> options) {
        if (options == null) {
            return null;
        }
        return options.stream()
                .filter(option -> option.getIsDeleted() == null || !option.getIsDeleted())
                .map(option -> {
                    QuizOptionResponse response = this.toResponse(option);
                    response.setIsCorrect(null);
                    return response;
                })
                .toList();
    }
}
