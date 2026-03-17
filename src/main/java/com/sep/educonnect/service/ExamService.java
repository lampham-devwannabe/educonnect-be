package com.sep.educonnect.service;

import com.sep.educonnect.dto.exam.ExamRequest;
import com.sep.educonnect.dto.exam.ExamResponse;
import com.sep.educonnect.dto.exam.QuizRequest;
import com.sep.educonnect.dto.exam.QuizResponse;
import com.sep.educonnect.entity.Exam;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.entity.QuizOption;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamMapper;
import com.sep.educonnect.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final LessonRepository lessonRepository;
    private final TutorClassRepository tutorClassRepository;
    private final UserRepository userRepository;
    private final ExamMapper examMapper;
    private final EntityManager entityManager;

    public Page<ExamResponse> getExamsByLesson(
            Long lessonId, int page, int size, String sortBy, String direction) {
        // Validate lesson exists
        lessonRepository
                .findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Exam> examPage = examRepository.findByLessonId(lessonId, pageable);
        return examPage.map(examMapper::toResponse);
    }

    public ExamResponse getById(Long examId) {
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));
        // Don't modify collection - let mapper filter deleted options
        // The mapper already filters isDeleted in optionListToResponse()
        return examMapper.toResponse(exam);
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        // Validate lesson exists
        lessonRepository
                .findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        // If tutorClassId provided, validate class exists
        if (request.getTutorClassId() != null) {
            // Check class exists
            tutorClassRepository
                    .findById(request.getTutorClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

            // Check ownership: current user must be the tutor of that class
            var context = SecurityContextHolder.getContext();
            String username = context.getAuthentication().getName();
            User tutor =
                    userRepository
                            .findByUsername(username)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            tutorClassRepository
                    .findByIdAndTutorUserId(request.getTutorClassId(), tutor.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_EXAM_OWNER));
        }

        Exam exam = examMapper.toEntity(request);
        Exam saved = examRepository.save(exam);
        return examMapper.toResponse(saved);
    }

    @Transactional
    public ExamResponse updateExam(Long examId, ExamRequest request) {
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Ownership checks:
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        User tutor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // If the existing exam is bound to a class, only that class's tutor can modify it
        if (exam.getTutorClassId() != null) {
            tutorClassRepository
                    .findByIdAndTutorUserId(exam.getTutorClassId(), tutor.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_EXAM_OWNER));
        }

        // If request attempts to bind the exam to a class, check existence and ownership for the
        // target class
        if (request.getTutorClassId() != null) {
            tutorClassRepository
                    .findById(request.getTutorClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
            tutorClassRepository
                    .findByIdAndTutorUserId(request.getTutorClassId(), tutor.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_EXAM_OWNER));
        }

        examMapper.updateEntity(exam, request);

        Exam saved = examRepository.save(exam);
        return examMapper.toResponse(saved);
    }

    @Transactional
    public void deleteExam(Long examId) {
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));
        // If exam is bound to a tutor class, ensure current user is the class tutor
        if (exam.getTutorClassId() != null) {
            var context = SecurityContextHolder.getContext();
            String username = context.getAuthentication().getName();
            User tutor =
                    userRepository
                            .findByUsername(username)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            tutorClassRepository
                    .findByIdAndTutorUserId(exam.getTutorClassId(), tutor.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_EXAM_OWNER));
        }

        examRepository.delete(exam);
    }

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        // Validate exam exists
        examRepository
                .findById(request.getExamId())
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        Quiz quiz = examMapper.toEntity(request);

        Quiz saved = quizRepository.save(quiz);

        // Save options if provided
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            List<QuizOption> options =
                    request.getOptions().stream()
                            .map(
                                    opt -> {
                                        QuizOption option = examMapper.toEntity(opt);
                                        option.setQuizId(saved.getQuizId());
                                        option.setIsDeleted(false);
                                        return option;
                                    })
                            .collect(Collectors.toList());
            quizOptionRepository.saveAll(options);
        }

        // Flush to ensure all changes are persisted
        entityManager.flush();
        // Refresh to reload entity state from database
        entityManager.refresh(saved);

        // Don't modify collection in transaction - let mapper filter deleted options
        // The mapper already filters isDeleted in optionListToResponse()
        return examMapper.toResponse(saved);
    }

    @Transactional
    public QuizResponse updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_EXISTED));

        examMapper.updateEntity(quiz, request);

        Quiz saved = quizRepository.save(quiz);

        // Update options if provided
        if (request.getOptions() != null) {
            // HARD DELETE existing options
            quizOptionRepository.deleteByQuizId(quizId);

            // Create new options
            List<QuizOption> options =
                    request.getOptions().stream()
                            .map(
                                    opt -> {
                                        QuizOption option = examMapper.toEntity(opt);
                                        option.setQuizId(saved.getQuizId());
                                        return option;
                                    })
                            .collect(Collectors.toList());
            quizOptionRepository.saveAll(options);
        }

        // Flush to ensure all changes are persisted
        entityManager.flush();
        // Refresh to reload entity state from database
        entityManager.refresh(saved);

        // Don't modify collection in transaction - let mapper filter deleted options
        // The mapper already filters isDeleted in optionListToResponse()
        return examMapper.toResponse(saved);
    }

    public Page<QuizResponse> getQuizzesByExam(
            Long examId, int page, int size, String sortBy, String direction) {
        // Validate exam exists
        examRepository
                .findById(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Quiz> quizPage = quizRepository.findByExamIdOrderByOrderNoAsc(examId, pageable);
        return quizPage.map(examMapper::toResponse);
    }

    public QuizResponse getQuizById(Long quizId) {
        Quiz quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_EXISTED));
        // Don't modify collection - let mapper filter deleted options
        // The mapper already filters isDeleted in optionListToResponse()
        return examMapper.toResponse(quiz);
    }

    public QuizResponse getQuizByIdAndExamId(Long quizId, Long examId) {
        Quiz quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_EXISTED));
        if (!quiz.getExamId().equals(examId)) {
            throw new AppException(ErrorCode.QUIZ_NOT_EXISTED);
        }
        // Don't modify collection - let mapper filter deleted options
        // The mapper already filters isDeleted in optionListToResponse()
        return examMapper.toResponse(quiz);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_EXISTED));
        quizRepository.delete(quiz);
    }
}
