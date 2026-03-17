package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.exam.ExamAnswerResponse;
import com.sep.educonnect.dto.exam.ExamResultResponse;
import com.sep.educonnect.dto.exam.ExamSubmissionRequest;
import com.sep.educonnect.dto.exam.ExamSubmissionResponse;
import com.sep.educonnect.entity.ExamAnswer;
import com.sep.educonnect.entity.ExamSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ExamSubmissionMapper {

    @Mapping(target = "submissionId", ignore = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "exam", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "correctAnswers", ignore = true)
    @Mapping(target = "submittedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "durationSeconds", expression = "java(calculateDurationSeconds(request.getStartedAt()))")
    @Mapping(target = "answers", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ExamSubmission toEntity(ExamSubmissionRequest request, Long examId, String studentId);

    @Mapping(target = "answers", source = "submission.answers", qualifiedByName = "answerListToResponse")
    @Mapping(target = "durationSeconds", source = "submission.durationSeconds")
    ExamSubmissionResponse toResponse(ExamSubmission submission);

    @Mapping(target = "explanation", ignore = true)
    ExamAnswerResponse toAnswerResponse(ExamAnswer answer);

    @Mapping(target = "studentName", expression = "java(getStudentName(submission))")
    @Mapping(target = "answers", source = "submission.answers", qualifiedByName = "answerListToResponse")
    ExamResultResponse toResultResponse(ExamSubmission submission);

    @Named("answerListToResponse")
    default List<ExamAnswerResponse> answerListToResponse(List<ExamAnswer> answers) {
        if (answers == null) {
            return null;
        }
        return answers.stream().map(this::toAnswerResponse).toList();
    }

    default Long calculateDurationSeconds(LocalDateTime startedAt) {
        if (startedAt == null) {
            return null;
        }
        return Duration.between(startedAt, LocalDateTime.now()).getSeconds();
    }

    default String getStudentName(ExamSubmission submission) {
        if (submission == null || submission.getStudent() == null) {
            return null;
        }
        String firstName = submission.getStudent().getFirstName();
        String lastName = submission.getStudent().getLastName();
        if (firstName == null && lastName == null) {
            return submission.getStudent().getUsername();
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}

