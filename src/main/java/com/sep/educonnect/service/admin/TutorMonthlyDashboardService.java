package com.sep.educonnect.service.admin;

import com.sep.educonnect.dto.admin.statistic.*;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.utils.RevenueCalculationUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorMonthlyDashboardService {

    ClassSessionRepository classSessionRepository;
    TutorClassRepository tutorClassRepository;
    CourseRepository courseRepository;
    TutorProfileRepository tutorProfileRepository;

    public TutorMonthlyDashboardDTO getMonthlyDashboard(int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = calculateEndOfMonth(targetMonth);

        // Get sessions for current and previous month
        List<ClassSession> currentMonthSessions =
                classSessionRepository.findSessionsInDateRange(startOfMonth, endOfMonth);

        YearMonth previousMonth = targetMonth.minusMonths(1);
        List<ClassSession> previousMonthSessions =
                classSessionRepository.findSessionsInDateRange(
                        previousMonth.atDay(1).atStartOfDay(),
                        previousMonth.atEndOfMonth().atTime(23, 59, 59));

        // Calculate overall statistics
        Integer totalActiveTutors =
                (int) tutorProfileRepository.countTutorsCreatedInMonth(year, month);
        Double totalTeachingHours = calculateTotalHours(currentMonthSessions);
        Integer totalSessionsCompleted = currentMonthSessions.size();

        // Calculate growth percentages
        Double hoursGrowthPercentage =
                calculateGrowthPercentage(
                        totalTeachingHours, calculateTotalHours(previousMonthSessions));
        Double sessionsGrowthPercentage =
                calculateGrowthPercentage(
                        totalSessionsCompleted.doubleValue(), previousMonthSessions.size());
        int prevActiveTutors =
                (int)
                        tutorProfileRepository.countTutorsCreatedInMonth(
                                previousMonth.getYear(), previousMonth.getMonthValue());
        Double tutorsGrowthPercentage =
                calculateGrowthPercentage(totalActiveTutors.doubleValue(), prevActiveTutors);

        // Build tutor statistics from sessions (single pass)
        Map<String, TutorStats> tutorStatsMap = buildTutorStatsMap(currentMonthSessions);

        // Get top 3 performers for each category
        List<TopTutorDTO> topTutorsByHours =
                getTopTutorsByMetric(
                        tutorStatsMap,
                        3,
                        Comparator.comparingDouble(TutorStats::getHours).reversed());
        List<TopTutorDTO> topTutorsBySessions =
                getTopTutorsByMetric(
                        tutorStatsMap,
                        3,
                        Comparator.comparingInt(TutorStats::getSessionsCount).reversed());
        List<TopTutorDTO> topTutorsByStudents =
                getTopTutorsByMetric(
                        tutorStatsMap,
                        3,
                        Comparator.comparingInt(TutorStats::getStudentsCount).reversed());

        return TutorMonthlyDashboardDTO.builder()
                .year(year)
                .month(month)
                .totalActiveTutors(totalActiveTutors)
                .totalTeachingHours(totalTeachingHours)
                .totalSessionsCompleted(totalSessionsCompleted)
                .topTutorsByHours(topTutorsByHours)
                .topTutorsBySessions(topTutorsBySessions)
                .topTutorsByStudents(topTutorsByStudents)
                .hoursGrowthPercentage(hoursGrowthPercentage)
                .sessionsGrowthPercentage(sessionsGrowthPercentage)
                .tutorsGrowthPercentage(tutorsGrowthPercentage)
                .build();
    }

    @Cacheable(value = "tutorMonthlySummary", key = "#year + '_' + #month + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortDirection")
    public Page<TutorMonthlySummaryDTO> getMonthlySummaries(
            int year, int month, int page, int size, String sortBy, String sortDirection) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        Sort.Direction direction =
                "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size);

        Page<TutorSessionStatsProjection> statsPage =
                tutorClassRepository.findTutorSessionStats(
                        String.valueOf(ProfileStatus.APPROVED),
                        startOfMonth,
                        endOfMonth,
                        sortBy,
                        direction,
                        pageable);

        if (statsPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Extract tutor IDs for revenue calculation
        List<String> tutorIds =
                statsPage.getContent().stream()
                        .map(TutorSessionStatsProjection::getTutorId)
                        .collect(Collectors.toList());

        // Calculate revenues separately (your existing complex logic)
        Map<String, BigDecimal> revenueByTutor = calculateBatchTutorRevenues(tutorIds, targetMonth);

        // Combine stats with revenue
        List<TutorMonthlySummaryDTO> summaries =
                statsPage.getContent().stream()
                        .map(
                                stats ->
                                        TutorMonthlySummaryDTO.builder()
                                                .id(stats.getId())
                                                .tutorId(stats.getTutorId())
                                                .tutorName(stats.getTutorName())
                                                .teachingHours(stats.getTeachingHours())
                                                .sessionsCompleted(stats.getSessionsCompleted())
                                                .monthlyRevenue(
                                                        revenueByTutor.getOrDefault(
                                                                stats.getTutorId(),
                                                                BigDecimal.ZERO))
                                                .build())
                        .collect(Collectors.toList());

        return new PageImpl<>(summaries, pageable, statsPage.getTotalElements());
    }

    public TutorMonthlyDetailDTO getMonthlyDetail(Long id, int year, int month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = calculateEndOfMonth(targetMonth);

        // 1. Get tutor profile with basic info
        TutorProfile profile =
                tutorProfileRepository
                        .findByIdAndStatus(id, ProfileStatus.APPROVED)
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        String tutorId = profile.getUser().getUserId();

        // 2. Get current month sessions
        List<ClassSession> currentMonthSessions =
                classSessionRepository.findCompletedSessionsByTutorInDateRange(
                        tutorId, startOfMonth, endOfMonth);

        // 3. Get previous month sessions for growth calculations
        YearMonth previousMonth = targetMonth.minusMonths(1);
        List<ClassSession> previousMonthSessions =
                classSessionRepository.findCompletedSessionsByTutorInDateRange(
                        tutorId,
                        previousMonth.atDay(1).atStartOfDay(),
                        previousMonth.atEndOfMonth().atTime(23, 59, 59));

        // 4. Calculate basic metrics
        double teachingHours = calculateTotalHours(currentMonthSessions);
        Integer sessionsCompleted = currentMonthSessions.size();

        // Calculate revenues separately for online and self-paced courses
        BigDecimal onlineRevenue =
                calculateOnlineRevenue(currentMonthSessions, profile.getExperience());
        BigDecimal selfPacedRevenue =
                calculateSelfPacedRevenue(currentMonthSessions, profile.getExperience());
        BigDecimal monthlyRevenue = onlineRevenue.add(selfPacedRevenue);

        BigDecimal averageHourlyRate =
                teachingHours > 0
                        ? monthlyRevenue.divide(
                                BigDecimal.valueOf(teachingHours), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        // 5. Calculate student counts by course type
        StudentCountsByType studentCounts = calculateStudentCountsByType(currentMonthSessions);

        // 6. Build subject breakdown
        List<SubjectMonthlyStatsDTO> subjectBreakdown = buildSubjectBreakdown(currentMonthSessions);

        // 7. Build class statistics
        List<ClassMonthlyStatsDTO> classStats = buildClassStats(currentMonthSessions);

        // 8. Calculate growth indicators
        Double hoursChangeFromLastMonth =
                calculateGrowthPercentage(
                        teachingHours, calculateTotalHours(previousMonthSessions));
        BigDecimal prevOnlineRevenue =
                calculateOnlineRevenue(previousMonthSessions, profile.getExperience());
        BigDecimal prevSelfPacedRevenue =
                calculateSelfPacedRevenue(previousMonthSessions, profile.getExperience());
        BigDecimal prevRevenue = prevOnlineRevenue.add(prevSelfPacedRevenue);
        Double revenueChangeFromLastMonth =
                calculateGrowthPercentage(monthlyRevenue.doubleValue(), prevRevenue.doubleValue());

        // 9. Map sessions to DTOs
        List<SessionDetailDTO> sessions =
                currentMonthSessions.stream()
                        .map(this::mapToSessionDetailDTO)
                        .sorted(Comparator.comparing(SessionDetailDTO::getStartTime).reversed())
                        .collect(Collectors.toList());

        return TutorMonthlyDetailDTO.builder()
                .id(id)
                .tutorId(tutorId)
                .tutorName(profile.getUser().getFirstName() + " " + profile.getUser().getLastName())
                .tutorEmail(profile.getUser().getEmail())
                .teachingHours(teachingHours)
                .sessionsCompleted(sessionsCompleted)
                .onlineCourseStudents(studentCounts.onlineStudents())
                .selfPacedCourseStudents(studentCounts.selfPacedStudents())
                .monthlyRevenue(monthlyRevenue)
                .onlineRevenue(onlineRevenue)
                .selfPacedRevenue(selfPacedRevenue)
                .averageHourlyRate(averageHourlyRate)
                .subjectBreakdown(subjectBreakdown)
                .classStats(classStats)
                .hoursChangeFromLastMonth(hoursChangeFromLastMonth)
                .revenueChangeFromLastMonth(revenueChangeFromLastMonth)
                .sessions(sessions)
                .build();
    }

    private LocalDateTime calculateEndOfMonth(YearMonth targetMonth) {
        YearMonth currentMonth = YearMonth.now();

        if (targetMonth.equals(currentMonth)) {
            // If target month is current month, use current datetime
            return LocalDateTime.now();
        } else {
            // If target month is past month, use end of month
            return targetMonth.atEndOfMonth().atTime(23, 59, 59);
        }
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

    private Double calculateGrowthPercentage(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }

    /** Build statistics for all tutors from sessions in a single pass */
    private Map<String, TutorStats> buildTutorStatsMap(List<ClassSession> sessions) {
        Map<String, TutorStats> statsMap = new HashMap<>();

        for (ClassSession session : sessions) {
            if (session.getTutorClass() == null || session.getTutorClass().getTutor() == null) {
                continue;
            }

            String tutorId = session.getTutorClass().getTutor().getUserId();
            TutorStats stats =
                    statsMap.computeIfAbsent(
                            tutorId,
                            id -> {
                                User tutor = session.getTutorClass().getTutor();
                                return new TutorStats(
                                        tutorId, tutor.getFirstName() + " " + tutor.getLastName());
                            });

            // Count session
            stats.incrementSessions();

            // Add hours
            if (session.getStartTime() != null && session.getEndTime() != null) {
                long minutes =
                        ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
                stats.addHours(minutes / 60.0);
            }

            // Count unique students
            session.getTutorClass()
                    .getEnrollments()
                    .forEach(enrollment -> stats.addStudent(enrollment.getStudent().getUserId()));
        }

        return statsMap;
    }

    /** Get top N tutors by a specific metric */
    private List<TopTutorDTO> getTopTutorsByMetric(
            Map<String, TutorStats> tutorStatsMap, int limit, Comparator<TutorStats> comparator) {

        return tutorStatsMap.values().stream()
                .sorted(comparator)
                .limit(limit)
                .map(
                        stats ->
                                TopTutorDTO.builder()
                                        .tutorId(stats.getTutorId())
                                        .tutorName(stats.getTutorName())
                                        .teachingHours(stats.getHours())
                                        .sessionsCompleted(stats.getSessionsCount())
                                        .studentsTaught(stats.getStudentsCount())
                                        .build())
                .collect(Collectors.toList());
    }

    /** Calculate unique student counts separated by course type */
    private StudentCountsByType calculateStudentCountsByType(List<ClassSession> sessions) {
        Set<String> onlineStudents = new HashSet<>();
        Set<String> selfPacedStudents = new HashSet<>();

        for (ClassSession session : sessions) {
            if (session.getTutorClass() == null || session.getTutorClass().getCourse() == null) {
                continue;
            }

            Course course = session.getTutorClass().getCourse();
            Set<String> students =
                    session.getTutorClass().getEnrollments().stream()
                            .map(enrollment -> enrollment.getStudent().getUserId())
                            .collect(Collectors.toSet());

            if (course.getType() == CourseType.ONLINE) {
                onlineStudents.addAll(students);
            } else if (course.getType() == CourseType.SELF_PACED) {
                selfPacedStudents.addAll(students);
            }
        }

        return new StudentCountsByType(onlineStudents.size(), selfPacedStudents.size());
    }

    /** Build subject breakdown from monthly sessions */
    private List<SubjectMonthlyStatsDTO> buildSubjectBreakdown(List<ClassSession> sessions) {
        Map<Long, SubjectStatsAggregator> subjectMap = new HashMap<>();

        // Aggregate stats by subject
        for (ClassSession session : sessions) {
            if (session.getTutorClass() == null || session.getTutorClass().getCourse() == null) {
                continue;
            }

            Course course = session.getTutorClass().getCourse();
            if (course.getSyllabus() == null) continue;

            Long subjectId = course.getSyllabus().getSubjectId();
            String subjectName = course.getSyllabus().getName();

            SubjectStatsAggregator stats =
                    subjectMap.computeIfAbsent(
                            subjectId, id -> new SubjectStatsAggregator(subjectId, subjectName));

            stats.addSession(session);
        }

        // Convert to DTOs and sort by teaching hours
        return subjectMap.values().stream()
                .map(
                        stats ->
                                SubjectMonthlyStatsDTO.builder()
                                        .subjectId(stats.getSubjectId())
                                        .subjectName(stats.getSubjectName())
                                        .teachingHours(stats.getTotalHours())
                                        .sessionsCompleted(stats.getSessionCount())
                                        .classesCount(stats.getClassCount())
                                        .build())
                .sorted(
                        Comparator.comparingDouble(SubjectMonthlyStatsDTO::getTeachingHours)
                                .reversed())
                .collect(Collectors.toList());
    }

    /** Build class statistics from monthly sessions */
    private List<ClassMonthlyStatsDTO> buildClassStats(List<ClassSession> sessions) {
        Map<Long, ClassStatsAggregator> classMap = new HashMap<>();

        // Aggregate stats by class
        for (ClassSession session : sessions) {
            if (session.getTutorClass() == null) continue;

            TutorClass tutorClass = session.getTutorClass();
            Long classId = tutorClass.getId();

            ClassStatsAggregator stats =
                    classMap.computeIfAbsent(
                            classId,
                            id ->
                                    new ClassStatsAggregator(
                                            classId,
                                            tutorClass.getTitle(),
                                            tutorClass.getCurrentStudents()));

            stats.addSession(session);
        }

        // Convert to DTOs and sort by teaching hours
        return classMap.values().stream()
                .map(
                        stats ->
                                ClassMonthlyStatsDTO.builder()
                                        .classId(stats.getClassId())
                                        .classTitle(stats.getClassTitle())
                                        .teachingHours(stats.getTotalHours())
                                        .sessionsCompleted(stats.getSessionCount())
                                        .enrolledStudents(stats.getEnrolledStudents())
                                        .build())
                .sorted(
                        Comparator.comparingDouble(ClassMonthlyStatsDTO::getTeachingHours)
                                .reversed())
                .collect(Collectors.toList());
    }

    /** Map ClassSession to SessionDetailDTO */
    private SessionDetailDTO mapToSessionDetailDTO(ClassSession session) {
        Double durationHours = null;
        if (session.getStartTime() != null && session.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
            durationHours = minutes / 60.0;
        }

        return SessionDetailDTO.builder()
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .durationHours(durationHours)
                .topic(session.getTopic())
                .build();
    }

    private Map<String, BigDecimal> calculateBatchTutorRevenues(
            List<String> tutorIds, YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        // 1. Batch fetch ALL sessions for all tutorsS
        List<ClassSession> allSessions =
                classSessionRepository.findCompletedSessionsByTutorsInDateRange(
                        tutorIds, startOfMonth, endOfMonth);

        if (allSessions.isEmpty()) {
            return tutorIds.stream().collect(Collectors.toMap(id -> id, id -> BigDecimal.ZERO));
        }

        // 2. Extract unique class IDs and course IDs
        Set<Long> classIds =
                allSessions.stream()
                        .filter(s -> s.getTutorClass() != null)
                        .map(s -> s.getTutorClass().getId())
                        .collect(Collectors.toSet());

        Set<Long> courseIds =
                allSessions.stream()
                        .filter(
                                s ->
                                        s.getTutorClass() != null
                                                && s.getTutorClass().getCourse() != null)
                        .map(s -> s.getTutorClass().getCourse().getId())
                        .collect(Collectors.toSet());

        // 3. Batch fetch all classes (to get totalSessions per class)
        Map<Long, TutorClass> classesById =
                tutorClassRepository.findAllById(classIds).stream()
                        .collect(Collectors.toMap(TutorClass::getId, c -> c));

        // 4. Batch fetch all courses
        Map<Long, Course> coursesById =
                courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, c -> c));

        // 5. Get experience levels for all tutors
        Map<String, String> experienceByTutor =
                tutorProfileRepository.findByUserIds(tutorIds).stream()
                        .collect(
                                Collectors.toMap(
                                        tp -> tp.getUser().getUserId(),
                                        tp ->
                                                tp.getExperience() != null
                                                        ? tp.getExperience()
                                                        : "0-1 years"));

        // 6. Group sessions by tutor
        Map<String, List<ClassSession>> sessionsByTutor =
                allSessions.stream()
                        .collect(
                                Collectors.groupingBy(
                                        session -> session.getTutorClass().getTutor().getUserId()));

        // 7. Calculate revenue for each tutor
        Map<String, BigDecimal> revenueByTutor = new HashMap<>();

        for (String tutorId : tutorIds) {
            BigDecimal totalRevenue = BigDecimal.ZERO;
            String experience = experienceByTutor.getOrDefault(tutorId, "0-1 years");

            List<ClassSession> tutorSessions = sessionsByTutor.get(tutorId);
            if (tutorSessions != null) {
                totalRevenue =
                        calculateTutorRevenueFromSessions(
                                tutorSessions, classesById, coursesById, experience);
            }

            revenueByTutor.put(tutorId, totalRevenue);
        }

        return revenueByTutor;
    }

    private BigDecimal calculateTutorRevenueFromSessions(
            List<ClassSession> sessions,
            Map<Long, TutorClass> classesById,
            Map<Long, Course> coursesById,
            String experience) {

        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Group sessions by course and class
        Map<Long, Map<Long, List<ClassSession>>> sessionsByCourseAndClass =
                sessions.stream()
                        .filter(
                                s ->
                                        s.getTutorClass() != null
                                                && s.getTutorClass().getCourse() != null)
                        .collect(
                                Collectors.groupingBy(
                                        s -> s.getTutorClass().getCourse().getId(),
                                        Collectors.groupingBy(s -> s.getTutorClass().getId())));

        for (Map.Entry<Long, Map<Long, List<ClassSession>>> courseEntry :
                sessionsByCourseAndClass.entrySet()) {
            Long courseId = courseEntry.getKey();
            Map<Long, List<ClassSession>> classSessions = courseEntry.getValue();

            Course course = coursesById.get(courseId);
            if (course == null || course.getPrice() == null) continue;

            if (course.getType() == CourseType.ONLINE) {
                // For online courses: calculate revenue per class
                for (Map.Entry<Long, List<ClassSession>> classEntry : classSessions.entrySet()) {
                    Long classId = classEntry.getKey();
                    int completedSessions = classEntry.getValue().size();

                    TutorClass tutorClass = classesById.get(classId);
                    if (tutorClass == null) continue;

                    int totalSessions = tutorClass.getTotalSessions(); // Get from class entity

                    BigDecimal classRevenue =
                            RevenueCalculationUtil.calculateOnlineCourseRevenue(
                                    completedSessions,
                                    totalSessions,
                                    course.getPrice(),
                                    experience);

                    totalRevenue = totalRevenue.add(classRevenue);
                }

            } else if (course.getType() == CourseType.SELF_PACED) {
                // For self-paced courses: calculate once per course (regardless of session count)
                BigDecimal courseRevenue =
                        RevenueCalculationUtil.calculateSelfPacedCourseRevenue(
                                course.getPrice(), experience);

                totalRevenue = totalRevenue.add(courseRevenue);
            }
        }

        return totalRevenue;
    }

    /** Calculate online revenue using RevenueCalculationUtil */
    private BigDecimal calculateOnlineRevenue(List<ClassSession> sessions, String experience) {
        BigDecimal onlineRevenue = BigDecimal.ZERO;

        // Group sessions by course and sum online revenue
        Map<Long, List<ClassSession>> sessionsByCourse =
                sessions.stream()
                        .filter(
                                s ->
                                        s.getTutorClass() != null
                                                && s.getTutorClass().getCourse() != null)
                        .filter(s -> s.getTutorClass().getCourse().getType() == CourseType.ONLINE)
                        .collect(Collectors.groupingBy(s -> s.getTutorClass().getCourse().getId()));

        for (Map.Entry<Long, List<ClassSession>> entry : sessionsByCourse.entrySet()) {
            Long courseId = entry.getKey();
            List<ClassSession> courseSessions = entry.getValue();

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getPrice() == null) continue;

            int completedSessions = courseSessions.size();
            // For now, assume completed sessions equals total sessions for the month

            onlineRevenue =
                    onlineRevenue.add(
                            RevenueCalculationUtil.calculateOnlineCourseRevenue(
                                    completedSessions,
                                    completedSessions,
                                    course.getPrice(),
                                    experience));
        }

        return onlineRevenue;
    }

    /** Calculate self-paced revenue using RevenueCalculationUtil */
    private BigDecimal calculateSelfPacedRevenue(List<ClassSession> sessions, String experience) {
        BigDecimal selfPacedRevenue = BigDecimal.ZERO;

        // Group sessions by course and sum self-paced revenue
        Map<Long, Integer> courseSessionCounts =
                sessions.stream()
                        .filter(
                                s ->
                                        s.getTutorClass() != null
                                                && s.getTutorClass().getCourse() != null)
                        .filter(
                                s ->
                                        s.getTutorClass().getCourse().getType()
                                                == CourseType.SELF_PACED)
                        .collect(
                                Collectors.groupingBy(
                                        s -> s.getTutorClass().getCourse().getId(),
                                        Collectors.collectingAndThen(
                                                Collectors.toList(), List::size)));

        for (Map.Entry<Long, Integer> entry : courseSessionCounts.entrySet()) {
            Long courseId = entry.getKey();

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getPrice() == null) continue;

            // For self-paced courses, we just need the course price regardless of session count
            selfPacedRevenue =
                    selfPacedRevenue.add(
                            RevenueCalculationUtil.calculateSelfPacedCourseRevenue(
                                    course.getPrice(), experience));
        }

        return selfPacedRevenue;
    }

    /** Helper class to aggregate tutor statistics */
    @Data
    private static class TutorStats {
        private final String tutorId;
        private final String tutorName;
        private final Set<String> uniqueStudents = new HashSet<>();
        private int sessionsCount = 0;
        private double hours = 0.0;

        public TutorStats(String tutorId, String tutorName) {
            this.tutorId = tutorId;
            this.tutorName = tutorName;
        }

        public void incrementSessions() {
            this.sessionsCount++;
        }

        public void addHours(double hours) {
            this.hours += hours;
        }

        public void addStudent(String studentId) {
            this.uniqueStudents.add(studentId);
        }

        public int getStudentsCount() {
            return uniqueStudents.size();
        }
    }

    /** Helper class for student counts by course type */
    private record StudentCountsByType(int onlineStudents, int selfPacedStudents) {}

    /** Helper class to aggregate subject statistics */
    @Data
    private static class SubjectStatsAggregator {
        private final Long subjectId;
        private final String subjectName;
        private final Set<Long> uniqueClassIds = new HashSet<>();
        private int sessionCount = 0;
        private double totalHours = 0.0;

        public SubjectStatsAggregator(Long subjectId, String subjectName) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
        }

        public void addSession(ClassSession session) {
            sessionCount++;
            uniqueClassIds.add(session.getTutorClass().getId());

            if (session.getStartTime() != null && session.getEndTime() != null) {
                long minutes =
                        ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
                totalHours += minutes / 60.0;
            }
        }

        public int getClassCount() {
            return uniqueClassIds.size();
        }
    }

    /** Helper class to aggregate class statistics */
    @Getter
    private static class ClassStatsAggregator {
        private final Long classId;
        private final String classTitle;
        private final Integer enrolledStudents;
        private int sessionCount = 0;
        private double totalHours = 0.0;

        public ClassStatsAggregator(Long classId, String classTitle, Integer enrolledStudents) {
            this.classId = classId;
            this.classTitle = classTitle;
            this.enrolledStudents = enrolledStudents;
        }

        public void addSession(ClassSession session) {
            sessionCount++;

            if (session.getStartTime() != null && session.getEndTime() != null) {
                long minutes =
                        ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
                totalHours += minutes / 60.0;
            }
        }
    }
}
