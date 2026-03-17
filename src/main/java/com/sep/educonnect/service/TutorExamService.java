package com.sep.educonnect.service;

import com.sep.educonnect.dto.exam.*;
import com.sep.educonnect.entity.Exam;
import com.sep.educonnect.entity.ExamAnswer;
import com.sep.educonnect.entity.ExamSubmission;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamSubmissionMapper;
import com.sep.educonnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorExamService {

    private final ExamRepository examRepository;
    private final ExamSubmissionRepository examSubmissionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final ExamSubmissionMapper examSubmissionMapper;

    public ExamResultsResponse getExamResults(Long examId, String tutorId) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate tutor là owner của course chứa exam
        // Lấy lesson từ exam
        com.sep.educonnect.entity.Lesson lesson =
                lessonRepository
                        .findById(exam.getLessonId())
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        // Kiểm tra tutor có phải owner của course không
        // Course -> Syllabus -> Module -> Lesson
        // Cần check Course.tutor.userId == tutorId
        boolean isOwner = checkTutorOwnership(lesson, tutorId);
        if (!isOwner) {
            throw new AppException(ErrorCode.INVALID_EXAM_OWNER);
        }

        // Lấy tất cả submissions của exam
        List<ExamSubmission> submissions = examSubmissionRepository.findByExamId(examId);

        // Lấy tất cả quizzes để lấy explanation
        List<Quiz> quizzes = quizRepository.findByExamId(examId);
        Map<Long, Quiz> quizMap =
                quizzes.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));

        // Map submissions to results
        List<ExamResultResponse> results =
                submissions.stream()
                        .map(
                                submission -> {
                                    ExamResultResponse result =
                                            examSubmissionMapper.toResultResponse(submission);

                                    // Load answers và thêm explanation
                                    List<ExamAnswer> answers =
                                            examAnswerRepository.findBySubmissionId(
                                                    submission.getSubmissionId());
                                    List<ExamAnswerResponse> answerResponses =
                                            answers.stream()
                                                    .map(
                                                            answer -> {
                                                                ExamAnswerResponse answerResponse =
                                                                        examSubmissionMapper
                                                                                .toAnswerResponse(
                                                                                        answer);
                                                                Quiz quiz =
                                                                        quizMap.get(
                                                                                answer.getQuizId());
                                                                if (quiz != null) {
                                                                    answerResponse.setExplanation(
                                                                            quiz.getExplanation());
                                                                }
                                                                return answerResponse;
                                                            })
                                                    .collect(Collectors.toList());
                                    result.setAnswers(answerResponses);

                                    return result;
                                })
                        .collect(Collectors.toList());

        // Lấy lesson title
        String lessonTitle = lesson.getTitle();
        String examTitle = exam.getField() != null ? exam.getField() : lessonTitle;

        return ExamResultsResponse.builder()
                .examId(examId)
                .examTitle(examTitle)
                .lessonTitle(lessonTitle)
                .totalSubmissions(submissions.size())
                .results(results)
                .build();
    }

    public ExamStatisticsResponse getExamStatistics(Long examId, String tutorId) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate tutor là owner của course chứa exam
        com.sep.educonnect.entity.Lesson lesson =
                lessonRepository
                        .findById(exam.getLessonId())
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        boolean isOwner = checkTutorOwnership(lesson, tutorId);
        if (!isOwner) {
            throw new AppException(ErrorCode.INVALID_EXAM_OWNER);
        }

        // Lấy tất cả submissions với student
        List<ExamSubmission> submissions = examSubmissionRepository.findByExamIdWithStudent(examId);

        // Lấy danh sách studentId duy nhất từ submissions (tránh thêm query)
        List<String> distinctStudentIds =
                submissions.stream()
                        .map(ExamSubmission::getStudentId)
                        .distinct()
                        .collect(Collectors.toList());

        // Tính toán thống kê cho từng student
        List<StudentStatisticsResponse> studentStatistics =
                distinctStudentIds.stream()
                        .<StudentStatisticsResponse>map(
                                studentId -> {
                                    // Lấy submissions của student này
                                    List<ExamSubmission> studentSubmissions =
                                            submissions.stream()
                                                    .filter(s -> s.getStudentId().equals(studentId))
                                                    .collect(Collectors.toList());

                                    if (studentSubmissions.isEmpty()) {
                                        return null;
                                    }

                                    // Lấy thông tin student từ submission đầu tiên
                                    ExamSubmission firstSubmission = studentSubmissions.get(0);
                                    String studentName =
                                            examSubmissionMapper.getStudentName(firstSubmission);

                                    // Tính toán các chỉ số từ danh sách submissions đã load (tránh
                                    // N+1 query)
                                    int attemptCount = studentSubmissions.size();

                                    // Best score: tìm điểm cao nhất
                                    Double bestScore =
                                            studentSubmissions.stream()
                                                    .map(ExamSubmission::getScore)
                                                    .filter(Objects::nonNull)
                                                    .max(Double::compareTo)
                                                    .orElse(null);

                                    // Average score: tính trung bình
                                    Double averageScore = null;
                                    OptionalDouble avgOpt =
                                            studentSubmissions.stream()
                                                    .map(ExamSubmission::getScore)
                                                    .filter(Objects::nonNull)
                                                    .mapToDouble(Double::doubleValue)
                                                    .average();
                                    if (avgOpt.isPresent()) {
                                        averageScore = avgOpt.getAsDouble();
                                    }

                                    // First attempt: tìm thời gian submit sớm nhất
                                    LocalDateTime firstAttempt =
                                            studentSubmissions.stream()
                                                    .map(ExamSubmission::getSubmittedAt)
                                                    .filter(Objects::nonNull)
                                                    .min(LocalDateTime::compareTo)
                                                    .orElse(null);

                                    // Last attempt: tìm thời gian submit muộn nhất
                                    LocalDateTime lastAttempt =
                                            studentSubmissions.stream()
                                                    .map(ExamSubmission::getSubmittedAt)
                                                    .filter(Objects::nonNull)
                                                    .max(LocalDateTime::compareTo)
                                                    .orElse(null);

                                    return StudentStatisticsResponse.builder()
                                            .studentId(studentId)
                                            .studentName(studentName)
                                            .attemptCount(attemptCount)
                                            .bestScore(bestScore)
                                            .averageScore(averageScore)
                                            .firstAttemptAt(firstAttempt)
                                            .lastAttemptAt(lastAttempt)
                                            .build();
                                })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        // Lấy lesson title
        String lessonTitle = lesson.getTitle();
        String examTitle = exam.getField() != null ? exam.getField() : lessonTitle;

        return ExamStatisticsResponse.builder()
                .examId(examId)
                .examTitle(examTitle)
                .lessonTitle(lessonTitle)
                .lessonId(lesson.getLessonId())
                .totalSubmissions(submissions.size())
                .totalStudents(distinctStudentIds.size())
                .studentStatistics(studentStatistics)
                .build();
    }

    public ExamAttemptsResponse getExamAttempts(Long examId, String tutorId, String studentId) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate tutor là owner của course chứa exam
        com.sep.educonnect.entity.Lesson lesson =
                lessonRepository
                        .findById(exam.getLessonId())
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        boolean isOwner = checkTutorOwnership(lesson, tutorId);
        if (!isOwner) {
            throw new AppException(ErrorCode.INVALID_EXAM_OWNER);
        }

        // Lấy submissions (có filter theo studentId nếu có) 
        List<ExamSubmission> submissions;
        if (studentId != null && !studentId.trim().isEmpty()) {
            submissions =
                    examSubmissionRepository
                            .findByExamIdAndStudentIdWithStudentOrderBySubmittedAtDesc(
                                    examId, studentId);
        } else {
            submissions = new ArrayList<>(examSubmissionRepository.findByExamIdWithStudent(examId));
            // Sort by submittedAt desc
            submissions.sort((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()));
        }

        // Map to summary responses (không có answers)
        List<ExamAttemptSummaryResponse> attempts =
                submissions.stream()
                        .map(
                                submission -> {
                                    String studentName =
                                            examSubmissionMapper.getStudentName(submission);
                                    return ExamAttemptSummaryResponse.builder()
                                            .submissionId(submission.getSubmissionId())
                                            .studentId(submission.getStudentId())
                                            .studentName(studentName)
                                            .score(submission.getScore())
                                            .totalQuestions(submission.getTotalQuestions())
                                            .correctAnswers(submission.getCorrectAnswers())
                                            .submittedAt(submission.getSubmittedAt())
                                            .durationSeconds(submission.getDurationSeconds())
                                            .build();
                                })
                        .collect(Collectors.toList());

        // Lấy lesson title
        String lessonTitle = lesson.getTitle();
        String examTitle = exam.getField() != null ? exam.getField() : lessonTitle;

        return ExamAttemptsResponse.builder()
                .examId(examId)
                .examTitle(examTitle)
                .lessonTitle(lessonTitle)
                .totalAttempts(attempts.size())
                .attempts(attempts)
                .build();
    }

    public ExamResultResponse getExamAttemptDetail(Long examId, Long submissionId, String tutorId) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate tutor là owner của course chứa exam
        com.sep.educonnect.entity.Lesson lesson =
                lessonRepository
                        .findById(exam.getLessonId())
                        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        boolean isOwner = checkTutorOwnership(lesson, tutorId);
        if (!isOwner) {
            throw new AppException(ErrorCode.INVALID_EXAM_OWNER);
        }

        // Lấy submission với student và answers
        ExamSubmission submission =
                examSubmissionRepository
                        .findBySubmissionIdWithStudent(submissionId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_SUBMISSION_NOT_FOUND));

        // Validate submission thuộc exam này
        if (!submission.getExamId().equals(examId)) {
            throw new AppException(ErrorCode.EXAM_SUBMISSION_NOT_FOUND);
        }

        // Map to result response
        ExamResultResponse result = examSubmissionMapper.toResultResponse(submission);

        // Load answers và thêm explanation
        List<ExamAnswer> answers = examAnswerRepository.findBySubmissionId(submissionId);
        List<Quiz> quizzes = quizRepository.findByExamId(examId);
        Map<Long, Quiz> quizMap =
                quizzes.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));

        List<ExamAnswerResponse> answerResponses =
                answers.stream()
                        .map(
                                answer -> {
                                    ExamAnswerResponse answerResponse =
                                            examSubmissionMapper.toAnswerResponse(answer);
                                    Quiz quiz = quizMap.get(answer.getQuizId());
                                    if (quiz != null) {
                                        answerResponse.setExplanation(quiz.getExplanation());
                                    }
                                    return answerResponse;
                                })
                        .collect(Collectors.toList());
        result.setAnswers(answerResponses);

        return result;
    }

    private boolean checkTutorOwnership(com.sep.educonnect.entity.Lesson lesson, String tutorId) {
        return courseRepository.existsByTutorIdAndLessonId(tutorId, lesson.getLessonId());
    }
}
