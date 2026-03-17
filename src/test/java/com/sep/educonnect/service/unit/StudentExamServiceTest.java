package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.exam.*;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamMapper;
import com.sep.educonnect.mapper.ExamSubmissionMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.StudentExamService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentExamService Unit Tests")
class StudentExamServiceTest {

    @Mock private ExamRepository examRepository;

    @Mock private QuizRepository quizRepository;

    @Mock private ExamSubmissionRepository examSubmissionRepository;

    @Mock private ExamAnswerRepository examAnswerRepository;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock private LessonRepository lessonRepository;

    @Mock private ExamMapper examMapper;

    @Mock private ExamSubmissionMapper examSubmissionMapper;

    @InjectMocks private StudentExamService studentExamService;

    private Exam exam;
    private Quiz quiz;
    private String studentId;

    @BeforeEach
    void setUp() {
        studentId = "student-1";

        exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        quiz = new Quiz();
        quiz.setQuizId(1L);
        quiz.setExamId(1L);
        quiz.setValidAnswer("A");
        quiz.setType("SINGLE_CHOICE");
    }

    @Test
    @DisplayName("Should get exam for student successfully")
    void should_getExamForStudent_successfully() {
        // Given
        ExamForStudentResponse response = new ExamForStudentResponse();
        response.setExamId(1L);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(examMapper.toExamForStudentResponse(exam)).thenReturn(response);

        // When
        ExamForStudentResponse result = studentExamService.getExamForStudent(1L, studentId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        verify(examRepository).findById(1L);
        verify(classEnrollmentRepository).existsByStudentIdAndLessonId(studentId, 10L);
    }

    @Test
    @DisplayName("Should throw when exam not found")
    void should_throwWhen_examNotFound() {
        // Given
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(999L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when exam not published")
    void should_throwWhen_examNotPublished() {
        // Given
        exam.setStatus("DRAFT");
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when student not enrolled")
    void should_throwWhen_studentNotEnrolled() {
        // Given
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_ACCESSIBLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_PUBLISHED when exam status is ARCHIVED")
    void should_throwExamNotPublished_when_examStatusIsArchived() {
        // Given
        exam.setStatus("ARCHIVED");
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
        verify(classEnrollmentRepository, never())
                .existsByStudentIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_PUBLISHED when exam status is DELETED")
    void should_throwExamNotPublished_when_examStatusIsDeleted() {
        // Given
        exam.setStatus("DELETED");
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_PUBLISHED when exam status is null")
    void should_throwExamNotPublished_when_examStatusIsNull() {
        // Given
        exam.setStatus(null);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_PUBLISHED when exam status is empty string")
    void should_throwExamNotPublished_when_examStatusIsEmpty() {
        // Given
        exam.setStatus("");
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get exam successfully with different student ID")
    void should_getExam_successfully_withDifferentStudentId() {
        // Given
        String differentStudentId = "student-999";
        ExamForStudentResponse response = new ExamForStudentResponse();
        response.setExamId(1L);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(differentStudentId, 10L))
                .thenReturn(true);
        when(examMapper.toExamForStudentResponse(exam)).thenReturn(response);

        // When
        ExamForStudentResponse result =
                studentExamService.getExamForStudent(1L, differentStudentId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        verify(examRepository).findById(1L);
        verify(classEnrollmentRepository).existsByStudentIdAndLessonId(differentStudentId, 10L);
    }

    @Test
    @DisplayName("Should validate enrollment before checking mapper")
    void should_validateEnrollment_beforeCheckingMapper() {
        // Given
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_ACCESSIBLE, exception.getErrorCode());
        verify(examMapper, never()).toExamForStudentResponse(any());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when exam ID is null")
    void should_throwExamNotExisted_when_examIdIsNull() {
        // Given
        when(examRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(null, studentId));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should check exam status is case-sensitive")
    void should_checkExamStatus_isCaseSensitive() {
        // Given
        exam.setStatus("published"); // lowercase instead of "PUBLISHED"
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when exam has invalid lessonId")
    void should_throw_when_examHasInvalidLessonId() {
        // Given
        exam.setLessonId(null);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        // When & Then - This should fail at enrollment check when lessonId is null
        assertThrows(Exception.class, () -> studentExamService.getExamForStudent(1L, studentId));
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when exam ID is negative")
    void should_throwExamNotExisted_when_examIdIsNegative() {
        // Given
        when(examRepository.findById(-1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(-1L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when exam ID is zero")
    void should_throwExamNotExisted_when_examIdIsZero() {
        // Given
        when(examRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(0L, studentId));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_ACCESSIBLE when student ID is empty")
    void should_throwExamNotAccessible_when_studentIdIsEmpty() {
        // Given
        String emptyStudentId = "";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(emptyStudentId, 10L))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.getExamForStudent(1L, emptyStudentId));
        assertEquals(ErrorCode.EXAM_NOT_ACCESSIBLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get exam successfully when exam has field value")
    void should_getExam_successfully_whenExamHasFieldValue() {
        // Given
        exam.setField("Mathematics Final Exam");
        ExamForStudentResponse response = new ExamForStudentResponse();
        response.setExamId(1L);
        response.setField("Mathematics Final Exam");

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(examMapper.toExamForStudentResponse(exam)).thenReturn(response);

        // When
        ExamForStudentResponse result = studentExamService.getExamForStudent(1L, studentId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Mathematics Final Exam", result.getField());
        verify(examRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get exam successfully for lesson with large ID")
    void should_getExam_successfully_forLessonWithLargeId() {
        // Given
        exam.setLessonId(999999L);
        ExamForStudentResponse response = new ExamForStudentResponse();
        response.setExamId(1L);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 999999L))
                .thenReturn(true);
        when(examMapper.toExamForStudentResponse(exam)).thenReturn(response);

        // When
        ExamForStudentResponse result = studentExamService.getExamForStudent(1L, studentId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        verify(classEnrollmentRepository).existsByStudentIdAndLessonId(studentId, 999999L);
    }

    @Test
    @DisplayName("Should verify enrollment check is called with correct parameters")
    void should_verifyEnrollmentCheck_isCalledWithCorrectParameters() {
        // Given
        String specificStudentId = "student-specific-123";
        Long specificLessonId = 456L;
        exam.setLessonId(specificLessonId);

        ExamForStudentResponse response = new ExamForStudentResponse();
        response.setExamId(1L);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(
                        specificStudentId, specificLessonId))
                .thenReturn(true);
        when(examMapper.toExamForStudentResponse(exam)).thenReturn(response);

        // When
        studentExamService.getExamForStudent(1L, specificStudentId);

        // Then
        verify(classEnrollmentRepository)
                .existsByStudentIdAndLessonId(specificStudentId, specificLessonId);
        verify(classEnrollmentRepository, times(1))
                .existsByStudentIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should submit exam successfully")
    void should_submitExam_successfully() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("A");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setScore(100.0);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setSubmissionId(100L);
        response.setAnswers(new java.util.ArrayList<>());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        when(examSubmissionMapper.toResponse(savedSubmission)).thenReturn(response);

        // When
        ExamSubmissionResponse result = studentExamService.submitExam(1L, studentId, request);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getSubmissionId());
        verify(examSubmissionRepository).save(any(ExamSubmission.class));
        verify(examAnswerRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw when invalid quiz IDs in submission")
    void should_throwWhen_invalidQuizIds() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(999L); // Invalid quiz ID

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.submitExam(1L, studentId, request));
        assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should calculate correct score")
    void should_calculateCorrectScore() {
        // Given
        Quiz quiz2 = new Quiz();
        quiz2.setQuizId(2L);
        quiz2.setExamId(1L);
        quiz2.setValidAnswer("B");

        QuizAnswerRequest answer1 = new QuizAnswerRequest();
        answer1.setQuizId(1L);
        answer1.setAnswer("A"); // Correct

        QuizAnswerRequest answer2 = new QuizAnswerRequest();
        answer2.setQuizId(2L);
        answer2.setAnswer("C"); // Wrong

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(Arrays.asList(answer1, answer2));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Arrays.asList(quiz, quiz2));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - verify submission was saved with 50% score (1 out of 2 correct)
        verify(examSubmissionRepository).save(argThat(submission -> submission.getScore() == 50.0));
    }

    @Test
    @DisplayName("Should normalize multiple choice answers with spaces")
    void should_normalizeMultipleChoiceAnswersWithSpaces() {
        // Given
        quiz.setType("MULTIPLE_CHOICE");
        quiz.setValidAnswer("A, B, C, D");

        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("D,C,B,A");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(101L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getScore() == 100.0
                                                && submission.getCorrectAnswers() == 1));
    }

    @Test
    @DisplayName("Should accept letter answers when validAnswer stores option text")
    void should_acceptLetterAnswers_whenValidAnswerIsOptionText() {
        quiz.setType("SINGLE_CHOICE");
        quiz.setValidAnswer("299,792,458 M/S");

        QuizOption optionA = new QuizOption();
        optionA.setText("150,000,000 M/S");
        optionA.setIsCorrect(false);

        QuizOption optionB = new QuizOption();
        optionB.setText("299,792,458 M/S");
        optionB.setIsCorrect(true);

        quiz.setOptions(Arrays.asList(optionA, optionB));

        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("B");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(102L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getCorrectAnswers() == 1
                                                && submission.getScore() == 100.0));
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when exam not found in submitExam")
    void should_throwExamNotExisted_when_examNotFound_inSubmitExam() {
        // Given
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of());
        request.setStartedAt(LocalDateTime.now());

        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.submitExam(999L, studentId, request));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_ACCESSIBLE when student not enrolled in submitExam")
    void should_throwExamNotAccessible_when_studentNotEnrolled_inSubmitExam() {
        // Given
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of());
        request.setStartedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.submitExam(1L, studentId, request));
        assertEquals(ErrorCode.EXAM_NOT_ACCESSIBLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when exam has no quizzes")
    void should_throwExamNotExisted_when_examHasNoQuizzes() {
        // Given
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of());
        request.setStartedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.emptyList());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.submitExam(1L, studentId, request));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should calculate 0% score when all answers are wrong")
    void should_calculate0Score_when_allAnswersWrong() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("B"); // Wrong answer (correct is A)

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getScore() == 0.0
                                                && submission.getCorrectAnswers() == 0));
    }

    @Test
    @DisplayName("Should handle empty answer strings")
    void should_handleEmptyAnswerStrings() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer(""); // Empty answer

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(argThat(submission -> submission.getCorrectAnswers() == 0));
    }

    @Test
    @DisplayName("Should handle null answer values")
    void should_handleNullAnswerValues() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer(null); // Null answer

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(argThat(submission -> submission.getCorrectAnswers() == 0));
    }

    @Test
    @DisplayName("Should handle whitespace-only answers")
    void should_handleWhitespaceOnlyAnswers() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("   "); // Whitespace only

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then
        verify(examSubmissionRepository)
                .save(argThat(submission -> submission.getCorrectAnswers() == 0));
    }

    @Test
    @DisplayName("Should handle partial quiz answers (not all quizzes answered)")
    void should_handlePartialQuizAnswers() {
        // Given - Exam has 2 quizzes but student only answers 1
        Quiz quiz2 = new Quiz();
        quiz2.setQuizId(2L);
        quiz2.setExamId(1L);
        quiz2.setValidAnswer("B");
        quiz2.setType("SINGLE_CHOICE");

        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("A"); // Correct

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest)); // Only answering quiz 1, not quiz 2
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Arrays.asList(quiz, quiz2));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Should calculate based on total questions (2), not answered (1)
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getTotalQuestions() == 2
                                                && submission.getCorrectAnswers() == 1
                                                && submission.getScore() == 50.0));
    }

    @Test
    @DisplayName("Should calculate duration correctly")
    void should_calculateDurationCorrectly() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 12, 5, 10, 0, 0);
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("A");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(startTime);

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(startTime);
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Verify duration was calculated
        verify(examSubmissionRepository)
                .save(argThat(submission -> submission.getDurationSeconds() != null));
    }

