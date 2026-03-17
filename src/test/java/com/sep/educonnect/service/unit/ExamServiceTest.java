package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.exam.ExamRequest;
import com.sep.educonnect.dto.exam.ExamResponse;
import com.sep.educonnect.dto.exam.QuizOptionRequest;
import com.sep.educonnect.dto.exam.QuizRequest;
import com.sep.educonnect.dto.exam.QuizResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.ExamService;
import com.sep.educonnect.util.MockHelper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamService Unit Tests")
class ExamServiceTest {

        @Mock
        private ExamRepository examRepository;

        @Mock
        private QuizRepository quizRepository;

        @Mock
        private QuizOptionRepository quizOptionRepository;

        @Mock
        private ExamMapper examMapper;

        @Mock
        private LessonRepository lessonRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private TutorClassRepository tutorClassRepository;

        @Mock
        private EntityManager entityManager;

        @InjectMocks
        private ExamService examService;

        @AfterEach
        void tearDown() {
                MockHelper.clearSecurityContext();
        }

        // ========== Exam Tests ==========

        @Test
        @DisplayName("Should get exams by lesson with pagination and ascending sort")
        void should_getExamsByLesson_withAscendingSort() {
                // Given
                Long lessonId = 1L;
                int page = 0;
                int size = 10;
                String sortBy = "createdAt";
                String direction = "asc";

                Exam exam1 = Exam.builder()
                                .examId(1L)
                                .lessonId(lessonId)
                                .status("PUBLISHED")
                                .field("Math")
                                .build();

                Exam exam2 = Exam.builder()
                                .examId(2L)
                                .lessonId(lessonId)
                                .status("DRAFT")
                                .field("Science")
                                .build();

                List<Exam> exams = List.of(exam1, exam2);
                Page<Exam> examPage = new PageImpl<>(exams, PageRequest.of(page, size), exams.size());

                ExamResponse response1 = ExamResponse.builder()
                                .examId(1L)
                                .lessonId(lessonId)
                                .status("PUBLISHED")
                                .field("Math")
                                .build();

                ExamResponse response2 = ExamResponse.builder()
                                .examId(2L)
                                .lessonId(lessonId)
                                .status("DRAFT")
                                .field("Science")
                                .build();

                when(lessonRepository.findById(lessonId))
                                .thenReturn(Optional.of(new Lesson()));
                when(examRepository.findByLessonId(eq(lessonId), any(Pageable.class))).thenReturn(examPage);
                when(examMapper.toResponse(any(Exam.class))).thenReturn(response1, response2);

                // When
                Page<ExamResponse> result = examService.getExamsByLesson(lessonId, page, size, sortBy, direction);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(1L, result.getContent().get(0).getExamId());
                assertEquals(2L, result.getContent().get(1).getExamId());
                verify(examRepository).findByLessonId(eq(lessonId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get exams by lesson with descending sort")
        void should_getExamsByLesson_withDescendingSort() {
                // Given
                Long lessonId = 1L;
                int page = 0;
                int size = 10;
                String sortBy = "createdAt";
                String direction = "desc";

                Exam exam = Exam.builder()
                                .examId(1L)
                                .lessonId(lessonId)
                                .status("PUBLISHED")
                                .build();

                List<Exam> exams = List.of(exam);
                Page<Exam> examPage = new PageImpl<>(exams);

                ExamResponse response = ExamResponse.builder()
                                .examId(1L)
                                .lessonId(lessonId)
                                .status("PUBLISHED")
                                .build();

                when(lessonRepository.findById(lessonId))
                                .thenReturn(Optional.of(new Lesson()));
                when(examRepository.findByLessonId(eq(lessonId), any(Pageable.class))).thenReturn(examPage);
                when(examMapper.toResponse(any(Exam.class))).thenReturn(response);

                // When
                Page<ExamResponse> result = examService.getExamsByLesson(lessonId, page, size, sortBy, direction);

                // Then
                assertNotNull(result);
                verify(examRepository).findByLessonId(eq(lessonId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw exception when lesson not found for getExamsByLesson")
        void should_throwException_when_lessonNotFoundForGetExamsByLesson() {
                // Given
                Long lessonId = 999L;
                int page = 0;
                int size = 10;
                String sortBy = "createdAt";
                String direction = "asc";

                when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.getExamsByLesson(lessonId, page, size, sortBy, direction));

                assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
                verify(examRepository, never()).findByLessonId(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get exam by ID successfully")
        void should_getById_successfully() {
                // Given
                Long examId = 1L;
                Exam exam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("PUBLISHED")
                                .field("Math")
                                .build();

                ExamResponse expectedResponse = ExamResponse.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("PUBLISHED")
                                .field("Math")
                                .build();

                when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
                when(examMapper.toResponse(any(Exam.class))).thenReturn(expectedResponse);

                // When
                ExamResponse result = examService.getById(examId);

                // Then
                assertNotNull(result);
                assertEquals(examId, result.getExamId());
                assertEquals("PUBLISHED", result.getStatus());
                verify(examRepository).findById(examId);
                verify(examMapper).toResponse(exam);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when exam not found for getById")
        void should_throwException_when_examNotFoundForGetById() {
                // Given
                Long examId = 999L;
                when(examRepository.findById(examId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.getById(examId));

                assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode()); // " + examId,
                                                                                    // exception.getMessage());
                verify(examRepository).findById(examId);
                verify(examMapper, never()).toResponse(any(Exam.class));
        }

        @Test
        @DisplayName("Should create exam successfully")
        void should_createExam_successfully() {
                // Given
                ExamRequest request = ExamRequest.builder()
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                Exam exam = Exam.builder()
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                Exam savedExam = Exam.builder()
                                .examId(1L)
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                ExamResponse expectedResponse = ExamResponse.builder()
                                .examId(1L)
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                when(lessonRepository.findById(1L)).thenReturn(Optional.of(new Lesson()));
                when(examMapper.toEntity(request)).thenReturn(exam);
                when(examRepository.save(exam)).thenReturn(savedExam);
                when(examMapper.toResponse(savedExam)).thenReturn(expectedResponse);

                // When
                ExamResponse result = examService.createExam(request);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getExamId());
                assertEquals("DRAFT", result.getStatus());
                assertEquals("Math", result.getField());
                verify(examMapper).toEntity(request);
                verify(examRepository).save(exam);
                verify(examMapper).toResponse(savedExam);
        }

        @Test
        @DisplayName("Should throw exception when lesson not found for createExam")
        void should_throwException_when_lessonNotFoundForCreateExam() {
                // Given
                Long lessonId = 999L;
                ExamRequest request = ExamRequest.builder()
                                .lessonId(lessonId)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.createExam(request));

                assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
                verify(examMapper, never()).toEntity(any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when tutor class not found for createExam")
        void should_throwException_when_tutorClassNotFoundForCreateExam() {
                // Given
                Long lessonId = 1L;
                Long classId = 999L;

                ExamRequest request = ExamRequest.builder()
                                .lessonId(lessonId)
                                .tutorClassId(classId)
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

                when(lessonRepository.findById(lessonId))
                                .thenReturn(Optional.of(new Lesson()));
                when(tutorClassRepository.findById(classId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.createExam(request));

                assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
                verify(examMapper, never()).toEntity(any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not tutor of the class for createExam")
        void should_throwException_when_userNotTutorOfClassForCreateExam() {
                // Given
                Long lessonId = 1L;
                Long classId = 1L;
                String otherTutorId = "tutor-2";
                String username = "tutor-user";

                ExamRequest request = ExamRequest.builder()
                                .lessonId(lessonId)
                                .tutorClassId(classId)
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

               TutorClass tutorClass = com.sep.educonnect.entity.TutorClass.builder()
                                .id(classId)
                                .build();

                User tutor = User.builder()
                                .userId(otherTutorId)
                                .username(username)
                                .build();

                MockHelper.mockSecurityContext(username);
                when(lessonRepository.findById(lessonId))
                                .thenReturn(Optional.of(new Lesson()));
                when(tutorClassRepository.findById(classId)).thenReturn(Optional.of(tutorClass));
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(tutor));
                when(tutorClassRepository.findByIdAndTutorUserId(classId, otherTutorId))
                                .thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.createExam(request));

                assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
                verify(examMapper, never()).toEntity(any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update exam successfully")
        void should_updateExam_successfully() {
                // Given
                Long examId = 1L;
                String username = "tutor-user";
                String userId = "tutor-1";
                
                ExamRequest request = ExamRequest.builder()
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

                Exam existingExam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                Exam updatedExam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

                ExamResponse expectedResponse = ExamResponse.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

                User tutor = User.builder()
                                .userId(userId)
                                .username(username)
                                .build();

                MockHelper.mockSecurityContext(username);
                when(examRepository.findById(examId)).thenReturn(Optional.of(existingExam));
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(tutor));
                doNothing().when(examMapper).updateEntity(any(Exam.class), any(ExamRequest.class));
                when(examRepository.save(existingExam)).thenReturn(updatedExam);
                when(examMapper.toResponse(any(Exam.class))).thenReturn(expectedResponse);

                // When
                ExamResponse result = examService.updateExam(examId, request);

                // Then
                assertNotNull(result);
                assertEquals(examId, result.getExamId());
                assertEquals("PUBLISHED", result.getStatus());
                assertEquals("Science", result.getField());
                verify(examRepository).findById(examId);
                verify(examMapper).updateEntity(existingExam, request);
                verify(examRepository).save(existingExam);
                verify(examMapper).toResponse(updatedExam);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when exam not found for update")
        void should_throwException_when_examNotFoundForUpdate() {
                // Given
                Long examId = 999L;
                ExamRequest request = ExamRequest.builder()
                                .status("PUBLISHED")
                                .build();

                when(examRepository.findById(examId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.updateExam(examId, request));

                assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode()); // " + examId,
                                                                                    // exception.getMessage());
                verify(examRepository).findById(examId);
                verify(examMapper, never()).updateEntity(any(Exam.class), any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not tutor of existing exam's class for updateExam")
        void should_throwException_when_userNotTutorOfExistingExamClassForUpdate() {
                // Given
                Long examId = 1L;
                Long classId = 1L;
                String username = "tutor-user";
                String userId = "tutor-1";

                ExamRequest request = ExamRequest.builder()
                                .status("PUBLISHED")
                                .field("Science")
                                .build();

                Exam existingExam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .tutorClassId(classId)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                User tutor = User.builder()
                                .userId(userId)
                                .username(username)
                                .build();

                MockHelper.mockSecurityContext(username);
                when(examRepository.findById(examId)).thenReturn(Optional.of(existingExam));
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(tutor));
                when(tutorClassRepository.findByIdAndTutorUserId(classId, userId))
                                .thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.updateExam(examId, request));

                assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
                verify(examRepository).findById(examId);
                verify(userRepository).findByUsername(username);
                verify(tutorClassRepository).findByIdAndTutorUserId(classId, userId);
                verify(examMapper, never()).updateEntity(any(Exam.class), any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when class not found for updateExam")
        void should_throwException_when_classNotFoundForUpdateExam() {
                // Given
                Long examId = 1L;
                Long classId = 999L;
                String username = "tutor-user";
                String userId = "tutor-1";

                ExamRequest request = ExamRequest.builder()
                                .status("PUBLISHED")
                                .field("Science")
                                .tutorClassId(classId)
                                .build();

                Exam existingExam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                User tutor = User.builder()
                                .userId(userId)
                                .username(username)
                                .build();

                MockHelper.mockSecurityContext(username);
                when(examRepository.findById(examId)).thenReturn(Optional.of(existingExam));
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(tutor));
                when(tutorClassRepository.findById(classId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.updateExam(examId, request));

                assertEquals(ErrorCode.CLASS_NOT_FOUND, exception.getErrorCode());
                verify(examRepository).findById(examId);
                verify(userRepository).findByUsername(username);
                verify(tutorClassRepository).findById(classId);
                verify(tutorClassRepository, never()).findByIdAndTutorUserId(anyLong(), anyString());
                verify(examMapper, never()).updateEntity(any(Exam.class), any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not tutor of requested class for updateExam")
        void should_throwException_when_userNotTutorOfRequestedClassForUpdate() {
                // Given
                Long examId = 1L;
                Long classId = 1L;
                String username = "tutor-user";
                String userId = "tutor-1";

                ExamRequest request = ExamRequest.builder()
                                .status("PUBLISHED")
                                .field("Science")
                                .tutorClassId(classId)
                                .build();

                Exam existingExam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("DRAFT")
                                .field("Math")
                                .build();

                User tutor = User.builder()
                                .userId(userId)
                                .username(username)
                                .build();

                TutorClass tutorClass = TutorClass.builder()
                                .id(classId)
                                .build();

                MockHelper.mockSecurityContext(username);
                when(examRepository.findById(examId)).thenReturn(Optional.of(existingExam));
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(tutor));
                when(tutorClassRepository.findById(classId)).thenReturn(Optional.of(tutorClass));
                when(tutorClassRepository.findByIdAndTutorUserId(classId, userId))
                                .thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.updateExam(examId, request));

                assertEquals(ErrorCode.INVALID_EXAM_OWNER, exception.getErrorCode());
                verify(examRepository).findById(examId);
                verify(userRepository).findByUsername(username);
                verify(tutorClassRepository).findById(classId);
                verify(tutorClassRepository).findByIdAndTutorUserId(classId, userId);
                verify(examMapper, never()).updateEntity(any(Exam.class), any(ExamRequest.class));
                verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should delete exam successfully")
        void should_deleteExam_successfully() {
                // Given
                Long examId = 1L;
                Exam exam = Exam.builder()
                                .examId(examId)
                                .lessonId(1L)
                                .status("DRAFT")
                                .build();

                when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
                doNothing().when(examRepository).delete(exam);

                // When
                examService.deleteExam(examId);

                // Then
                verify(examRepository).findById(examId);
                verify(examRepository).delete(exam);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when exam not found for delete")
        void should_throwException_when_examNotFoundForDelete() {
                // Given
                Long examId = 999L;
                when(examRepository.findById(examId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.deleteExam(examId));

                assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode()); // " + examId,
                                                                                    // exception.getMessage());
                verify(examRepository).findById(examId);
                verify(examRepository, never()).delete(any());
        }

        // ========== Quiz Tests ==========

        @Test
        @DisplayName("Should create quiz with options successfully")
        void should_createQuiz_withOptions_successfully() {
                // Given
                QuizOptionRequest optionRequest1 = QuizOptionRequest.builder()
                                .text("Option A")
                                .isCorrect(true)
                                .build();

                QuizOptionRequest optionRequest2 = QuizOptionRequest.builder()
                                .text("Option B")
                                .isCorrect(false)
                                .build();

                QuizRequest request = QuizRequest.builder()
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .validAnswer("4")
                                .explanation("Basic math")
                                .options(List.of(optionRequest1, optionRequest2))
                                .build();

                Quiz quiz = Quiz.builder()
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .validAnswer("4")
                                .explanation("Basic math")
                                .build();

                Quiz savedQuiz = Quiz.builder()
                                .quizId(1L)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .validAnswer("4")
                                .explanation("Basic math")
                                .build();

                QuizOption option1 = QuizOption.builder()
                                .quizId(1L)
                                .text("Option A")
                                .isCorrect(true)
                                .build();

                QuizOption option2 = QuizOption.builder()
                                .quizId(1L)
                                .text("Option B")
                                .isCorrect(false)
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(1L)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .validAnswer("4")
                                .explanation("Basic math")
                                .build();

                when(examRepository.findById(1L)).thenReturn(Optional.of(Exam.builder().examId(1L).build())); // Added
                                                                                                              // for
                                                                                                              // createQuiz
                when(examMapper.toEntity(request)).thenReturn(quiz);
                when(quizRepository.save(quiz)).thenReturn(savedQuiz);
                when(examMapper.toEntity(optionRequest1)).thenReturn(option1);
                when(examMapper.toEntity(optionRequest2)).thenReturn(option2);
                when(quizOptionRepository.saveAll(anyList())).thenReturn(List.of(option1, option2));
                doNothing().when(entityManager).flush();
                doNothing().when(entityManager).refresh(savedQuiz);
                when(examMapper.toResponse(savedQuiz)).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.createQuiz(request);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getQuizId());
                assertEquals("What is 2+2?", result.getText());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<QuizOption>> optionsCaptor = ArgumentCaptor.forClass(List.class);
                verify(quizOptionRepository).saveAll(optionsCaptor.capture());
                List<QuizOption> savedOptions = optionsCaptor.getValue();
                assertEquals(2, savedOptions.size());
                assertEquals(1L, savedOptions.get(0).getQuizId());
                assertFalse(savedOptions.get(0).getIsDeleted());

                verify(entityManager).flush();
                verify(entityManager).refresh(savedQuiz);
                verify(examMapper).toResponse(savedQuiz);
        }

        @Test
        @DisplayName("Should create quiz without options successfully")
        void should_createQuiz_withoutOptions_successfully() {
                // Given
                QuizRequest request = QuizRequest.builder()
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("TRUE_FALSE")
                                .validAnswer("True")
                                .options(null)
                                .build();

                Quiz quiz = Quiz.builder()
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("TRUE_FALSE")
                                .validAnswer("True")
                                .build();

                Quiz savedQuiz = Quiz.builder()
                                .quizId(1L)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("TRUE_FALSE")
                                .validAnswer("True")
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(1L)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("TRUE_FALSE")
                                .validAnswer("True")
                                .build();

                // Define a dummy Exam object for the mock
                Exam dummyExam = Exam.builder().examId(1L).build();
                when(examRepository.findById(1L)).thenReturn(Optional.of(dummyExam));
                when(examMapper.toEntity(request)).thenReturn(quiz);
                when(quizRepository.save(quiz)).thenReturn(savedQuiz);
                doNothing().when(entityManager).flush();
                doNothing().when(entityManager).refresh(savedQuiz);
                when(examMapper.toResponse(savedQuiz)).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.createQuiz(request);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getQuizId());
                verify(quizOptionRepository, never()).saveAll(anyList());
                verify(entityManager).flush();
                verify(entityManager).refresh(savedQuiz);
        }

        @Test
        @DisplayName("Should throw exception when exam not found for createQuiz")
        void should_throwException_when_examNotFoundForCreateQuiz() {
                // Given
                Long examId = 999L;
                QuizRequest request = QuizRequest.builder()
                                .examId(examId)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .validAnswer("4")
                                .build();

                when(examRepository.findById(examId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.createQuiz(request));

                assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
                verify(examRepository).findById(examId);
                verify(examMapper, never()).toEntity(any(QuizRequest.class));
                verify(quizRepository, never()).save(any(Quiz.class));
                verify(quizOptionRepository, never()).saveAll(anyList());
                verify(entityManager, never()).flush();
                verify(entityManager, never()).refresh(any(Quiz.class));
        }

        @Test
        @DisplayName("Should update quiz with options successfully")
        void should_updateQuiz_withOptions_successfully() {
                // Given
                Long quizId = 1L;
                QuizOptionRequest optionRequest = QuizOptionRequest.builder()
                                .text("New Option")
                                .isCorrect(true)
                                .build();

                QuizRequest request = QuizRequest.builder()
                                .text("Updated question?")
                                .orderNo(2)
                                .type("MULTIPLE_CHOICE")
                                .validAnswer("A")
                                .explanation("Updated explanation")
                                .options(List.of(optionRequest))
                                .build();

                Quiz existingQuiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Original question?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .build();

                Quiz updatedQuiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Updated question?")
                                .orderNo(2)
                                .type("MULTIPLE_CHOICE")
                                .validAnswer("A")
                                .explanation("Updated explanation")
                                .build();

                QuizOption newOption = QuizOption.builder()
                                .quizId(quizId)
                                .text("New Option")
                                .isCorrect(true)
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Updated question?")
                                .orderNo(2)
                                .type("MULTIPLE_CHOICE")
                                .validAnswer("A")
                                .explanation("Updated explanation")
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(existingQuiz));
                doNothing().when(examMapper).updateEntity(any(Quiz.class), any(QuizRequest.class));
                when(quizRepository.save(existingQuiz)).thenReturn(updatedQuiz);
                doNothing().when(quizOptionRepository).deleteByQuizId(quizId);
                when(examMapper.toEntity(optionRequest)).thenReturn(newOption);
                when(quizOptionRepository.saveAll(anyList())).thenReturn(List.of(newOption));
                doNothing().when(entityManager).flush();
                doNothing().when(entityManager).refresh(updatedQuiz);
                when(examMapper.toResponse(any(Quiz.class))).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.updateQuiz(quizId, request);

                // Then
                assertNotNull(result);
                assertEquals(quizId, result.getQuizId());
                assertEquals("Updated question?", result.getText());
                verify(quizRepository).findById(quizId);
                verify(examMapper).updateEntity(existingQuiz, request);
                verify(quizOptionRepository).deleteByQuizId(quizId);
                verify(quizOptionRepository).saveAll(anyList());
                verify(entityManager).flush();
                verify(entityManager).refresh(updatedQuiz);
        }

        @Test
        @DisplayName("Should update quiz without options successfully")
        void should_updateQuiz_withoutOptions_successfully() {
                // Given
                Long quizId = 1L;
                QuizRequest request = QuizRequest.builder()
                                .text("Updated question?")
                                .orderNo(2)
                                .options(null)
                                .build();

                Quiz existingQuiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Original question?")
                                .orderNo(1)
                                .build();

                Quiz updatedQuiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Updated question?")
                                .orderNo(2)
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Updated question?")
                                .orderNo(2)
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(existingQuiz));
                doNothing().when(examMapper).updateEntity(any(Quiz.class), any(QuizRequest.class));
                when(quizRepository.save(existingQuiz)).thenReturn(updatedQuiz);
                doNothing().when(entityManager).flush();
                doNothing().when(entityManager).refresh(updatedQuiz);
                when(examMapper.toResponse(any(Quiz.class))).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.updateQuiz(quizId, request);

                // Then
                assertNotNull(result);
                assertEquals(quizId, result.getQuizId());
                verify(quizOptionRepository, never()).deleteByQuizId(anyLong());
                verify(quizOptionRepository, never()).saveAll(anyList());
                verify(entityManager).flush();
                verify(entityManager).refresh(updatedQuiz);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quiz not found for update")
        void should_throwException_when_quizNotFoundForUpdate() {
                // Given
                Long quizId = 999L;
                QuizRequest request = QuizRequest.builder()
                                .text("Updated question?")
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.updateQuiz(quizId, request));

                assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode()); // " + quizId,
                                                                                    // exception.getMessage());
                verify(quizRepository).findById(quizId);
                verify(examMapper, never()).updateEntity(any(Quiz.class), any(QuizRequest.class));
        }

        @Test
        @DisplayName("Should get quizzes by exam with pagination and sorting")
        void should_getQuizzesByExam_withPaginationAndSorting() {
                // Given
                Long examId = 1L;
                int page = 0;
                int size = 10;
                String sortBy = "orderNo";
                String direction = "asc";

                Quiz quiz1 = Quiz.builder()
                                .quizId(1L)
                                .examId(examId)
                                .text("Question 1")
                                .orderNo(1)
                                .build();

                Quiz quiz2 = Quiz.builder()
                                .quizId(2L)
                                .examId(examId)
                                .text("Question 2")
                                .orderNo(2)
                                .build();

                List<Quiz> quizzes = List.of(quiz1, quiz2);
                Page<Quiz> quizPage = new PageImpl<>(quizzes, PageRequest.of(page, size), quizzes.size());

                QuizResponse response1 = QuizResponse.builder()
                                .quizId(1L)
                                .examId(examId)
                                .text("Question 1")
                                .orderNo(1)
                                .build();

                QuizResponse response2 = QuizResponse.builder()
                                .quizId(2L)
                                .examId(examId)
                                .text("Question 2")
                                .orderNo(2)
                                .build();

                // Define a dummy Exam object for the mock
                Exam dummyExam = Exam.builder().examId(examId).build();
                when(examRepository.findById(examId)).thenReturn(Optional.of(dummyExam));
                when(quizRepository.findByExamIdOrderByOrderNoAsc(eq(examId), any(Pageable.class)))
                                .thenReturn(quizPage);
                when(examMapper.toResponse(any(Quiz.class))).thenReturn(response1, response2);

                // When
                Page<QuizResponse> result = examService.getQuizzesByExam(examId, page, size, sortBy, direction);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(1L, result.getContent().get(0).getQuizId());
                assertEquals(2L, result.getContent().get(1).getQuizId());
                verify(quizRepository).findByExamIdOrderByOrderNoAsc(eq(examId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw exception when exam not found for getQuizzesByExam")
        void should_throwException_when_examNotFoundForGetQuizzesByExam() {
                // Given
                Long examId = 999L;
                int page = 0;
                int size = 10;
                String sortBy = "orderNo";
                String direction = "asc";

                when(examRepository.findById(examId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class,
                                () -> examService.getQuizzesByExam(examId, page, size, sortBy, direction));

                assertEquals(ErrorCode.EXAM_NOT_EXISTED, exception.getErrorCode());
                verify(examRepository).findById(examId);
                verify(quizRepository, never()).findByExamIdOrderByOrderNoAsc(anyLong(), any(Pageable.class));
                verify(examMapper, never()).toResponse(any(Quiz.class));
        }

        @Test
        @DisplayName("Should get quiz by ID successfully")
        void should_getQuizById_successfully() {
                // Given
                Long quizId = 1L;
                Quiz quiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .type("SINGLE_CHOICE")
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
                when(examMapper.toResponse(any(Quiz.class))).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.getQuizById(quizId);

                // Then
                assertNotNull(result);
                assertEquals(quizId, result.getQuizId());
                assertEquals("What is 2+2?", result.getText());
                verify(quizRepository).findById(quizId);
                verify(examMapper).toResponse(quiz);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quiz not found for getQuizById")
        void should_throwException_when_quizNotFoundForGetQuizById() {
                // Given
                Long quizId = 999L;
                when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.getQuizById(quizId));

                assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode()); // " + quizId,
                                                                                    // exception.getMessage());
                verify(quizRepository).findById(quizId);
                verify(examMapper, never()).toResponse(any(Quiz.class));
        }

        @Test
        @DisplayName("Should get quiz by ID and exam ID successfully")
        void should_getQuizByIdAndExamId_successfully() {
                // Given
                Long quizId = 1L;
                Long examId = 1L;
                Quiz quiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(examId)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .build();

                QuizResponse expectedResponse = QuizResponse.builder()
                                .quizId(quizId)
                                .examId(examId)
                                .text("What is 2+2?")
                                .orderNo(1)
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
                when(examMapper.toResponse(any(Quiz.class))).thenReturn(expectedResponse);

                // When
                QuizResponse result = examService.getQuizByIdAndExamId(quizId, examId);

                // Then
                assertNotNull(result);
                assertEquals(quizId, result.getQuizId());
                assertEquals(examId, result.getExamId());
                verify(quizRepository).findById(quizId);
                verify(examMapper).toResponse(quiz);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quiz does not belong to exam")
        void should_throwException_when_quizDoesNotBelongToExam() {
                // Given
                Long quizId = 1L;
                Long examId = 1L;
                Long differentExamId = 2L;
                Quiz quiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(differentExamId)
                                .text("What is 2+2?")
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.getQuizByIdAndExamId(quizId, examId));

                assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode()); // "error.quiz.not.existed"
                verify(quizRepository).findById(quizId);
                verify(examMapper, never()).toResponse(any(Quiz.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quiz not found for getQuizByIdAndExamId")
        void should_throwException_when_quizNotFoundForGetQuizByIdAndExamId() {
                // Given
                Long quizId = 999L;
                Long examId = 1L;
                when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.getQuizByIdAndExamId(quizId, examId));

                assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode()); // " + quizId,
                                                                                    // exception.getMessage());
                verify(quizRepository).findById(quizId);
                verify(examMapper, never()).toResponse(any(Quiz.class));
        }

        @Test
        @DisplayName("Should delete quiz successfully")
        void should_deleteQuiz_successfully() {
                // Given
                Long quizId = 1L;
                Quiz quiz = Quiz.builder()
                                .quizId(quizId)
                                .examId(1L)
                                .text("Question to delete")
                                .orderNo(1)
                                .build();

                when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
                doNothing().when(quizRepository).delete(quiz);

                // When
                examService.deleteQuiz(quizId);

                // Then
                verify(quizRepository).findById(quizId);
                verify(quizRepository).delete(quiz);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quiz not found for delete")
        void should_throwException_when_quizNotFoundForDelete() {
                // Given
                Long quizId = 999L;
                when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> examService.deleteQuiz(quizId));

                assertEquals(ErrorCode.QUIZ_NOT_EXISTED, exception.getErrorCode()); // " + quizId,
                                                                                    // exception.getMessage());
                verify(quizRepository).findById(quizId);
                verify(quizRepository, never()).delete(any());
        }
}
