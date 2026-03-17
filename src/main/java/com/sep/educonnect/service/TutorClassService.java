package com.sep.educonnect.service;

import com.sep.educonnect.constant.TemplateMail;
import com.sep.educonnect.dto.attendance.AttendanceRequest;
import com.sep.educonnect.dto.attendance.AttendanceResponse;
import com.sep.educonnect.dto.attendance.SessionAttendanceDTO;
import com.sep.educonnect.dto.classenrollment.ClassStudentResponse;
import com.sep.educonnect.dto.classenrollment.StudentClassResponse;
import com.sep.educonnect.dto.classsession.ClassSessionResponse;
import com.sep.educonnect.dto.exam.StudentExamListItemResponse;
import com.sep.educonnect.dto.mail.Email;
import com.sep.educonnect.dto.mail.Mailer;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.student.StudentScheduleDTO;
import com.sep.educonnect.dto.tutor.request.CreateTutorClassRequest;
import com.sep.educonnect.dto.tutor.request.WeeklySchedule;
import com.sep.educonnect.dto.tutor.response.TutorClassResponse;
import com.sep.educonnect.dto.tutorclass.*;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.NotificationLink;
import com.sep.educonnect.enums.NotificationType;
import com.sep.educonnect.enums.TeachingSlot;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.TutorClassMapper;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.email.MailService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorClassService {

    final TutorClassRepository tutorClassRepository;
    final ClassSessionRepository classSessionRepository;
    final ClassEnrollmentRepository classEnrollmentRepository;
    final TutorAvailabilityRepository tutorAvailabilityRepository;
    final CourseRepository courseRepository;
    final UserRepository userRepository;
    final TutorClassMapper tutorClassMapper;
    final MailService mailService;
    final SessionAttendanceRepository sessionAttendanceRepository;
    final BookingMemberRepository bookingMemberRepository;
    final ProgressService progressService;
    final NotificationService notificationService;
    final ScheduleChangeRepository scheduleChangeRepository;
    final LessonRepository lessonRepository;
    final ExamRepository examRepository;
    final ExamSubmissionRepository examSubmissionRepository;

    public TutorClass createTutorClass(CreateTutorClassRequest request) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User tutor =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 1️⃣ Get tutor availability
        TutorAvailability availability =
                tutorAvailabilityRepository
                        .findByUserUserId(tutor.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_AVAILABILITY_NOT_SET));

        Course course =
                courseRepository
                        .findById(request.getCourseId())
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        if (request.getMaxStudents() <= 0) throw new AppException(ErrorCode.INVALID_CLASS_SIZE);

        if (request.getTotalSessions() <= 0)
            throw new AppException(ErrorCode.INVALID_NUMBER_OF_SESSION);

        if (request.getStartDate().isBefore(LocalDate.now()))
            throw new AppException(ErrorCode.INVALID_START_DATE);

        // 2️⃣ Create TutorClass entity
        TutorClass tutorClass = new TutorClass();
        tutorClass.setCourse(course);
        tutorClass.setTitle(request.getTitle());
        tutorClass.setDescription(request.getDescription());
        tutorClass.setMaxStudents(request.getMaxStudents());
        tutorClass.setStartDate(request.getStartDate());

        tutorClass.setTutor(tutor);

        // 3️⃣ Generate sessions based on weekly schedule
        List<ClassSession> sessions = generateSessions(request, availability, tutorClass);

        tutorClass.setEndDate(sessions.getLast().getSessionDate());

        // 4️⃣ Save sessions
        classSessionRepository.saveAll(sessions);
        tutorClass.setSessions(sessions);
        tutorClass = tutorClassRepository.save(tutorClass);
        // 5️⃣ Update tutor availability after class creation
        updateTutorAvailability(availability, sessions);
        tutorAvailabilityRepository.save(availability);

        return tutorClass;
    }

    private List<ClassSession> generateSessions(
            CreateTutorClassRequest request,
            TutorAvailability availability,
            TutorClass tutorClass) {

        List<ClassSession> sessions = new ArrayList<>();
        LocalDate current = request.getStartDate();
        int sessionCount = 0;

        while (sessionCount < request.getTotalSessions()) {

            int dayOfWeek = current.getDayOfWeek().getValue(); // 1-7

            if (dayOfWeek == 7) {
                dayOfWeek = 0;
            }

            // Check weekly schedule input
            for (WeeklySchedule schedule : request.getWeeklySchedules()) {

                if (dayOfWeek == schedule.getDayOfWeek()) {

                    // Tutor must work that day
                    if (!availability.isWorkOnDay(dayOfWeek)) {
                        throw new AppException(ErrorCode.TUTOR_NOT_AVAILABLE);
                    }

                    for (Integer slotNum : schedule.getSlotNumbers()) {

                        // Must be available for slot
                        if (!availability.getSlotsByDay(dayOfWeek).contains(slotNum)) {
                            throw new AppException(ErrorCode.TUTOR_NOT_AVAILABLE);
                        }

                        if (sessionCount >= request.getTotalSessions()) break;

                        TeachingSlot slot = TeachingSlot.values()[slotNum - 1];

                        ClassSession session = new ClassSession();
                        session.setTutorClass(tutorClass);
                        session.setSessionDate(current);
                        session.setSlotNumber(slotNum);
                        session.setSessionNumber(++sessionCount);
                        session.setStartTime(LocalDateTime.of(current, slot.getStartTime()));
                        session.setEndTime(LocalDateTime.of(current, slot.getEndTime()));
                        session.setTopic("Session " + sessionCount);

                        sessions.add(session);
                    }
                }
            }

            // Move to next day
            current = current.plusDays(1);
        }

        return sessions;
    }

    /** ❗ After class creation, mark the used slots as unavailable in TutorAvailability. */
    private void updateTutorAvailability(
            TutorAvailability availability, List<ClassSession> sessions) {
        // Group by day of week
        Map<Integer, Set<Integer>> usedSlotsByDay = new HashMap<>();

        for (ClassSession session : sessions) {
            int day = session.getSessionDate().getDayOfWeek().getValue();
            usedSlotsByDay.computeIfAbsent(day, d -> new HashSet<>()).add(session.getSlotNumber());
        }

        // Remove booked slots
        for (Map.Entry<Integer, Set<Integer>> entry : usedSlotsByDay.entrySet()) {
            int day = entry.getKey();
            List<Integer> currentSlots = availability.getSlotsByDay(day);
            currentSlots.removeAll(entry.getValue());
            availability.setSlotsByDay(day, currentSlots);
        }

        log.info("Tutor availability updated — booked slots removed: {}", usedSlotsByDay);
    }

    public TutorClass getTutorClassByClassId(Long classId) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User tutor =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return tutorClassRepository
                .findByIdAndTutorUserId(classId, tutor.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    public TutorClass getTutorClassById(Long classId) {

        return tutorClassRepository
                .findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    public List<TutorClassResponse> getTutorClasses(LocalDate startDate) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User tutor =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<TutorClass> tutorClasses = tutorClassRepository.findByTutorUserId(tutor.getUserId());

        if (tutorClasses.isEmpty()) {
            throw new AppException(ErrorCode.NO_CLASSES_FOUND);
        }

        if (startDate == null) {
            startDate = LocalDate.now();
        }

        LocalDate finalEndDate = startDate.plusDays(6);
        LocalDate finalStartDate = startDate;

        return tutorClasses.stream()
                .map(
                        tutorClass -> {
                            List<ClassSessionResponse> filteredSessions =
                                    tutorClass.getSessions().stream()
                                            .filter(
                                                    session ->
                                                            !session.getSessionDate()
                                                                            .isBefore(
                                                                                    finalStartDate)
                                                                    && !session.getSessionDate()
                                                                            .isAfter(finalEndDate))
                                            .map(this::mapToClassSessionResponse)
                                            .toList();

                            return mapToTutorClassResponse(tutorClass, filteredSessions);
                        })
                .toList();
    }

    public Page<TutorClassDTO> getAllClass(int page, int size) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User tutor =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(page, size);

        Page<TutorClass> tutorClasses =
                tutorClassRepository.findByTutorUserId(tutor.getUserId(), pageable);
        return tutorClasses.map(this::mapToTutorClassDTO);
    }

    public List<AttendanceResponse> getAttendanceList(Long sessionId) {
        List<SessionAttendance> records = sessionAttendanceRepository.findBySessionId(sessionId);

        return records.stream()
                .map(
                        a ->
                                new AttendanceResponse(
                                        a.getId(),
                                        a.getSession().getId(),
                                        a.getEnrollment().getId(),
                                        a.getEnrollment().getStudent().getUserId(),
                                        a.getEnrollment().getStudent().getFirstName()
                                                + " "
                                                + a.getEnrollment().getStudent().getLastName(),
                                        a.getAttended(),
                                        a.getNotes()))
                .toList();
    }

    public List<AttendanceResponse> createAttendance(List<AttendanceRequest> updates) {

        List<SessionAttendance> attendances = new ArrayList<>();

        for (AttendanceRequest request : updates) {

            ClassEnrollment enrollment =
                    classEnrollmentRepository
                            .findById(request.getEnrollmentId())
                            .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_EXISTED));
            ClassSession session =
                    classSessionRepository
                            .findById(request.getSessionId())
                            .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

            SessionAttendance sessionAttendance =
                    SessionAttendance.builder()
                            .enrollment(enrollment)
                            .session(session)
                            .attended(request.getAttended())
                            .notes(request.getNotes())
                            .build();

            attendances.add(sessionAttendance);
        }

        sessionAttendanceRepository.saveAll(attendances);

        Set<Long> sessionIds =
                updates.stream().map(AttendanceRequest::getSessionId).collect(Collectors.toSet());
        Set<Long> enrollmentIds =
                updates.stream()
                        .map(AttendanceRequest::getEnrollmentId)
                        .collect(Collectors.toSet());

        // Fetch một lần
        Map<Long, ClassSession> sessionMap =
                classSessionRepository.findAllById(sessionIds).stream()
                        .collect(Collectors.toMap(ClassSession::getId, s -> s));

        Map<Long, ClassEnrollment> enrollmentMap =
                classEnrollmentRepository.findAllById(enrollmentIds).stream()
                        .collect(Collectors.toMap(ClassEnrollment::getId, e -> e));

        // Send notifications to students
        for (AttendanceRequest request : updates) {
            ClassSession session = sessionMap.get(request.getSessionId());
            ClassEnrollment enrollment = enrollmentMap.get(request.getEnrollmentId());

            String studentId = enrollment.getStudent().getUserId();
            String sessionTitle =
                    session.getTutorClass() != null
                            ? session.getTutorClass().getTitle()
                            : "Session";
            String actionLink =
                    NotificationLink.SESSION_ATTENDANCE.getPrefix() + "/" + session.getId();
            String message = "Attendance has been recorded for " + sessionTitle + " session";

            notificationService.createAndSendNotification(
                    studentId, message, NotificationType.TYPICAL, null, actionLink);
        }

        // ✅ Map sang AttendanceResponse
        return attendances.stream()
                .map(
                        a ->
                                AttendanceResponse.builder()
                                        .attendanceId(a.getId())
                                        .sessionId(a.getSession().getId())
                                        .enrollmentId(a.getEnrollment().getId())
                                        .studentId(a.getEnrollment().getStudent().getUserId())
                                        .studentName(
                                                a.getEnrollment().getStudent().getFirstName()
                                                        + " "
                                                        + a.getEnrollment()
                                                                .getStudent()
                                                                .getLastName())
                                        .attended(a.getAttended())
                                        .notes(a.getNotes())
                                        .build())
                .toList();
    }

    public List<AttendanceResponse> updateAttendance(List<AttendanceRequest> updates) {
        // Kiểm tra session có tồn tại không
        ClassSession session =
                classSessionRepository
                        .findById(updates.getFirst().getSessionId())
                        .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

        // Lấy tất cả attendance của buổi học
        List<SessionAttendance> existing =
                sessionAttendanceRepository.findBySessionId(updates.getFirst().getSessionId());

        // Duyệt qua danh sách request để cập nhật tương ứng
        for (AttendanceRequest dto : updates) {
            existing.stream()
                    .filter(a -> a.getEnrollment().getId().equals(dto.getEnrollmentId()))
                    .findFirst()
                    .ifPresent(
                            a -> {
                                a.setAttended(dto.getAttended());
                                a.setNotes(dto.getNotes());
                            });
        }

        // 4️⃣ Lưu lại tất cả
        sessionAttendanceRepository.saveAll(existing);

        String sessionTitle =
                session.getTutorClass() != null ? session.getTutorClass().getTitle() : "Session";
        String actionLink = NotificationLink.SESSION_ATTENDANCE.getPrefix() + "/" + session.getId();

        // Get unique student IDs from updates
        Map<Long, SessionAttendance> attendanceMap =
                existing.stream().collect(Collectors.toMap(a -> a.getEnrollment().getId(), a -> a));

        Set<String> notifiedStudents = new HashSet<>();
        for (AttendanceRequest dto : updates) {
            SessionAttendance attendance = attendanceMap.get(dto.getEnrollmentId());
            if (attendance != null) {
                String studentId = attendance.getEnrollment().getStudent().getUserId();
                if (!notifiedStudents.contains(studentId)) {
                    notifiedStudents.add(studentId);
                    // send notification
                    String message = "Attendance has been updated for " + sessionTitle + " session";
                    notificationService.createAndSendNotification(
                            studentId, message, NotificationType.TYPICAL, null, actionLink);
                }
            }
        }

        return existing.stream()
                .map(
                        a ->
                                AttendanceResponse.builder()
                                        .attendanceId(a.getId())
                                        .sessionId(a.getSession().getId())
                                        .enrollmentId(a.getEnrollment().getId())
                                        .studentId(a.getEnrollment().getStudent().getUserId())
                                        .studentName(
                                                a.getEnrollment().getStudent().getFirstName()
                                                        + " "
                                                        + a.getEnrollment()
                                                                .getStudent()
                                                                .getLastName())
                                        .attended(a.getAttended())
                                        .notes(a.getNotes())
                                        .build())
                .toList();
    }

    public List<StudentScheduleDTO> getStudentSchedule(
            String studentId, LocalDate fromDate, LocalDate toDate) {
        List<ClassSession> sessions =
                classSessionRepository.findStudentSchedule(studentId, fromDate, toDate);

        // Lấy danh sách sessionIds
        List<Long> sessionIds = sessions.stream().map(ClassSession::getId).toList();

        // Lấy các thay đổi lịch đã được duyệt
        Map<Long, ScheduleChange> approvedChanges = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            approvedChanges =
                    scheduleChangeRepository.findApprovedChangesBySessionIds(sessionIds).stream()
                            .collect(
                                    Collectors.toMap(
                                            sc -> sc.getSession().getId(),
                                            sc -> sc,
                                            (existing, replacement) ->
                                                    replacement // Nếu có nhiều change, lấy cái mới
                                            // nhất
                                            ));
        }

        // Map sang DTO và apply các thay đổi
        Map<Long, ScheduleChange> finalApprovedChanges = approvedChanges;
        return sessions.stream()
                .map(
                        session -> {
                            ScheduleChange change = finalApprovedChanges.get(session.getId());
                            return StudentScheduleDTO.fromSession(session, studentId, change);
                        })
                .toList();
    }

    // Service
    public List<SessionAttendanceDTO> getStudentAttendanceInClass(Long classId) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User student =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<SessionAttendance> attendances =
                sessionAttendanceRepository.findByStudentAndClass(student.getUserId(), classId);

        return attendances.stream().map(this::convertToDTO).toList();
    }

    private SessionAttendanceDTO convertToDTO(SessionAttendance attendance) {
        return SessionAttendanceDTO.builder()
                .id(attendance.getId())
                .attended(attendance.getAttended())
                .notes(attendance.getNotes())
                .sessionId(attendance.getSession().getId())
                .sessionDate(attendance.getSession().getSessionDate())
                .sessionNumber(attendance.getSession().getSessionNumber())
                .topic(attendance.getSession().getTopic())
                .startTime(attendance.getSession().getStartTime())
                .endTime(attendance.getSession().getEndTime())
                .enrollmentId(attendance.getEnrollment().getId())
                .studentId(attendance.getEnrollment().getStudent().getUserId())
                .studentName(
                        attendance.getEnrollment().getStudent().getFirstName()
                                + attendance.getEnrollment().getStudent().getLastName())
                .classId(attendance.getSession().getTutorClass().getId())
                .className(attendance.getSession().getTutorClass().getTitle())
                .build();
    }

    private TutorClassDTO mapToTutorClassDTO(TutorClass tutorClass) {

        return TutorClassDTO.builder()
                .id(tutorClass.getId())
                .title(tutorClass.getTitle())
                .description(tutorClass.getDescription())
                .course(tutorClassMapper.toCourseDTO(tutorClass.getCourse()))
                .currentStudents(tutorClass.getCurrentStudents())
                .startDate(tutorClass.getStartDate())
                .endDate(tutorClass.getEndDate())
                .startDate(tutorClass.getStartDate())
                .maxStudents(tutorClass.getMaxStudents())
                .build();
    }

    private TutorClassResponse mapToTutorClassResponse(
            TutorClass tutorClass, List<ClassSessionResponse> sessions) {
        TutorClassResponse response = new TutorClassResponse();
        response.setId(tutorClass.getId());
        response.setTutor(tutorClassMapper.toTutorBasicDTO(tutorClass.getTutor()));
        response.setCourse(tutorClassMapper.toCourseDTO(tutorClass.getCourse()));
        response.setStartDate(tutorClass.getStartDate());
        response.setEndDate(tutorClass.getEndDate());
        response.setMaxStudents(tutorClass.getMaxStudents());
        response.setCurrentStudents(tutorClass.getCurrentStudents());
        response.setTitle(tutorClass.getTitle());
        response.setDescription(tutorClass.getDescription());
        response.setSessions(sessions);
        return response;
    }

    private ClassSessionResponse mapToClassSessionResponse(ClassSession session) {
        return tutorClassMapper.toClassSessionDTO(session);
    }

    /**
     * Invite students to a class: - Validate the class belongs to the current tutor - Create
     * ClassEnrollment records - Send class invitation email to each student
     */
    public void inviteStudents(Long classId, List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            log.warn("No student IDs provided for class invitation.");
            return;
        }

        var context = SecurityContextHolder.getContext();
        String tutorUsername = context.getAuthentication().getName();
        User tutor =
                userRepository
                        .findByUsername(tutorUsername)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorClass tutorClass =
                tutorClassRepository
                        .findByIdAndTutorUserId(classId, tutor.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Set<String> alreadyEnrolled = new HashSet<>();
        if (tutorClass.getEnrollments() != null) {
            for (ClassEnrollment enrollment : tutorClass.getEnrollments()) {
                if (enrollment.getStudent() != null
                        && enrollment.getStudent().getUserId() != null) {
                    alreadyEnrolled.add(enrollment.getStudent().getUserId());
                }
            }
        }

        for (String studentId : studentIds) {
            try {
                Optional<User> studentOpt = userRepository.findByIdAndNotDeleted(studentId);
                if (studentOpt.isEmpty()) {
                    log.warn("Student not found or deleted: {}", studentId);
                    continue;
                }
                User student = studentOpt.get();

                if (alreadyEnrolled.contains(student.getUserId())) {
                    log.info(
                            "Student {} already enrolled in class {}",
                            student.getUserId(),
                            classId);
                    continue;
                }

                ClassEnrollment enrollment = new ClassEnrollment();
                enrollment.setTutorClass(tutorClass);
                enrollment.setStudent(student);
                enrollment.setEnrolledAt(LocalDateTime.now());
                enrollment.setHasJoined(false);
                ClassEnrollment savedEnrollment = classEnrollmentRepository.save(enrollment);
                progressService.createCourseProgress(savedEnrollment.getId());

                Email email =
                        Email.builder()
                                .subject("Thư mời tham gia lớp học")
                                .to(List.of(Mailer.builder().email(student.getEmail()).build()))
                                .build();

                Map<String, Object> variables = new HashMap<>();
                variables.put("studentName", student.getFirstName() + " " + student.getLastName());
                variables.put("tutorName", tutor.getFirstName() + " " + tutor.getLastName());
                variables.put("classTitle", tutorClass.getTitle());
                variables.put(
                        "courseName",
                        tutorClass.getCourse() != null ? tutorClass.getCourse().getName() : "");
                variables.put("currentStudents", tutorClass.getCurrentStudents());
                variables.put("description", tutorClass.getDescription());
                String CLASS_URL = "https://educonnect.dev/classes/";
                variables.put("inviteUrl", CLASS_URL + classId);
                variables.put("emailSign", "EduConnect");

                mailService.send(email, TemplateMail.CLASS_INVITATION, variables);
                log.info(
                        "Invitation email sent to student {} for class {}",
                        student.getEmail(),
                        classId);
            } catch (Exception e) {
                log.error(
                        "Failed to invite student {} to class {}: {}",
                        studentId,
                        classId,
                        e.getMessage(),
                        e);
            }
        }

        Integer totalStudent = classEnrollmentRepository.countStudentsInClass(classId);
        tutorClass.setCurrentStudents(totalStudent);
        tutorClassRepository.save(tutorClass);
    }

    public List<StudentGeneralResponse> getStudentsToInvite(Long classId) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User tutor =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorClass tutorClass =
                tutorClassRepository
                        .findByIdAndTutorUserId(classId, tutor.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        Long courseId = tutorClass.getCourse().getId();
        // Lấy PAID hoặc (APPROVED AND TRIAL)
        List<BookingMember> members = bookingMemberRepository.findPaidOrApprovedTrialByCourseId(courseId);
        Set<String> enrolledStudentIds =
                classEnrollmentRepository.findDistinctStudentUserIdsByCourseId(courseId);

        return members.stream()
                .filter(member -> !enrolledStudentIds.contains(member.getUserId()))
                .map(
                        member -> {
                            User student =
                                    userRepository
                                            .findByIdAndNotDeleted(member.getUserId())
                                            .orElseThrow(
                                                    () ->
                                                            new AppException(
                                                                    ErrorCode.USER_NOT_EXISTED));
                            return StudentGeneralResponse.builder()
                                    .userId(student.getUserId())
                                    .name(student.getFirstName() + " " + student.getLastName())
                                    .email(student.getEmail())
                                    .build();
                        })
                .toList();
    }

    // Get list of students in a class
    public List<ClassStudentResponse> getClassStudents(Long classId) {
        tutorClassRepository
                .findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        List<ClassEnrollment> enrollments =
                classEnrollmentRepository.findByTutorClassIdOrderByEnrolledAtAsc(classId);

        return enrollments.stream()
                .map(
                        enrollment ->
                                ClassStudentResponse.builder()
                                        .enrollmentId(enrollment.getId())
                                        .student(
                                                ClassStudentResponse.StudentInfo.builder()
                                                        .userId(enrollment.getStudent().getUserId())
                                                        .firstName(
                                                                enrollment
                                                                        .getStudent()
                                                                        .getFirstName())
                                                        .lastName(
                                                                enrollment
                                                                        .getStudent()
                                                                        .getLastName())
                                                        .email(enrollment.getStudent().getEmail())
                                                        .avatar(enrollment.getStudent().getAvatar())
                                                        .build())
                                        .enrolledAt(enrollment.getEnrolledAt())
                                        .notes(enrollment.getNotes())
                                        .hasJoined(enrollment.getHasJoined())
                                        .build())
                .toList();
    }

    /**
     * Lấy danh sách các classes mà student hiện tại đã enroll.
     *
     * @return List of classes với thông tin enrollment, tutor và course
     */
    public List<StudentClassResponse> getStudentClasses(
            String classTitle, LocalDate startDate, LocalDate endDate, String tutorName) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser =
                userRepository
                        .findByUsernameAndNotDeleted(currentUsername)
                        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        List<ClassEnrollment> enrollments =
                classEnrollmentRepository.findByStudentUserIdOrderByEnrolledAtDesc(
                        currentUser.getUserId());
        log.info(
                "Retrieved {} classes for student: {}",
                enrollments.size(),
                currentUser.getUserId());

        return enrollments.stream()
                .filter(
                        enrollment -> {
                            TutorClass tutorClass = enrollment.getTutorClass();

                            boolean titleMatch =
                                    classTitle == null
                                            || (tutorClass.getTitle() != null
                                                    && tutorClass
                                                            .getTitle()
                                                            .toLowerCase()
                                                            .contains(classTitle.toLowerCase()));

                            boolean startMatch =
                                    startDate == null
                                            || (tutorClass.getStartDate() != null
                                                    && !tutorClass
                                                            .getStartDate()
                                                            .isBefore(startDate));

                            boolean endMatch =
                                    endDate == null
                                            || (tutorClass.getEndDate() != null
                                                    && !tutorClass.getEndDate().isAfter(endDate));

                            boolean tutorMatch = true;
                            if (tutorName != null && tutorClass.getTutor() != null) {
                                String name =
                                        (tutorClass.getTutor().getFirstName()
                                                        + " "
                                                        + tutorClass.getTutor().getLastName())
                                                .trim()
                                                .toLowerCase();
                                tutorMatch = name.contains(tutorName.toLowerCase());
                            } else if (tutorName != null) {
                                tutorMatch = false;
                            }

                            return titleMatch && startMatch && endMatch && tutorMatch;
                        })
                .map(
                        enrollment -> {
                            TutorClass tutorClass = enrollment.getTutorClass();
                            User tutor = tutorClass.getTutor();
                            Course course = tutorClass.getCourse();

                            return StudentClassResponse.builder()
                                    .enrollmentId(enrollment.getId())
                                    .enrolledAt(enrollment.getEnrolledAt())
                                    .hasJoined(enrollment.getHasJoined())
                                    .classId(tutorClass.getId())
                                    .classTitle(tutorClass.getTitle())
                                    .startDate(tutorClass.getStartDate())
                                    .endDate(tutorClass.getEndDate())
                                    .currentStudents(tutorClass.getCurrentStudents())
                                    .maxStudents(tutorClass.getMaxStudents())
                                    .tutorId(tutor.getUserId())
                                    .tutorName(tutor.getFirstName() + " " + tutor.getLastName())
                                    .tutorAvatar(tutor.getAvatar())
                                    .courseId(course.getId())
                                    .courseName(course.getName())
                                    .build();
                        })
                .toList();
    }

    public Page<StudentExamListItemResponse> getStudentExamsInClass(
            Long classId, String studentId, int page, int size) {

        // Kiểm tra enrollment
        Optional<ClassEnrollment> enrollment =
                classEnrollmentRepository.findByTutorClassIdAndStudentUserId(classId, studentId);

        if (enrollment.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Lấy class
        TutorClass tutorClass =
                tutorClassRepository
                        .findById(classId)
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (tutorClass.getCourse() == null || tutorClass.getCourse().getSyllabus() == null) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Lấy lessons từ syllabus
        Long syllabusId = tutorClass.getCourse().getSyllabus().getSyllabusId();
        List<Lesson> lessons = lessonRepository.findBySyllabusId(syllabusId);

        if (lessons.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        List<Long> lessonIds = lessons.stream().map(Lesson::getLessonId).toList();

        // Lấy tất cả exams (global hoặc specific cho class) — không lọc theo status
        List<Exam> exams = examRepository.findByLessonIdInAndClass(lessonIds, classId);

        // Map lesson titles
        Map<Long, String> lessonTitleMap =
                lessons.stream().collect(Collectors.toMap(Lesson::getLessonId, Lesson::getTitle));

        // Map submissions
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
                        .toList();

        // Phân trang
        int start = (int) PageRequest.of(page, size).getOffset();
        int end = Math.min((start + size), responses.size());
        List<StudentExamListItemResponse> pagedResponses = responses.subList(start, end);

        return new PageImpl<>(pagedResponses, PageRequest.of(page, size), responses.size());
    }

    // API cho giáo viên
    public Page<TeacherClassExamResponse> getClassExams(Long classId, int page, int size) {
        // Kiểm tra class
        TutorClass tutorClass =
                tutorClassRepository
                        .findById(classId)
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // Lấy danh sách học sinh
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByTutorClassId(classId);

        if (enrollments.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Lấy syllabus và lessons
        if (tutorClass.getCourse() == null || tutorClass.getCourse().getSyllabus() == null) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        Long syllabusId = tutorClass.getCourse().getSyllabus().getSyllabusId();
        List<Lesson> lessons = lessonRepository.findBySyllabusId(syllabusId);

        if (lessons.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        List<Long> lessonIds = lessons.stream().map(Lesson::getLessonId).toList();

        // Lấy exams - include global exams or exams specific to this class
        List<Exam> exams = examRepository.findByLessonIdInAndClass(lessonIds, classId);

        // Map lesson titles
        Map<Long, String> lessonTitleMap =
                lessons.stream().collect(Collectors.toMap(Lesson::getLessonId, Lesson::getTitle));

        // Lấy student IDs
        List<String> studentIds =
                enrollments.stream().map(e -> e.getStudent().getUserId()).toList();

        Map<String, ClassEnrollment> studentEnrollmentMap =
                enrollments.stream()
                        .collect(Collectors.toMap(e -> e.getStudent().getUserId(), e -> e));

        // Map responses
        List<TeacherClassExamResponse> responses =
                exams.stream()
                        .map(
                                exam ->
                                        buildExamResponse(
                                                exam,
                                                lessonTitleMap,
                                                studentIds,
                                                studentEnrollmentMap,
                                                classId))
                        .toList();

        // Phân trang
        int start = (int) PageRequest.of(page, size).getOffset();
        int end = Math.min((start + size), responses.size());
        List<TeacherClassExamResponse> pagedResponses = responses.subList(start, end);

        return new PageImpl<>(pagedResponses, PageRequest.of(page, size), responses.size());
    }

    private TeacherClassExamResponse buildExamResponse(
            Exam exam,
            Map<Long, String> lessonTitleMap,
            List<String> studentIds,
            Map<String, ClassEnrollment> studentEnrollmentMap,
            Long classId) {

        String lessonTitle = lessonTitleMap.getOrDefault(exam.getLessonId(), "");
        String examTitle = exam.getField() != null ? exam.getField() : lessonTitle;

        // Lấy tổng số sessions của class
        Integer totalSessions = classSessionRepository.countByTutorClassId(classId);

        // Build student details
        List<StudentExamDetail> studentDetails = new ArrayList<>();
        int submittedCount = 0;
        double totalScore = 0;
        int scoredCount = 0;

        int totalPresentCount = 0;
        int totalAbsentCount = 0;

        for (String studentId : studentIds) {
            // Lấy submission info
            Optional<Double> bestScore =
                    examSubmissionRepository.findBestScoreByExamIdAndStudentId(
                            exam.getExamId(), studentId);

            Long submissionCount =
                    examSubmissionRepository.countByExamIdAndStudentId(exam.getExamId(), studentId);

            boolean submitted = submissionCount > 0;
            if (submitted) {
                submittedCount++;
            }

            if (bestScore.isPresent()) {
                totalScore += bestScore.get();
                scoredCount++;
            }

            // Lấy thông tin attendance của student trong class này
            AttendanceInfo attendanceInfo =
                    getStudentAttendanceInfo(studentId, classId, totalSessions);

            totalPresentCount += attendanceInfo.getPresentCount();
            totalAbsentCount += attendanceInfo.getAbsentCount();

            ClassEnrollment enrollment = studentEnrollmentMap.get(studentId);
            User student = enrollment.getStudent();

            studentDetails.add(
                    StudentExamDetail.builder()
                            .studentId(studentId)
                            .studentName(student.getFirstName() + " " + student.getLastName())
                            .studentEmail(student.getEmail())
                            .submitted(submitted)
                            .bestScore(bestScore.orElse(null))
                            .submissionCount(submissionCount.intValue())
                            .attendanceInfo(attendanceInfo)
                            .build());
        }

        Double averageScore = scoredCount > 0 ? totalScore / scoredCount : null;

        // Tính attendance summary trung bình
        int studentCount = studentIds.size();
        AttendanceSummary attendanceSummary =
                AttendanceSummary.builder()
                        .totalSessions(totalSessions)
                        .averagePresent(studentCount > 0 ? totalPresentCount / studentCount : 0)
                        .averageAbsent(studentCount > 0 ? totalAbsentCount / studentCount : 0)
                        .build();

        return TeacherClassExamResponse.builder()
                .examId(exam.getExamId())
                .lessonId(exam.getLessonId())
                .lessonTitle(lessonTitle)
                .examTitle(examTitle)
                .status(exam.getStatus())
                .totalStudents(studentIds.size())
                .submittedCount(submittedCount)
                .notSubmittedCount(studentIds.size() - submittedCount)
                .averageScore(averageScore)
                .attendanceSummary(attendanceSummary)
                .studentDetails(studentDetails)
                .build();
    }

    // Lấy thông tin attendance của 1 học sinh trong class
    private AttendanceInfo getStudentAttendanceInfo(
            String studentId, Long classId, Integer totalSessions) {
        // Tìm enrollment của student trong class
        Optional<ClassEnrollment> enrollmentOpt =
                classEnrollmentRepository.findByTutorClassIdAndStudentUserId(classId, studentId);

        if (enrollmentOpt.isEmpty()) {
            return AttendanceInfo.builder()
                    .totalSessions(totalSessions)
                    .presentCount(0)
                    .absentCount(0)
                    .attendanceRate(0.0)
                    .build();
        }

        // Lấy tất cả session attendances thông qua enrollment
        List<SessionAttendance> attendances =
                sessionAttendanceRepository.findByEnrollmentId(enrollmentOpt.get().getId());

        int presentCount = 0;
        int absentCount = 0;

        for (SessionAttendance attendance : attendances) {
            Boolean check = attendance.getAttended();
            if (check) {
                presentCount++;
            } else {
                absentCount++;
            }
        }

        double attendanceRate =
                totalSessions > 0 ? (double) presentCount / totalSessions * 100 : 0.0;

        return AttendanceInfo.builder()
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                .build();
    }
}
