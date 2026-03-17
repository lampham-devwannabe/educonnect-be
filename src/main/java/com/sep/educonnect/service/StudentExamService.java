package com.sep.educonnect.service;

import com.sep.educonnect.dto.exam.*;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExamMapper;
import com.sep.educonnect.mapper.ExamSubmissionMapper;
import com.sep.educonnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentExamService {

    private final ExamRepository examRepository;
    private final QuizRepository quizRepository;
    private final ExamSubmissionRepository examSubmissionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final LessonRepository lessonRepository;
    private final ExamMapper examMapper;
    private final ExamSubmissionMapper examSubmissionMapper;

    public ExamForStudentResponse getExamForStudent(Long examId, String studentId) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate exam status = PUBLISHED
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new AppException(ErrorCode.EXAM_NOT_PUBLISHED);
        }

        // Validate student đã enroll course chứa lesson của exam
        boolean hasAccess =
                classEnrollmentRepository.existsByStudentIdAndLessonId(
                        studentId, exam.getLessonId());
        if (!hasAccess) {
            throw new AppException(ErrorCode.EXAM_NOT_ACCESSIBLE);
        }

        return examMapper.toExamForStudentResponse(exam);
    }

    @Transactional
    public ExamSubmissionResponse submitExam(
            Long examId, String studentId, ExamSubmissionRequest request) {
        // Validate exam tồn tại
        Exam exam =
                examRepository
                        .findById(examId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_EXISTED));

        // Validate student đã enroll course
        boolean hasAccess =
                classEnrollmentRepository.existsByStudentIdAndLessonId(
                        studentId, exam.getLessonId());
        if (!hasAccess) {
            throw new AppException(ErrorCode.EXAM_NOT_ACCESSIBLE);
        }

        // Lấy tất cả quizzes của exam
        List<Quiz> quizzes = quizRepository.findByExamId(examId);
        if (quizzes.isEmpty()) {
            throw new AppException(ErrorCode.EXAM_NOT_EXISTED);
        }

        // Validate tất cả quizId trong request thuộc về exam
        Set<Long> examQuizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toSet());
        Set<Long> requestQuizIds =
                request.getAnswers().stream()
                        .map(QuizAnswerRequest::getQuizId)
                        .collect(Collectors.toSet());

        if (!examQuizIds.containsAll(requestQuizIds)) {
            throw new AppException(ErrorCode.QUIZ_NOT_EXISTED);
        }

        // Tạo map quizId -> Quiz để dễ lookup
        Map<Long, Quiz> quizMap =
                quizzes.stream().collect(Collectors.toMap(Quiz::getQuizId, q -> q));

        // Tính điểm
        int correctAnswers = 0;
        int totalQuestions = quizzes.size();
        List<ExamAnswer> examAnswers = new ArrayList<>();

        LocalDateTime submittedAt = LocalDateTime.now();
        long durationSeconds = Duration.between(request.getStartedAt(), submittedAt).getSeconds();

        for (QuizAnswerRequest answerRequest : request.getAnswers()) {
            Quiz quiz = quizMap.get(answerRequest.getQuizId());
            if (quiz == null) {
                continue;
            }

            String studentAnswer =
                    answerRequest.getAnswer() != null ? answerRequest.getAnswer().trim() : "";
            String correctAnswer =
                    quiz.getValidAnswer() != null ? quiz.getValidAnswer().trim() : "";
            boolean isCorrect = compareAnswers(studentAnswer, quiz);

            if (isCorrect) {
                correctAnswers++;
            }

            ExamAnswer examAnswer =
                    ExamAnswer.builder()
                            .quizId(quiz.getQuizId())
                            .studentAnswer(studentAnswer)
                            .correctAnswer(correctAnswer)
                            .isCorrect(isCorrect)
                            .build();
            examAnswers.add(examAnswer);
        }

        // Tính điểm
        double score = totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 100 : 0;

        // Tạo ExamSubmission
        ExamSubmission submission =
                ExamSubmission.builder()
                        .examId(examId)
                        .studentId(studentId)
                        .score(score)
                        .totalQuestions(totalQuestions)
                        .correctAnswers(correctAnswers)
                        .startedAt(request.getStartedAt())
                        .submittedAt(submittedAt)
                        .durationSeconds(durationSeconds)
                        .build();

        // Lưu submission
        ExamSubmission savedSubmission = examSubmissionRepository.save(submission);
        Long submissionId = savedSubmission.getSubmissionId();

        // Set submissionId cho answers và lưu
        examAnswers.forEach(answer -> answer.setSubmissionId(submissionId));
        examAnswerRepository.saveAll(examAnswers);

        // Load lại submission với answers để map
        ExamSubmission loadedSubmission =
                examSubmissionRepository
                        .findById(submissionId)
                        .orElseThrow(() -> new AppException(ErrorCode.EXAM_SUBMISSION_NOT_FOUND));

        ExamSubmissionResponse response = examSubmissionMapper.toResponse(loadedSubmission);

        // Thêm explanation vào answers
        for (int i = 0; i < response.getAnswers().size(); i++) {
            ExamAnswerResponse answerResponse = response.getAnswers().get(i);
            Quiz quiz = quizMap.get(answerResponse.getQuizId());
            if (quiz != null) {
                answerResponse.setExplanation(quiz.getExplanation());
            }
        }

        return response;
    }

    public Page<StudentExamListItemResponse> getMyExams(
            String studentId, Long classId, int page, int size) {
        List<ClassEnrollment> enrollments;

        if (classId != null) {
            // only consider enrollment for the specified class
            var optionalEnrollment =
                    classEnrollmentRepository.findByTutorClassIdAndStudentUserId(
                            classId, studentId);
            if (optionalEnrollment.isEmpty()) {
                return new org.springframework.data.domain.PageImpl<>(
                        Collections.emptyList(), PageRequest.of(page, size), 0);
            }
            enrollments = List.of(optionalEnrollment.get());
        } else {
            // existing behaviour: aggregate across all enrollments
            enrollments =
                    classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(studentId);
        }

        if (enrollments.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(
                    Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Lấy tất cả lessonIds từ enrolled courses
        Set<Long> lessonIds = new HashSet<>();
        for (ClassEnrollment enrollment : enrollments) {
            if (enrollment.getTutorClass() != null
                    && enrollment.getTutorClass().getCourse() != null
                    && enrollment.getTutorClass().getCourse().getSyllabus() != null) {

                Long syllabusId =
                        enrollment.getTutorClass().getCourse().getSyllabus().getSyllabusId();
                // Lấy lessons từ syllabus
                List<Lesson> lessons = lessonRepository.findBySyllabusId(syllabusId);
                lessonIds.addAll(
                        lessons.stream().map(Lesson::getLessonId).collect(Collectors.toList()));
            }
        }

        if (lessonIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Lấy tất cả exams từ lessons (chỉ PUBLISHED)
        // If caller provided classId, only include exams global or specific to that class
        List<Exam> exams =
                examRepository.findByLessonIdInAndStatusAndClass(
                        new ArrayList<>(lessonIds), "PUBLISHED", classId);

        // Tạo map lessonId -> Lesson title
        Map<Long, String> lessonTitleMap = new HashMap<>();
        for (Long lessonId : lessonIds) {
            lessonRepository
                    .findById(lessonId)
                    .ifPresent(lesson -> lessonTitleMap.put(lessonId, lesson.getTitle()));
        }

        // Tạo map examId -> best score và submission count
        Map<Long, Double> bestScoreMap = new HashMap<>();
        Map<Long, Long> submissionCountMap = new HashMap<>();
        for (Exam exam : exams) {
            Optional<Double> bestScore =
                    examSubmissionRepository.findBestScoreByExamIdAndStudentId(
                            exam.getExamId(), studentId);
            bestScore.ifPresent(score -> bestScoreMap.put(exam.getExamId(), score));

            Long count =
                    examSubmissionRepository.countByExamIdAndStudentId(exam.getExamId(), studentId);
            submissionCountMap.put(exam.getExamId(), count);
        }

        // Map to response
        List<StudentExamListItemResponse> responses =
                exams.stream()
                        .map(
                                exam -> {
                                    String lessonTitle =
                                            lessonTitleMap.getOrDefault(exam.getLessonId(), "");
                                    String examTitle =
                                            exam.getField() != null ? exam.getField() : lessonTitle;

                                    return StudentExamListItemResponse.builder()
                                            .examId(exam.getExamId())
                                            .lessonId(exam.getLessonId())
                                            .lessonTitle(lessonTitle)
                                            .examTitle(examTitle)
                                            .status(exam.getStatus())
                                            .submitted(
                                                    submissionCountMap.getOrDefault(
                                                                    exam.getExamId(), 0L)
                                                            > 0)
                                            .bestScore(bestScoreMap.get(exam.getExamId()))
                                            .submissionCount(
                                                    submissionCountMap
                                                            .getOrDefault(exam.getExamId(), 0L)
                                                            .intValue())
                                            .build();
                                })
                        .collect(Collectors.toList());

        // Phân trang
        int start = (int) PageRequest.of(page, size).getOffset();
        int end = Math.min((start + size), responses.size());

        // Handle case where page number exceeds total pages
        List<StudentExamListItemResponse> pagedResponses;
        if (start >= responses.size()) {
            pagedResponses = Collections.emptyList();
        } else {
            pagedResponses = responses.subList(start, end);
        }

        return new org.springframework.data.domain.PageImpl<>(
                pagedResponses, PageRequest.of(page, size), responses.size());
    }

    private boolean compareAnswers(String studentAnswerRaw, Quiz quiz) {
        if (quiz == null || studentAnswerRaw == null) {
            return false;
        }

        List<String> normalizedStudentAnswers =
                normalizeAnswerTokens(studentAnswerRaw, quiz.getOptions());
        List<String> normalizedCorrectAnswers = extractCorrectAnswers(quiz);

        if (normalizedStudentAnswers.isEmpty() || normalizedCorrectAnswers.isEmpty()) {
            return false;
        }

        String quizType = quiz.getType() != null ? quiz.getType().toUpperCase() : "SINGLE_CHOICE";

        if ("MULTIPLE_CHOICE".equals(quizType)) {
            return new LinkedHashSet<>(normalizedStudentAnswers)
                    .equals(new LinkedHashSet<>(normalizedCorrectAnswers));
        }

        return normalizedStudentAnswers.size() == 1
                && normalizedCorrectAnswers.size() == 1
                && normalizedStudentAnswers.getFirst().equals(normalizedCorrectAnswers.getFirst());
    }

    private List<String> normalizeAnswerTokens(String rawAnswer, List<QuizOption> options) {
        if (rawAnswer == null) {
            return List.of();
        }
        Map<String, String> letterToOptionTextMap = buildLetterToOptionTextMap(options);
        return Arrays.stream(rawAnswer.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> resolveAnswerToken(token, letterToOptionTextMap, options))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> extractCorrectAnswers(Quiz quiz) {
        List<QuizOption> options = quiz.getOptions();
        List<String> correctFromOptions =
                Optional.ofNullable(options).orElse(List.of()).stream()
                        .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                        .map(option -> normalizeText(option.getText()))
                        .filter(answer -> !answer.isEmpty())
                        .toList();

        if (!correctFromOptions.isEmpty()) {
            return correctFromOptions;
        }

        String validAnswer = quiz.getValidAnswer();
        if (validAnswer == null || validAnswer.isBlank()) {
            return List.of();
        }

        Map<String, String> letterToOptionTextMap = buildLetterToOptionTextMap(options);
        return Arrays.stream(validAnswer.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> resolveAnswerToken(token, letterToOptionTextMap, options))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, String> buildLetterToOptionTextMap(List<QuizOption> options) {
        Map<String, String> letterMap = new HashMap<>();

        if (options == null || options.isEmpty()) {
            return letterMap;
        }

        for (int i = 0; i < options.size(); i++) {
            String letter = String.valueOf((char) ('A' + i));
            letterMap.put(letter, normalizeText(options.get(i).getText()));
        }

        return letterMap;
    }

    private String resolveAnswerToken(
            String token, Map<String, String> letterToOptionTextMap, List<QuizOption> options) {
        String normalizedToken = normalizeText(token);
        if (normalizedToken.isEmpty()) {
            return null;
        }

        if (letterToOptionTextMap.containsKey(normalizedToken)) {
            return letterToOptionTextMap.get(normalizedToken);
        }

        if (options != null) {
            for (QuizOption option : options) {
                String optionText = normalizeText(option.getText());
                if (normalizedToken.equals(optionText)) {
                    return optionText;
                }
            }
        }

        return normalizedToken;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
