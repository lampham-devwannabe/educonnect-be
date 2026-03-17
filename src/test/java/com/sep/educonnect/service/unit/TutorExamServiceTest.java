package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.exam.*;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamSubmissionMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.TutorExamService;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorExamService Unit Tests")
class TutorExamServiceTest {

    @Mock private ExamRepository examRepository;

    @Mock private ExamSubmissionRepository examSubmissionRepository;

    @Mock private ExamAnswerRepository examAnswerRepository;

    @Mock private QuizRepository quizRepository;

    @Mock private LessonRepository lessonRepository;

    @Mock private CourseRepository courseRepository;

    @Mock private ExamSubmissionMapper examSubmissionMapper;

    @InjectMocks private TutorExamService tutorExamService;

    private Exam exam;
    private Lesson lesson;
    private ExamSubmission submission;
    private Quiz quiz;
    private ExamAnswer examAnswer;

    private static @NotNull ExamSubmission getExamSubmission(String studentId) {
        User student = new User();
        student.setUserId(studentId);
        student.setFirstName("Nguyễn");
        student.setLastName("Văn A");

        ExamSubmission submission = new ExamSubmission();
        submission.setSubmissionId(100L);
        submission.setExamId(1L);
        submission.setStudentId(studentId);
        submission.setScore(95.0);
        submission.setTotalQuestions(20);
        submission.setCorrectAnswers(19);
        submission.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        submission.setDurationSeconds(1800L);
        submission.setStudent(student);
        return submission;
    }

    // ===================== getExamResults Tests =====================

    private static @NotNull ExamSubmission getExamSubmission(Long submissionId) {
        User student = new User();
        student.setUserId("student-1");
        student.setFirstName("Nguyễn");
        student.setLastName("Văn A");

        ExamSubmission submission = new ExamSubmission();
        submission.setSubmissionId(submissionId);
        submission.setExamId(1L);
        submission.setStudentId("student-1");
        submission.setScore(95.0);
        submission.setTotalQuestions(20);
        submission.setCorrectAnswers(19);
        submission.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        submission.setDurationSeconds(1800L);
        submission.setStudent(student);
        return submission;
    }