    @Test
    @DisplayName("Should handle case-insensitive answers")
    void should_handleCaseInsensitiveAnswers() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("a"); // lowercase but correct answer is "A"

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Should be marked correct due to case-insensitive comparison
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getCorrectAnswers() == 1
                                                && submission.getScore() == 100.0));
    }

    @Test
    @DisplayName("Should handle answers with extra spaces")
    void should_handleAnswersWithExtraSpaces() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("  A  "); // Answer with spaces

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Spaces should be trimmed and answer marked correct
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getCorrectAnswers() == 1
                                                && submission.getScore() == 100.0));
    }

    @Test
    @DisplayName("Should save all exam answers to database")
    void should_saveAllExamAnswersToDatabase() {
        // Given
        Quiz quiz2 = new Quiz();
        quiz2.setQuizId(2L);
        quiz2.setExamId(1L);
        quiz2.setValidAnswer("B");
        quiz2.setType("SINGLE_CHOICE");

        QuizAnswerRequest answer1 = new QuizAnswerRequest();
        answer1.setQuizId(1L);
        answer1.setAnswer("A");

        QuizAnswerRequest answer2 = new QuizAnswerRequest();
        answer2.setQuizId(2L);
        answer2.setAnswer("B");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(Arrays.asList(answer1, answer2));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Arrays.asList(quiz, quiz2));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Verify all answers were saved
        verify(examAnswerRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle duplicate quiz answers (take first one)")
    void should_handleDuplicateQuizAnswers() {
        // Given - Student answers same quiz twice
        QuizAnswerRequest answer1 = new QuizAnswerRequest();
        answer1.setQuizId(1L);
        answer1.setAnswer("A"); // Correct

        QuizAnswerRequest answer2 = new QuizAnswerRequest();
        answer2.setQuizId(1L);
        answer2.setAnswer("B"); // Wrong (but duplicate)

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(Arrays.asList(answer1, answer2));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Should save both answers (service processes all)
        verify(examAnswerRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle multiple choice with partial correct answers")
    void should_handleMultipleChoiceWithPartialCorrectAnswers() {
        // Given
        quiz.setType("MULTIPLE_CHOICE");
        quiz.setValidAnswer("A, B");

        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("A, C"); // Only A is correct, C is wrong

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(5));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(5));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Partial match should be marked as wrong
        verify(examSubmissionRepository)
                .save(
                        argThat(
                                submission ->
                                        submission.getCorrectAnswers() == 0
                                                && submission.getScore() == 0.0));
    }

    @Test
    @DisplayName("Should throw when submission reload fails")
    void should_throw_when_submissionReloadFails() {
        // Given
        QuizAnswerRequest answerRequest = new QuizAnswerRequest();
        answerRequest.setQuizId(1L);
        answerRequest.setAnswer("A");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answerRequest));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong()))
                .thenReturn(Optional.empty()); // Fail to reload
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> studentExamService.submitExam(1L, studentId, request));
        assertEquals(ErrorCode.EXAM_SUBMISSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should set submissionId for all exam answers")
    void should_setSubmissionIdForAllExamAnswers() {
        // Given
        Quiz quiz2 = new Quiz();
        quiz2.setQuizId(2L);
        quiz2.setExamId(1L);
        quiz2.setValidAnswer("B");
        quiz2.setType("SINGLE_CHOICE");

        QuizAnswerRequest answer1 = new QuizAnswerRequest();
        answer1.setQuizId(1L);
        answer1.setAnswer("A");

        QuizAnswerRequest answer2 = new QuizAnswerRequest();
        answer2.setQuizId(2L);
        answer2.setAnswer("B");

        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(Arrays.asList(answer1, answer2));
        request.setStartedAt(LocalDateTime.now().minusMinutes(10));

        ExamSubmission savedSubmission = new ExamSubmission();
        savedSubmission.setSubmissionId(100L);
        savedSubmission.setStartedAt(LocalDateTime.now().minusMinutes(10));
        savedSubmission.setSubmittedAt(LocalDateTime.now());

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(classEnrollmentRepository.existsByStudentIdAndLessonId(studentId, 10L))
                .thenReturn(true);
        when(quizRepository.findByExamId(1L)).thenReturn(Arrays.asList(quiz, quiz2));
        when(examSubmissionRepository.save(any(ExamSubmission.class))).thenReturn(savedSubmission);
        when(examSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(savedSubmission));
        when(examAnswerRepository.saveAll(anyList())).thenReturn(List.of(new ExamAnswer()));
        ExamSubmissionResponse response = new ExamSubmissionResponse();
        response.setAnswers(new java.util.ArrayList<>());
        when(examSubmissionMapper.toResponse(any())).thenReturn(response);

        // When
        studentExamService.submitExam(1L, studentId, request);

        // Then - Verify all answers have submissionId set
        verify(examAnswerRepository)
                .saveAll(
                        argThat(
                                answers -> {
                                    if (answers == null) return false;
                                    List<ExamAnswer> answerList = new java.util.ArrayList<>();
                                    answers.forEach(answerList::add);
                                    return answerList.size() == 2
                                            && answerList.stream()
                                                    .allMatch(
                                                            ans ->
                                                                    ans.getSubmissionId() != null
                                                                            && ans.getSubmissionId()
                                                                                    == 100L);
                                }));
    }

    @Test
    @DisplayName("Should get my exams with pagination")
    void should_getMyExams_withPagination() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.of(100.0));
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(1L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().getFirst().getExamId());
    }

    @Test
    @DisplayName("Should return empty page when student has no enrollments")
    void should_returnEmptyPage_when_studentHasNoEnrollments() {
        // Given
        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(Collections.emptyList());

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(lessonRepository, never()).findBySyllabusId(anyLong());
        verify(examRepository, never())
                .findByLessonIdInAndStatusAndClass(anyList(), anyString(), any());
    }

    @Test
    @DisplayName("Should filter exams by classId when provided")
    void should_filterExamsByClassId_when_classIdProvided() {
        // Given
        Long classId = 5L;
        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(classId);
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");
        exam.setTutorClassId(classId);

        when(classEnrollmentRepository.findByTutorClassIdAndStudentUserId(classId, studentId))
                .thenReturn(Optional.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(
                        anyList(), eq("PUBLISHED"), eq(classId)))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.of(90.0));
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(2L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, classId, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getExamId());
        verify(classEnrollmentRepository).findByTutorClassIdAndStudentUserId(classId, studentId);
        verify(classEnrollmentRepository, never())
                .findByStudentUserIdOrderByEnrolledAtDesc(anyString());
        verify(examRepository)
                .findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), eq(classId));
    }

    @Test
    @DisplayName("Should return empty page when student not enrolled in specific class")
    void should_returnEmptyPage_when_studentNotEnrolledInSpecificClass() {
        // Given
        Long classId = 5L;
        when(classEnrollmentRepository.findByTutorClassIdAndStudentUserId(classId, studentId))
                .thenReturn(Optional.empty());

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, classId, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(lessonRepository, never()).findBySyllabusId(anyLong());
        verify(examRepository, never())
                .findByLessonIdInAndStatusAndClass(anyList(), anyString(), any());
    }

    @Test
    @DisplayName("Should return empty page when syllabus has no lessons")
    void should_returnEmptyPage_when_syllabusHasNoLessons() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(Collections.emptyList());

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(examRepository, never())
                .findByLessonIdInAndStatusAndClass(anyList(), anyString(), any());
    }

    @Test
    @DisplayName("Should return empty page when lessons have no published exams")
    void should_returnEmptyPage_when_lessonsHaveNoPublishedExams() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(Collections.emptyList());

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should show submitted flag when student has submissions")
    void should_showSubmittedFlag_when_studentHasSubmissions() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.of(85.5));
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(3L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        StudentExamListItemResponse item = result.getContent().get(0);
        assertTrue(item.getSubmitted());
        assertEquals(85.5, item.getBestScore());
        assertEquals(3, item.getSubmissionCount());
    }

    @Test
    @DisplayName("Should show not submitted when student has no submissions")
    void should_showNotSubmitted_when_studentHasNoSubmissions() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        StudentExamListItemResponse item = result.getContent().get(0);
        assertFalse(item.getSubmitted());
        assertNull(item.getBestScore());
        assertEquals(0, item.getSubmissionCount());
    }

    @Test
    @DisplayName("Should use exam field as title when available")
    void should_useExamFieldAsTitle_when_available() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");
        exam.setField("Mathematics Final Exam");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Mathematics Final Exam", result.getContent().get(0).getExamTitle());
        assertEquals("Lesson 1", result.getContent().get(0).getLessonTitle());
    }

    @Test
    @DisplayName("Should use lesson title as exam title when field is null")
    void should_useLessonTitleAsExamTitle_when_fieldIsNull() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Introduction to Algorithms");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");
        exam.setField(null);

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Introduction to Algorithms", result.getContent().get(0).getExamTitle());
        assertEquals("Introduction to Algorithms", result.getContent().get(0).getLessonTitle());
    }

    @Test
    @DisplayName("Should handle multiple enrollments and aggregate exams")
    void should_handleMultipleEnrollments_and_aggregateExams() {
        // Given - Student enrolled in 2 classes
        TutorClass tutorClass1 = new TutorClass();
        Course course1 = new Course();
        Syllabus syllabus1 = new Syllabus();
        syllabus1.setSyllabusId(1L);
        course1.setSyllabus(syllabus1);
        tutorClass1.setCourse(course1);

        TutorClass tutorClass2 = new TutorClass();
        Course course2 = new Course();
        Syllabus syllabus2 = new Syllabus();
        syllabus2.setSyllabusId(2L);
        course2.setSyllabus(syllabus2);
        tutorClass2.setCourse(course2);

        ClassEnrollment enrollment1 = new ClassEnrollment();
        enrollment1.setTutorClass(tutorClass1);

        ClassEnrollment enrollment2 = new ClassEnrollment();
        enrollment2.setTutorClass(tutorClass2);

        Lesson lesson1 = new Lesson();
        lesson1.setLessonId(10L);
        lesson1.setTitle("Lesson 1");

        Lesson lesson2 = new Lesson();
        lesson2.setLessonId(20L);
        lesson2.setTitle("Lesson 2");

        Exam exam1 = new Exam();
        exam1.setExamId(1L);
        exam1.setLessonId(10L);
        exam1.setStatus("PUBLISHED");

        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(20L);
        exam2.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(Arrays.asList(enrollment1, enrollment2));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson1));
        when(lessonRepository.findBySyllabusId(2L)).thenReturn(List.of(lesson2));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(Arrays.asList(exam1, exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson1));
        when(lessonRepository.findById(20L)).thenReturn(Optional.of(lesson2));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.of(90.0));
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(1L);
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(2L, studentId))
                .thenReturn(Optional.of(85.0));
        when(examSubmissionRepository.countByExamIdAndStudentId(2L, studentId)).thenReturn(2L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Should paginate results correctly with page 0")
    void should_paginateResultsCorrectly_withPage0() {
        // Given - Create 3 exams
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam1 = new Exam();
        exam1.setExamId(1L);
        exam1.setLessonId(10L);
        exam1.setStatus("PUBLISHED");

        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setStatus("PUBLISHED");

        Exam exam3 = new Exam();
        exam3.setExamId(3L);
        exam3.setLessonId(10L);
        exam3.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(Arrays.asList(exam1, exam2, exam3));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(anyLong(), eq(studentId)))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(anyLong(), eq(studentId)))
                .thenReturn(0L);

        // When - Get first page with size 2
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertFalse(result.isLast());
    }

    @Test
    @DisplayName("Should paginate results correctly with page 1")
    void should_paginateResultsCorrectly_withPage1() {
        // Given - Create 3 exams
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam1 = new Exam();
        exam1.setExamId(1L);
        exam1.setLessonId(10L);
        exam1.setStatus("PUBLISHED");

        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setStatus("PUBLISHED");

        Exam exam3 = new Exam();
        exam3.setExamId(3L);
        exam3.setLessonId(10L);
        exam3.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(Arrays.asList(exam1, exam2, exam3));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(anyLong(), eq(studentId)))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(anyLong(), eq(studentId)))
                .thenReturn(0L);

        // When - Get second page with size 2
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 1, 2);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertTrue(result.isLast());
    }

    @Test
    @DisplayName("Should handle enrollment with null tutorClass")
    void should_handleEnrollment_withNullTutorClass() {
        // Given
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(null);

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(lessonRepository, never()).findBySyllabusId(anyLong());
    }

    @Test
    @DisplayName("Should handle enrollment with null course")
    void should_handleEnrollment_withNullCourse() {
        // Given
        TutorClass tutorClass = new TutorClass();
        tutorClass.setCourse(null);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(lessonRepository, never()).findBySyllabusId(anyLong());
    }

    @Test
    @DisplayName("Should handle enrollment with null syllabus")
    void should_handleEnrollment_withNullSyllabus() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        course.setSyllabus(null);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(lessonRepository, never()).findBySyllabusId(anyLong());
    }

    @Test
    @DisplayName("Should handle lesson not found in repository")
    void should_handleLessonNotFound_inRepository() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty()); // Lesson not found
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("", result.getContent().get(0).getLessonTitle()); // Empty string as default
        assertEquals(
                "", result.getContent().get(0).getExamTitle()); // Falls back to empty lesson title
    }

    @Test
    @DisplayName("Should handle large page size")
    void should_handleLargePageSize() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When - Request page size of 100
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 0, 100);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertTrue(result.isLast());
    }

    @Test
    @DisplayName("Should return empty page when page number exceeds total pages")
    void should_returnEmptyPage_when_pageNumberExceedsTotalPages() {
        // Given
        TutorClass tutorClass = new TutorClass();
        Course course = new Course();
        Syllabus syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        course.setSyllabus(syllabus);
        tutorClass.setCourse(course);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setTutorClass(tutorClass);

        Lesson lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Lesson 1");

        Exam exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setStatus("PUBLISHED");

        when(classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId))
                .thenReturn(List.of(enrollment));
        when(lessonRepository.findBySyllabusId(1L)).thenReturn(List.of(lesson));
        when(examRepository.findByLessonIdInAndStatusAndClass(anyList(), eq("PUBLISHED"), isNull()))
                .thenReturn(List.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(examSubmissionRepository.findBestScoreByExamIdAndStudentId(1L, studentId))
                .thenReturn(Optional.empty());
        when(examSubmissionRepository.countByExamIdAndStudentId(1L, studentId)).thenReturn(0L);

        // When - Request page 10 but only 1 exam exists
        Page<StudentExamListItemResponse> result =
                studentExamService.getMyExams(studentId, null, 10, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements()); // Still shows total elements
        assertEquals(10, result.getNumber()); // Shows requested page number
    }
}
