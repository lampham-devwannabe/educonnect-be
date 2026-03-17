package com.sep.educonnect.service.admin;

import com.sep.educonnect.dto.admin.statistic.MonthlyStatDTO;
import com.sep.educonnect.dto.admin.statistic.SubjectStatDTO;
import com.sep.educonnect.dto.admin.statistic.TeachingStatisticsDTO;
import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassSessionRepository;
import com.sep.educonnect.repository.TutorClassRepository;
import com.sep.educonnect.repository.TutorProfileRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TeachingStatisticsService {
    final TutorClassRepository tutorClassRepository;
    final ClassSessionRepository sessionRepository;
    final TutorProfileRepository tutorProfileRepository;

    public TeachingStatisticsDTO getTeachingStatistics(String tutorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Lấy thông tin giảng viên
        TutorProfile profile =
                tutorProfileRepository
                        .findByUserUserIdAndSubmissionStatus(tutorId, ProfileStatus.APPROVED)
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        // Thống kê lớp học
        List<TutorClass> allClasses = tutorClassRepository.findByTutorUserId(tutorId);
        List<TutorClass> activeClasses =
                tutorClassRepository.findActiveClassesByTutor(tutorId, today);
        List<TutorClass> completedClasses =
                tutorClassRepository.findCompletedClassesByTutor(tutorId, today);

        // Thống kê buổi học
        List<ClassSession> completedSessions =
                sessionRepository.findCompletedSessionsByTutor(tutorId, now);
        List<ClassSession> upcomingSessions =
                sessionRepository.findUpcomingSessionsByTutor(tutorId, now);
        Integer cancelledSessions = sessionRepository.countCancelledSessionsByTutor(tutorId);

        // Tính tổng giờ dạy (từ các buổi đã hoàn thành)
        Double totalHours = calculateTotalHours(completedSessions);

        // Tổng số học sinh
        Integer totalStudents = allClasses.stream().mapToInt(TutorClass::getCurrentStudents).sum();

        // Thống kê theo tháng
        List<MonthlyStatDTO> monthlyStats = calculateMonthlyStats(completedSessions);

        // Thống kê theo môn học
        List<SubjectStatDTO> subjectStats = calculateSubjectStats(allClasses, completedSessions);

        // Thời gian dạy
        List<ClassSession> allSessions =
                sessionRepository.findAllSessionsByTutorOrderByDate(tutorId);
        LocalDateTime firstSession =
                allSessions.isEmpty() ? null : allSessions.get(0).getStartTime();
        LocalDateTime lastSession =
                completedSessions.isEmpty()
                        ? null
                        : completedSessions.stream()
                                .max(Comparator.comparing(ClassSession::getEndTime))
                                .map(ClassSession::getEndTime)
                                .orElse(null);
        LocalDateTime nextSession =
                upcomingSessions.isEmpty()
                        ? null
                        : upcomingSessions.stream()
                                .min(Comparator.comparing(ClassSession::getStartTime))
                                .map(ClassSession::getStartTime)
                                .orElse(null);

        return TeachingStatisticsDTO.builder()
                .tutorId(tutorId)
                .tutorName(profile.getUser().getFirstName() + " " + profile.getUser().getLastName())
                .totalClasses(allClasses.size())
                .activeClasses(activeClasses.size())
                .completedClasses(completedClasses.size())
                .totalStudents(totalStudents)
                .averageRating(profile.getRating())
                .totalSessionsCompleted(completedSessions.size())
                .totalSessionsUpcoming(upcomingSessions.size())
                .totalSessionsCancelled(cancelledSessions)
                .totalTeachingHours(totalHours)
                .monthlyStats(monthlyStats)
                .subjectStats(subjectStats)
                .firstSessionDate(firstSession)
                .lastSessionDate(lastSession)
                .nextSessionDate(nextSession)
                .build();
    }

    private Double calculateTotalHours(List<ClassSession> sessions) {
        return sessions.stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .mapToDouble(
                        s -> {
                            long minutes =
                                    ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime());
                            return minutes / 60.0;
                        })
                .sum();
    }

    private List<MonthlyStatDTO> calculateMonthlyStats(List<ClassSession> completedSessions) {
        Map<YearMonth, List<ClassSession>> sessionsByMonth =
                completedSessions.stream()
                        .filter(s -> s.getStartTime() != null)
                        .collect(Collectors.groupingBy(s -> YearMonth.from(s.getStartTime())));

        return sessionsByMonth.entrySet().stream()
                .map(
                        entry -> {
                            YearMonth yearMonth = entry.getKey();
                            List<ClassSession> sessions = entry.getValue();

                            Double hours =
                                    sessions.stream()
                                            .filter(
                                                    s ->
                                                            s.getStartTime() != null
                                                                    && s.getEndTime() != null)
                                            .mapToDouble(
                                                    s -> {
                                                        long minutes =
                                                                ChronoUnit.MINUTES.between(
                                                                        s.getStartTime(),
                                                                        s.getEndTime());
                                                        return minutes / 60.0;
                                                    })
                                            .sum();

                            Set<String> uniqueStudents =
                                    sessions.stream()
                                            .map(s -> s.getTutorClass().getEnrollments())
                                            .flatMap(List::stream)
                                            .map(e -> e.getStudent().getUserId())
                                            .collect(Collectors.toSet());

                            return MonthlyStatDTO.builder()
                                    .year(yearMonth.getYear())
                                    .month(yearMonth.getMonthValue())
                                    .sessionsCompleted(sessions.size())
                                    .hoursCompleted(hours)
                                    .studentsEnrolled(uniqueStudents.size())
                                    .build();
                        })
                .sorted(
                        Comparator.comparing(MonthlyStatDTO::getYear)
                                .thenComparing(MonthlyStatDTO::getMonth)
                                .reversed())
                .toList();
    }

    private List<SubjectStatDTO> calculateSubjectStats(
            List<TutorClass> classes, List<ClassSession> completedSessions) {

        // Sử dụng Map để lưu thông tin thay vì Builder
        Map<Long, SubjectStatData> subjectMap = new HashMap<>();

        // Đếm số lớp theo subject
        for (TutorClass tutorClass : classes) {
            Course course = tutorClass.getCourse();
            if (course != null && course.getSyllabus() != null) {
                Long subjectId = course.getSyllabus().getSubjectId();

                SubjectStatData data =
                        subjectMap.computeIfAbsent(subjectId, id -> new SubjectStatData(id));
                data.classCount++;
            }
        }

        // Thêm sessions và tính giờ dạy
        for (ClassSession session : completedSessions) {
            Course course = session.getTutorClass().getCourse();
            if (course != null && course.getSyllabus() != null) {
                Long subjectId = course.getSyllabus().getSubjectId();

                SubjectStatData data = subjectMap.get(subjectId);
                if (data != null) {
                    data.sessionCount++;

                    if (session.getStartTime() != null && session.getEndTime() != null) {
                        long minutes =
                                ChronoUnit.MINUTES.between(
                                        session.getStartTime(), session.getEndTime());
                        data.totalHours += minutes / 60.0;
                    }
                }
            }
        }

        // Lấy tên subject và build DTO
        return subjectMap.entrySet().stream()
                .map(
                        entry -> {
                            SubjectStatData data = entry.getValue();
                            // Lấy tên subject từ database hoặc từ một trong các course
                            String subjectName =
                                    getSubjectName(classes, completedSessions, entry.getKey());

                            return SubjectStatDTO.builder()
                                    .subjectId(data.subjectId)
                                    .subjectName(subjectName)
                                    .classCount(data.classCount)
                                    .sessionCount(data.sessionCount)
                                    .totalHours(data.totalHours)
                                    .build();
                        })
                .sorted(Comparator.comparing(SubjectStatDTO::getTotalHours).reversed())
                .toList();
    }

    // Lấy tên subject từ syllabus (có thể cần query thêm từ database)
    private String getSubjectName(
            List<TutorClass> classes, List<ClassSession> sessions, Long subjectId) {
        // Tìm trong classes
        for (TutorClass tc : classes) {
            if (tc.getCourse() != null
                    && tc.getCourse().getSyllabus() != null
                    && tc.getCourse().getSyllabus().getSubjectId().equals(subjectId)) {
                return tc.getCourse().getSyllabus().getName();
            }
        }

        // Tìm trong sessions
        for (ClassSession session : sessions) {
            if (session.getTutorClass().getCourse() != null
                    && session.getTutorClass().getCourse().getSyllabus() != null
                    && session.getTutorClass()
                            .getCourse()
                            .getSyllabus()
                            .getSubjectId()
                            .equals(subjectId)) {
                return session.getTutorClass().getCourse().getSyllabus().getName();
            }
        }

        return "Unknown Subject";
    }

    private static class SubjectStatData {
        Long subjectId;
        int classCount = 0;
        int sessionCount = 0;
        double totalHours = 0.0;

        SubjectStatData(Long subjectId) {
            this.subjectId = subjectId;
        }
    }
}