    @Test
    @DisplayName("GER01 - Should get exam results with valid examId=1 and tutorId")
    void GER01_should_getExamResults_withValidExamId1() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L))
                .thenReturn(Collections.singletonList(submission));
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examAnswerRepository.findBySubmissionId(100L))
                .thenReturn(Collections.singletonList(examAnswer));

        ExamResultResponse resultResponse = new ExamResultResponse();
        resultResponse.setSubmissionId(100L);
        when(examSubmissionMapper.toResultResponse(submission)).thenReturn(resultResponse);

        ExamAnswerResponse answerResponse = new ExamAnswerResponse();
        answerResponse.setQuizId(1L);
        when(examSubmissionMapper.toAnswerResponse(examAnswer)).thenReturn(answerResponse);

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Math Final Exam", result.getExamTitle());
        assertEquals("Advanced Mathematics", result.getLessonTitle());
        assertEquals(1, result.getTotalSubmissions());
        assertEquals(1, result.getResults().size());
        verify(examRepository).findById(1L);
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        verify(examSubmissionRepository).findByExamId(1L);
    }

    @Test
    @DisplayName("GER02 - Should get exam results with valid examId=2")
    void GER02_should_getExamResults_withValidExamId2() {
        // Given
        String tutorId = "tutor-1";
        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setField("Physics Final Exam");

        when(examRepository.findById(2L)).thenReturn(Optional.of(exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(2L)).thenReturn(List.of());
        when(quizRepository.findByExamId(2L)).thenReturn(List.of());

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(2L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getExamId());
        assertEquals("Physics Final Exam", result.getExamTitle());
        assertEquals(0, result.getTotalSubmissions());
        assertEquals(0, result.getResults().size());
        verify(examRepository).findById(2L);
    }

    @Test
    @DisplayName("GER03 - Should throw EXAM_NOT_EXISTED when examId=0")
    void GER03_should_throwExamNotExisted_withExamIdZero() {
        // Given
        when(examRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorExamService.getExamResults(0L, "tutor-1"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
        verify(examRepository).findById(0L);
        verify(lessonRepository, never()).findById(anyLong());
        verify(courseRepository, never()).existsByTutorIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("GER04 - Should throw EXAM_NOT_EXISTED when exam not found")
    void GER04_should_throwExamNotExisted_withNonExistentExam() {
        // Given
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorExamService.getExamResults(999L, "tutor-1"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
        verify(examRepository).findById(999L);
    }

    @Test
    @DisplayName("GER05 - Should throw LESSON_NOT_EXISTED when lesson not found")
    void GER05_should_throwLessonNotExisted_whenLessonNotFound() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorExamService.getExamResults(1L, tutorId));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(examRepository).findById(1L);
        verify(lessonRepository).findById(10L);
        verify(courseRepository, never()).existsByTutorIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("GER06 - Should throw INVALID_EXAM_OWNER when tutorId='a' (not owner)")
    void GER06_should_throwInvalidExamOwner_withTutorIdA() {
        // Given
        String wrongTutorId = "a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamResults(1L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(courseRepository).existsByTutorIdAndLessonId(wrongTutorId, 10L);
        verify(examSubmissionRepository, never()).findByExamId(anyLong());
    }

    @Test
    @DisplayName("GER07 - Should throw INVALID_EXAM_OWNER with different tutor")
    void GER07_should_throwInvalidExamOwner_withDifferentTutor() {
        // Given
        String wrongTutorId = "tutor-999";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamResults(1L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamId(anyLong());
    }

    @Test
    @DisplayName("GER08 - Should return empty results when no submissions")
    void GER08_should_returnEmptyResults_withNoSubmissions() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L)).thenReturn(List.of());
        when(quizRepository.findByExamId(1L)).thenReturn(List.of());

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals(0, result.getTotalSubmissions());
        assertEquals(0, result.getResults().size());
        verify(examSubmissionRepository).findByExamId(1L);
    }

    @Test
    @DisplayName("GER09 - Should return multiple results with multiple submissions")
    void GER09_should_returnMultipleResults_withMultipleSubmissions() {
        // Given
        String tutorId = "tutor-1";

        ExamSubmission submission1 = new ExamSubmission();
        submission1.setSubmissionId(100L);
        submission1.setExamId(1L);
        submission1.setStudentId("student-1");
        submission1.setScore(85.0);

        ExamSubmission submission2 = new ExamSubmission();
        submission2.setSubmissionId(101L);
        submission2.setExamId(1L);
        submission2.setStudentId("student-2");
        submission2.setScore(90.0);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L))
                .thenReturn(Arrays.asList(submission1, submission2));
        when(quizRepository.findByExamId(1L)).thenReturn(List.of());
        when(examAnswerRepository.findBySubmissionId(100L)).thenReturn(List.of());
        when(examAnswerRepository.findBySubmissionId(101L)).thenReturn(List.of());

        ExamResultResponse result1 = new ExamResultResponse();
        result1.setSubmissionId(100L);
        ExamResultResponse result2 = new ExamResultResponse();
        result2.setSubmissionId(101L);

        when(examSubmissionMapper.toResultResponse(submission1)).thenReturn(result1);
        when(examSubmissionMapper.toResultResponse(submission2)).thenReturn(result2);

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalSubmissions());
        assertEquals(2, result.getResults().size());
        verify(examAnswerRepository).findBySubmissionId(100L);
        verify(examAnswerRepository).findBySubmissionId(101L);
    }

    @Test
    @DisplayName("GER10 - Should include quiz explanations in results")
    void GER10_should_includeQuizExplanations_inResults() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L))
                .thenReturn(Collections.singletonList(submission));
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examAnswerRepository.findBySubmissionId(100L))
                .thenReturn(Collections.singletonList(examAnswer));

        ExamResultResponse resultResponse = new ExamResultResponse();
        resultResponse.setSubmissionId(100L);
        when(examSubmissionMapper.toResultResponse(submission)).thenReturn(resultResponse);

        ExamAnswerResponse answerResponse = new ExamAnswerResponse();
        answerResponse.setQuizId(1L);
        when(examSubmissionMapper.toAnswerResponse(examAnswer)).thenReturn(answerResponse);

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        verify(quizRepository).findByExamId(1L);
        verify(examAnswerRepository).findBySubmissionId(100L);
    }

    @Test
    @DisplayName("GER11 - Should use lesson title when exam field is null")
    void GER11_should_useLessonTitle_whenExamFieldIsNull() {
        // Given
        String tutorId = "tutor-1";
        Exam examWithoutField = new Exam();
        examWithoutField.setExamId(1L);
        examWithoutField.setLessonId(10L);
        examWithoutField.setField(null);

        when(examRepository.findById(1L)).thenReturn(Optional.of(examWithoutField));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L)).thenReturn(List.of());
        when(quizRepository.findByExamId(1L)).thenReturn(List.of());

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals("Advanced Mathematics", result.getExamTitle());
        assertEquals("Advanced Mathematics", result.getLessonTitle());
    }

    @Test
    @DisplayName("GER12 - Should verify all repository calls in correct order")
    void GER12_should_verifyRepositoryCalls_inCorrectOrder() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L)).thenReturn(List.of());
        when(quizRepository.findByExamId(1L)).thenReturn(List.of());

        // When
        tutorExamService.getExamResults(1L, tutorId);

        // Then - verify order
        var inOrder =
                inOrder(
                        examRepository,
                        lessonRepository,
                        courseRepository,
                        examSubmissionRepository,
                        quizRepository);
        inOrder.verify(examRepository).findById(1L);
        inOrder.verify(lessonRepository).findById(10L);
        inOrder.verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        inOrder.verify(examSubmissionRepository).findByExamId(1L);
        inOrder.verify(quizRepository).findByExamId(1L);
    }

    @BeforeEach
    void setUp() {
        exam = new Exam();
        exam.setExamId(1L);
        exam.setLessonId(10L);
        exam.setField("Math Final Exam");

        lesson = new Lesson();
        lesson.setLessonId(10L);
        lesson.setTitle("Advanced Mathematics");

        submission = new ExamSubmission();
        submission.setSubmissionId(100L);
        submission.setExamId(1L);
        submission.setStudentId("student-1");
        submission.setScore(85.0);

        quiz = new Quiz();
        quiz.setQuizId(1L);
        quiz.setExamId(1L);
        quiz.setText("What is 2+2?");
        quiz.setValidAnswer("4");
        quiz.setExplanation("Basic addition");

        examAnswer = new ExamAnswer();
        examAnswer.setAnswerId(1L);
        examAnswer.setSubmissionId(100L);
        examAnswer.setQuizId(1L);
        examAnswer.setStudentAnswer("4");
        examAnswer.setCorrectAnswer("4");
        examAnswer.setIsCorrect(true);
    }

    @Test
    @DisplayName("Should get exam results successfully with examId=1 and valid tutorId")
    void should_getExamResults_successfully_withExamId1() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(1L))
                .thenReturn(Collections.singletonList(submission));
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));
        when(examAnswerRepository.findBySubmissionId(100L))
                .thenReturn(Collections.singletonList(examAnswer));

        ExamResultResponse resultResponse = new ExamResultResponse();
        resultResponse.setSubmissionId(100L);
        when(examSubmissionMapper.toResultResponse(submission)).thenReturn(resultResponse);

        ExamAnswerResponse answerResponse = new ExamAnswerResponse();
        answerResponse.setQuizId(1L);
        when(examSubmissionMapper.toAnswerResponse(examAnswer)).thenReturn(answerResponse);

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Math Final Exam", result.getExamTitle());
        assertEquals("Advanced Mathematics", result.getLessonTitle());
        assertEquals(1, result.getTotalSubmissions());
        assertEquals(1, result.getResults().size());
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
    }

    @Test
    @DisplayName("Should get exam results successfully with examId=2 and valid tutorId")
    void should_getExamResults_successfully_withExamId2() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setField("Physics Final Exam");

        when(examRepository.findById(2L)).thenReturn(Optional.of(exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamId(2L)).thenReturn(List.of());
        when(quizRepository.findByExamId(2L)).thenReturn(List.of());

        // When
        ExamResultsResponse result = tutorExamService.getExamResults(2L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getExamId());
        assertEquals("Physics Final Exam", result.getExamTitle());
        assertEquals("Advanced Mathematics", result.getLessonTitle());
        assertEquals(0, result.getTotalSubmissions());
        assertEquals(0, result.getResults().size());
    }

    @Test
    @DisplayName("Should throw EXAM_NOT_EXISTED when examId is 0")
    void should_throwExamNotExisted_when_examIdIsZero() {
        // Given
        when(examRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorExamService.getExamResults(
                                        0L, "010af93d-365d-4762-bcc8-51f153290b3a"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw LESSON_NOT_EXISTED when lesson not found")
    void should_throwLessonNotExisted_when_lessonNotFound() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorExamService.getExamResults(1L, tutorId));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
    }

    // ========== Tests for getExamStatistics ==========

    @Test
    @DisplayName("Should throw INVALID_EXAM_OWNER when tutorId is 'a' (not owner)")
    void should_throwInvalidExamOwner_when_tutorIdIsA() {
        // Given
        String wrongTutorId = "a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamResults(1L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamId(anyLong());
    }

    @Test
    @DisplayName("Should throw INVALID_EXAM_OWNER when tutor is not owner with examId=2")
    void should_throwInvalidExamOwner_when_tutorNotOwnerWithExamId2() {
        // Given
        String wrongTutorId = "a";
        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setField("Physics Final Exam");

        when(examRepository.findById(2L)).thenReturn(Optional.of(exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamResults(2L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamId(anyLong());
    }

    @Test
    @DisplayName("Should get exam statistics successfully")
    void should_getExamStatistics_successfully() {
        // Given
        String tutorId = "tutor-1";
        String studentId1 = "student-1";
        String studentId2 = "student-2";

        // Setup submissions with students
        User student1 = new User();
        student1.setUserId(studentId1);
        student1.setFirstName("Nguyễn");
        student1.setLastName("Văn A");

        User student2 = new User();
        student2.setUserId(studentId2);
        student2.setFirstName("Trần");
        student2.setLastName("Thị B");

        ExamSubmission submission1 = new ExamSubmission();
        submission1.setSubmissionId(100L);
        submission1.setExamId(1L);
        submission1.setStudentId(studentId1);
        submission1.setScore(95.0);
        submission1.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        submission1.setStudent(student1);

        ExamSubmission submission2 = new ExamSubmission();
        submission2.setSubmissionId(101L);
        submission2.setExamId(1L);
        submission2.setStudentId(studentId1);
        submission2.setScore(88.0);
        submission2.setSubmittedAt(LocalDateTime.of(2024, 1, 10, 10, 0));
        submission2.setStudent(student1);

        ExamSubmission submission3 = new ExamSubmission();
        submission3.setSubmissionId(102L);
        submission3.setExamId(1L);
        submission3.setStudentId(studentId2);
        submission3.setScore(87.5);
        submission3.setSubmittedAt(LocalDateTime.of(2024, 1, 12, 9, 0));
        submission3.setStudent(student2);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(submission1, submission2, submission3));

        // Service calculates statistics from submissions in-memory
        // Only stub getStudentName for first submission of each student
        when(examSubmissionMapper.getStudentName(submission1)).thenReturn("Nguyễn Văn A");
        when(examSubmissionMapper.getStudentName(submission3)).thenReturn("Trần Thị B");

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Math Final Exam", result.getExamTitle());
        assertEquals("Advanced Mathematics", result.getLessonTitle());
        assertEquals(3, result.getTotalSubmissions());
        assertEquals(2, result.getTotalStudents());
        assertEquals(2, result.getStudentStatistics().size());

        StudentStatisticsResponse stat1 =
                result.getStudentStatistics().stream()
                        .filter(s -> s.getStudentId().equals(studentId1))
                        .findFirst()
                        .orElse(null);
        assertNotNull(stat1);
        assertEquals("Nguyễn Văn A", stat1.getStudentName());
        assertEquals(2, stat1.getAttemptCount());
        assertEquals(95.0, stat1.getBestScore());
        assertEquals(91.5, stat1.getAverageScore());
    }

    // ===================== Comprehensive getExamStatistics Tests =====================

    @Test
    @DisplayName("GES01 - Should get exam statistics with valid examId=1 and valid tutorId")
    void GES01_should_getExamStatistics_withValidExamId1AndTutorId() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        String studentId1 = "student-1";
        String studentId2 = "student-2";

        User student1 = new User();
        student1.setUserId(studentId1);
        student1.setFirstName("Nguyễn");
        student1.setLastName("Văn A");

        User student2 = new User();
        student2.setUserId(studentId2);
        student2.setFirstName("Trần");
        student2.setLastName("Thị B");

        ExamSubmission submission1 = new ExamSubmission();
        submission1.setSubmissionId(100L);
        submission1.setExamId(1L);
        submission1.setStudentId(studentId1);
        submission1.setScore(95.0);
        submission1.setStudent(student1);

        ExamSubmission submission2 = new ExamSubmission();
        submission2.setSubmissionId(101L);
        submission2.setExamId(1L);
        submission2.setStudentId(studentId2);
        submission2.setScore(87.5);
        submission2.setStudent(student2);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(submission1, submission2));
        when(examSubmissionMapper.getStudentName(submission1)).thenReturn("Nguyễn Văn A");
        when(examSubmissionMapper.getStudentName(submission2)).thenReturn("Trần Thị B");

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals(2, result.getTotalSubmissions());
        assertEquals(2, result.getTotalStudents());
        verify(examRepository).findById(1L);
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
    }

    @Test
    @DisplayName("GES02 - Should get exam statistics with examId=2")
    void GES02_should_getExamStatistics_withExamId2() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setField("Physics");

        when(examRepository.findById(2L)).thenReturn(Optional.of(exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(2L)).thenReturn(List.of());

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(2L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getExamId());
        assertEquals(0, result.getTotalSubmissions());
        verify(examRepository).findById(2L);
    }

    @Test
    @DisplayName("GES03 - Should throw EXAM_NOT_EXISTED when examId=0")
    void GES03_should_throwExamNotExisted_withExamIdZero() {
        // Given
        when(examRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamStatistics(0L, "tutor-1"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("GES04 - Should throw EXAM_NOT_EXISTED with non-existent examId")
    void GES04_should_throwExamNotExisted_withNonExistentExam() {
        // Given
        when(examRepository.findById(888L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamStatistics(888L, "tutor-1"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("GES05 - Should throw LESSON_NOT_EXISTED when lesson not found")
    void GES05_should_throwLessonNotExisted_inGetStatistics() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> tutorExamService.getExamStatistics(1L, tutorId));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(courseRepository, never()).existsByTutorIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("GES06 - Should throw INVALID_EXAM_OWNER when tutorId='a'")
    void GES06_should_throwInvalidOwner_withTutorIdA() {
        // Given
        String wrongTutorId = "a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamStatistics(1L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamIdWithStudent(anyLong());
    }

    @Test
    @DisplayName("GES07 - Should calculate average score correctly for multiple attempts")
    void GES07_should_calculateAverageScore_correctly() {
        // Given
        String tutorId = "tutor-1";
        User student = new User();
        student.setUserId("student-1");

        ExamSubmission sub1 = new ExamSubmission();
        sub1.setStudentId("student-1");
        sub1.setScore(80.0);
        sub1.setStudent(student);

        ExamSubmission sub2 = new ExamSubmission();
        sub2.setStudentId("student-1");
        sub2.setScore(90.0);
        sub2.setStudent(student);

        ExamSubmission sub3 = new ExamSubmission();
        sub3.setStudentId("student-1");
        sub3.setScore(85.0);
        sub3.setStudent(student);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(sub1, sub2, sub3));
        when(examSubmissionMapper.getStudentName(sub1)).thenReturn("Student");

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        StudentStatisticsResponse stat = result.getStudentStatistics().get(0);
        assertEquals(90.0, stat.getBestScore());
        assertEquals(85.0, stat.getAverageScore(), 0.01);
        assertEquals(3, stat.getAttemptCount());
    }

    @Test
    @DisplayName("GES08 - Should handle single submission per student")
    void GES08_should_handleSingleSubmission() {
        // Given
        String tutorId = "tutor-1";
        User student = new User();
        student.setUserId("student-1");

        ExamSubmission submission = new ExamSubmission();
        submission.setStudentId("student-1");
        submission.setScore(75.0);
        submission.setStudent(student);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Collections.singletonList(submission));
        when(examSubmissionMapper.getStudentName(submission)).thenReturn("Student");

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        StudentStatisticsResponse stat = result.getStudentStatistics().get(0);
        assertEquals(75.0, stat.getBestScore());
        assertEquals(75.0, stat.getAverageScore());
        assertEquals(1, stat.getAttemptCount());
    }

    @Test
    @DisplayName("GES09 - Should group submissions by studentId correctly")
    void GES09_should_groupByStudentId() {
        // Given
        String tutorId = "tutor-1";

        User student1 = new User();
        student1.setUserId("student-1");
        User student2 = new User();
        student2.setUserId("student-2");

        ExamSubmission sub1 = new ExamSubmission();
        sub1.setStudentId("student-1");
        sub1.setScore(80.0);
        sub1.setStudent(student1);

        ExamSubmission sub2 = new ExamSubmission();
        sub2.setStudentId("student-2");
        sub2.setScore(90.0);
        sub2.setStudent(student2);

        ExamSubmission sub3 = new ExamSubmission();
        sub3.setStudentId("student-1");
        sub3.setScore(85.0);
        sub3.setStudent(student1);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(sub1, sub2, sub3));
        when(examSubmissionMapper.getStudentName(any())).thenReturn("Name");

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        assertEquals(3, result.getTotalSubmissions());
        assertEquals(2, result.getTotalStudents());
        assertEquals(2, result.getStudentStatistics().size());
    }

    @Test
    @DisplayName("GES10 - Should verify repository method calls")
    void GES10_should_verifyRepositoryCalls() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of());

        // When
        tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        verify(examRepository).findById(1L);
        verify(lessonRepository).findById(10L);
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        verify(examSubmissionRepository).findByExamIdWithStudent(1L);
    }

    // ========== Tests for getExamAttempts ==========

    // ===================== Comprehensive getExamAttempts Tests =====================

    @Test
    @DisplayName("GEA01 - Should get exam attempts with valid examId=1 and no studentId filter")
    void GEA01_should_getExamAttempts_withValidExamId1NoFilter() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        String studentId = "student-1";

        User student = new User();
        student.setUserId(studentId);
        student.setFirstName("Nguyễn");
        student.setLastName("Văn A");

        ExamSubmission submission1 = new ExamSubmission();
        submission1.setSubmissionId(100L);
        submission1.setExamId(1L);
        submission1.setStudentId(studentId);
        submission1.setScore(95.0);
        submission1.setTotalQuestions(20);
        submission1.setCorrectAnswers(19);
        submission1.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        submission1.setDurationSeconds(1800L);
        submission1.setStudent(student);

        ExamSubmission submission2 = new ExamSubmission();
        submission2.setSubmissionId(101L);
        submission2.setExamId(1L);
        submission2.setStudentId(studentId);
        submission2.setScore(88.0);
        submission2.setTotalQuestions(20);
        submission2.setCorrectAnswers(17);
        submission2.setSubmittedAt(LocalDateTime.of(2024, 1, 12, 11, 20));
        submission2.setDurationSeconds(2100L);
        submission2.setStudent(student);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(submission1, submission2));
        when(examSubmissionMapper.getStudentName(submission1)).thenReturn("Nguyễn Văn A");
        when(examSubmissionMapper.getStudentName(submission2)).thenReturn("Nguyễn Văn A");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Math Final Exam", result.getExamTitle());
        assertEquals(2, result.getTotalAttempts());
        assertEquals(2, result.getAttempts().size());
        verify(examRepository).findById(1L);
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        verify(examSubmissionRepository).findByExamIdWithStudent(1L);
    }

    @Test
    @DisplayName("GEA02 - Should get exam attempts with examId=2")
    void GEA02_should_getExamAttempts_withExamId2() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        Exam exam2 = new Exam();
        exam2.setExamId(2L);
        exam2.setLessonId(10L);
        exam2.setField("Physics Exam");

        when(examRepository.findById(2L)).thenReturn(Optional.of(exam2));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(2L)).thenReturn(List.of());

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(2L, tutorId, null);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getExamId());
        assertEquals(0, result.getTotalAttempts());
        verify(examRepository).findById(2L);
    }

    @Test
    @DisplayName("GEA03 - Should get exam attempts with valid examId=1 and studentId filter")
    void GEA03_should_getExamAttempts_withStudentIdFilter() {
        // Given
        String tutorId = "010af93d-365d-4762-bcc8-51f153290b3a";
        String studentId = "student-1";

        ExamSubmission submission = getExamSubmission(studentId);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(
                        1L, studentId))
                .thenReturn(List.of(submission));
        when(examSubmissionMapper.getStudentName(submission)).thenReturn("Nguyễn Văn A");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, studentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAttempts());
        assertEquals(1, result.getAttempts().size());
        assertEquals(studentId, result.getAttempts().getFirst().getStudentId());
        verify(examSubmissionRepository)
                .findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(1L, studentId);
    }

    @Test
    @DisplayName("GEA04 - Should throw EXAM_NOT_EXISTED when examId=0")
    void GEA04_should_throwExamNotExisted_withExamIdZero() {
        // Given
        when(examRepository.findById(0L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(0L, "tutor-1", null));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("GEA05 - Should throw EXAM_NOT_EXISTED with non-existent examId")
    void GEA05_should_throwExamNotExisted_withNonExistentExam() {
        // Given
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(999L, "tutor-1", null));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("GEA06 - Should throw LESSON_NOT_EXISTED when lesson not found")
    void GEA06_should_throwLessonNotExisted_inGetAttempts() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(1L, tutorId, null));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(courseRepository, never()).existsByTutorIdAndLessonId(anyString(), anyLong());
    }

    @Test
    @DisplayName("GEA07 - Should throw INVALID_EXAM_OWNER when tutorId='a'")
    void GEA07_should_throwInvalidOwner_withTutorIdA() {
        // Given
        String wrongTutorId = "a";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(1L, wrongTutorId, null));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamIdWithStudent(anyLong());
    }

    @Test
    @DisplayName("GEA08 - Should return empty attempts when no submissions")
    void GEA08_should_returnEmptyAttempts_withNoSubmissions() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of());

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalAttempts());
        assertEquals(0, result.getAttempts().size());
    }

    @Test
    @DisplayName("GEA09 - Should order attempts by submittedAt descending")
    void GEA09_should_orderAttempts_bySubmittedAtDesc() {
        // Given
        String tutorId = "tutor-1";
        User student = new User();
        student.setUserId("student-1");

        ExamSubmission older = new ExamSubmission();
        older.setSubmissionId(100L);
        older.setSubmittedAt(LocalDateTime.of(2024, 1, 10, 10, 0));
        older.setStudent(student);

        ExamSubmission newer = new ExamSubmission();
        newer.setSubmissionId(101L);
        newer.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        newer.setStudent(student);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(newer, older)); // Already ordered
        when(examSubmissionMapper.getStudentName(any())).thenReturn("Student");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        assertEquals(2, result.getAttempts().size());
        assertEquals(101L, result.getAttempts().get(0).getSubmissionId());
        assertEquals(100L, result.getAttempts().get(1).getSubmissionId());
    }

    @Test
    @DisplayName("GEA10 - Should include all submission details in attempts")
    void GEA10_should_includeAllDetails_inAttempts() {
        // Given
        String tutorId = "tutor-1";
        ExamSubmission submission = getExamSubmission("student-1");

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of(submission));
        when(examSubmissionMapper.getStudentName(submission)).thenReturn("Nguyễn Văn A");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        ExamAttemptSummaryResponse attempt = result.getAttempts().get(0);
        assertEquals(100L, attempt.getSubmissionId());
        assertEquals("student-1", attempt.getStudentId());
        assertEquals("Nguyễn Văn A", attempt.getStudentName());
        assertEquals(95.0, attempt.getScore());
        assertEquals(20, attempt.getTotalQuestions());
        assertEquals(19, attempt.getCorrectAnswers());
        assertEquals(1800L, attempt.getDurationSeconds());
    }

    @Test
    @DisplayName("GEA11 - Should filter by studentId correctly")
    void GEA11_should_filterByStudentId() {
        // Given
        String tutorId = "tutor-1";
        String studentId = "student-1";
        ExamSubmission submission = getExamSubmission(studentId);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(
                        1L, studentId))
                .thenReturn(List.of(submission));
        when(examSubmissionMapper.getStudentName(submission)).thenReturn("Name");

        // When
        tutorExamService.getExamAttempts(1L, tutorId, studentId);

        // Then
        verify(examSubmissionRepository)
                .findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(1L, studentId);
        verify(examSubmissionRepository, never()).findByExamIdWithStudent(anyLong());
    }

    @Test
    @DisplayName("GEA12 - Should verify repository calls in correct order")
    void GEA12_should_verifyRepositoryCalls_inCorrectOrder() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of());

        // When
        tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        var inOrder =
                inOrder(
                        examRepository,
                        lessonRepository,
                        courseRepository,
                        examSubmissionRepository);
        inOrder.verify(examRepository).findById(1L);
        inOrder.verify(lessonRepository).findById(10L);
        inOrder.verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        inOrder.verify(examSubmissionRepository).findByExamIdWithStudent(1L);
    }

    @Test
    @DisplayName("Should throw when tutor is not owner in getExamStatistics")
    void should_throwWhen_tutorNotOwner_inGetExamStatistics() {
        // Given
        String wrongTutorId = "tutor-999";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamStatistics(1L, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty submissions in getExamStatistics")
    void should_handleEmptySubmissions_inGetExamStatistics() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of());

        // When
        ExamStatisticsResponse result = tutorExamService.getExamStatistics(1L, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalSubmissions());
        assertEquals(0, result.getTotalStudents());
        assertEquals(0, result.getStudentStatistics().size());
    }

    @Test
    @DisplayName("Should get exam attempts successfully without filter")
    void should_getExamAttempts_successfully_withoutFilter() {
        // Given
        String tutorId = "tutor-1";
        String studentId = "student-1";

        User student = new User();
        student.setUserId(studentId);
        student.setFirstName("Nguyễn");
        student.setLastName("Văn A");

        ExamSubmission submission1 = new ExamSubmission();
        submission1.setSubmissionId(100L);
        submission1.setExamId(1L);
        submission1.setStudentId(studentId);
        submission1.setScore(95.0);
        submission1.setTotalQuestions(20);
        submission1.setCorrectAnswers(19);
        submission1.setSubmittedAt(LocalDateTime.of(2024, 1, 15, 14, 30));
        submission1.setDurationSeconds(1800L);
        submission1.setStudent(student);

        ExamSubmission submission2 = new ExamSubmission();
        submission2.setSubmissionId(101L);
        submission2.setExamId(1L);
        submission2.setStudentId(studentId);
        submission2.setScore(88.0);
        submission2.setTotalQuestions(20);
        submission2.setCorrectAnswers(17);
        submission2.setSubmittedAt(LocalDateTime.of(2024, 1, 12, 11, 20));
        submission2.setDurationSeconds(2100L);
        submission2.setStudent(student);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L))
                .thenReturn(Arrays.asList(submission1, submission2));

        when(examSubmissionMapper.getStudentName(submission1)).thenReturn("Nguyễn Văn A");
        when(examSubmissionMapper.getStudentName(submission2)).thenReturn("Nguyễn Văn A");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getExamId());
        assertEquals("Math Final Exam", result.getExamTitle());
        assertEquals(2, result.getTotalAttempts());
        assertEquals(2, result.getAttempts().size());
        verify(examRepository).findById(1L);
        verify(courseRepository).existsByTutorIdAndLessonId(tutorId, 10L);
        verify(examSubmissionRepository).findByExamIdWithStudent(1L);
    }

    @Test
    @DisplayName("Should get exam attempts successfully with studentId filter")
    void should_getExamAttempts_successfully_withStudentIdFilter() {
        // Given
        String tutorId = "tutor-1";
        String studentId = "student-1";

        ExamSubmission submission = getExamSubmission(studentId);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(
                        1L, studentId))
                .thenReturn(List.of(submission));

        when(examSubmissionMapper.getStudentName(submission)).thenReturn("Nguyễn Văn A");

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, studentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAttempts());
        assertEquals(1, result.getAttempts().size());
        assertEquals(studentId, result.getAttempts().getFirst().getStudentId());
        assertEquals("Nguyễn Văn A", result.getAttempts().getFirst().getStudentName());
        verify(examSubmissionRepository)
                .findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(1L, studentId);
        verify(examSubmissionRepository, never()).findByExamIdWithStudent(anyLong());
    }

    @Test
    @DisplayName("Should throw when exam not found in getExamAttempts")
    void should_throwWhen_examNotFound_inGetExamAttempts() {
        // Given
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(999L, "tutor-1", null));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty attempts in getExamAttempts")
    void should_handleEmptyAttempts_inGetExamAttempts() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findByExamIdWithStudent(1L)).thenReturn(List.of());

        // When
        ExamAttemptsResponse result = tutorExamService.getExamAttempts(1L, tutorId, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalAttempts());
        assertEquals(0, result.getAttempts().size());
    }

    @Test
    @DisplayName("Should throw LESSON_NOT_EXISTED when lesson not found in getExamAttempts")
    void should_throwLessonNotExisted_when_lessonNotFound_inGetExamAttempts() {
        // Given
        String tutorId = "tutor-1";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(1L, tutorId, null));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
    }

    // ========== Tests for getExamAttemptDetail ==========

    @Test
    @DisplayName("Should throw INVALID_EXAM_OWNER when tutor is not owner in getExamAttempts")
    void should_throwInvalidExamOwner_when_tutorNotOwner_inGetExamAttempts() {
        // Given
        String wrongTutorId = "tutor-999";
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttempts(1L, wrongTutorId, null));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
        verify(examSubmissionRepository, never()).findByExamIdWithStudent(anyLong());
        verify(examSubmissionRepository, never())
                .findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should get exam attempt detail successfully")
    void should_getExamAttemptDetail_successfully() {
        // Given
        String tutorId = "tutor-1";
        Long submissionId = 100L;

        ExamSubmission submission = getExamSubmission(submissionId);

        ExamResultResponse resultResponse = new ExamResultResponse();
        resultResponse.setSubmissionId(submissionId);
        resultResponse.setStudentId("student-1");
        resultResponse.setStudentName("Nguyễn Văn A");
        resultResponse.setScore(95.0);

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findBySubmissionIdWithStudent(submissionId))
                .thenReturn(Optional.of(submission));
        when(examSubmissionMapper.toResultResponse(submission)).thenReturn(resultResponse);
        when(examAnswerRepository.findBySubmissionId(submissionId))
                .thenReturn(Collections.singletonList(examAnswer));
        when(quizRepository.findByExamId(1L)).thenReturn(Collections.singletonList(quiz));

        ExamAnswerResponse answerResponse = new ExamAnswerResponse();
        answerResponse.setQuizId(1L);
        when(examSubmissionMapper.toAnswerResponse(examAnswer)).thenReturn(answerResponse);

        // When
        ExamResultResponse result =
                tutorExamService.getExamAttemptDetail(1L, submissionId, tutorId);

        // Then
        assertNotNull(result);
        assertEquals(submissionId, result.getSubmissionId());
        assertEquals(95.0, result.getScore());
        verify(examAnswerRepository).findBySubmissionId(submissionId);
        verify(quizRepository).findByExamId(1L);
    }

    @Test
    @DisplayName("Should throw when exam not found in getExamAttemptDetail")
    void should_throwWhen_examNotFound_inGetExamAttemptDetail() {
        // Given
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttemptDetail(999L, 100L, "tutor-1"));
        assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submission not found in getExamAttemptDetail")
    void should_throwWhen_submissionNotFound_inGetExamAttemptDetail() {
        // Given
        String tutorId = "tutor-1";
        Long submissionId = 999L;
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findBySubmissionIdWithStudent(submissionId))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttemptDetail(1L, submissionId, tutorId));
        assertEquals(ErrorCode.EXAM_SUBMISSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when submission does not belong to exam in getExamAttemptDetail")
    void should_throwWhen_submissionNotBelongToExam_inGetExamAttemptDetail() {
        // Given
        String tutorId = "tutor-1";
        Long submissionId = 100L;

        ExamSubmission submission = new ExamSubmission();
        submission.setSubmissionId(submissionId);
        submission.setExamId(999L); // Different exam ID
        submission.setStudentId("student-1");

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(tutorId, 10L)).thenReturn(true);
        when(examSubmissionRepository.findBySubmissionIdWithStudent(submissionId))
                .thenReturn(Optional.of(submission));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttemptDetail(1L, submissionId, tutorId));
        assertEquals(ErrorCode.EXAM_SUBMISSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw LESSON_NOT_EXISTED when lesson not found in getExamAttemptDetail")
    void should_throwLessonNotExisted_when_lessonNotFound_inGetExamAttemptDetail() {
        // Given
        String tutorId = "tutor-1";
        Long submissionId = 100L;
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> tutorExamService.getExamAttemptDetail(1L, submissionId, tutorId));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw when tutor is not owner in getExamAttemptDetail")
    void should_throwWhen_tutorNotOwner_inGetExamAttemptDetail() {
        // Given
        String wrongTutorId = "tutor-999";
        Long submissionId = 100L;
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.existsByTutorIdAndLessonId(wrongTutorId, 10L)).thenReturn(false);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () ->
                                tutorExamService.getExamAttemptDetail(
                                        1L, submissionId, wrongTutorId));
        assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
    }
}
