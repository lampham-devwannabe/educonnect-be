package com.sep.educonnect.service;

import com.sep.educonnect.dto.course.response.MyCourseResponse;
import com.sep.educonnect.dto.student.CheckInviteRequest;
import com.sep.educonnect.dto.student.StudentBookingListItemResponse;
import com.sep.educonnect.dto.student.StudentGeneralResponse;
import com.sep.educonnect.dto.subject.request.SetPreferencesRequest;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.CourseMapper;
import com.sep.educonnect.mapper.TagMapper;
import com.sep.educonnect.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {
    final TutorAvailabilityRepository tutorAvailabilityRepository;
    final TutorAvailabilityExceptionRepository tutorAvailabilityExceptionRepository;
    final TutorProfileRepository tutorProfileRepository;
    final ClassSessionRepository sessionRepo;
    final ScheduleChangeRepository scheduleChangeRepository;
    private final UserRepository userRepository;
    private final BookingMemberRepository bookingMemberRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentLikesRepository studentLikesRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseMapper courseMapper;
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<StudentGeneralResponse> getStudentsForInvitation() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        User user =
                userRepository
                        .findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<User> students =
                userRepository.findByRole_NameAndIsDeletedFalse("STUDENT").stream()
                        .filter(s -> !s.getUserId().equals(user.getUserId()))
                        .toList();

        return students.stream()
                .map(
                        student ->
                                StudentGeneralResponse.builder()
                                        .userId(student.getUserId())
                                        .name(student.getFirstName() + " " + student.getLastName())
                                        .email(student.getEmail())
                                        .build())
                .toList();
    }

    public String checkCanInviteStudent(CheckInviteRequest request) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        User user =
                userRepository
                        .findByUsernameAndNotDeleted(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User toInvite =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Not student or the same email with current -> error
        if (!toInvite.getRole().getName().equals("STUDENT")
                || toInvite.getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        boolean checkBookingExist =
                bookingMemberRepository.existsByUserIdAndBooking_Course_Id(
                        toInvite.getUserId(), request.getCourseId());

        if (checkBookingExist) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
        }

        return toInvite.getUserId();
    }

    public WeeklyAvailabilityResponse getWeeklySchedule(String tutorId, LocalDate startDate) {
        User user =
                userRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy tutor profile
        TutorProfile tutorProfile =
                tutorProfileRepository
                        .findByUserUserIdAndSubmissionStatus(
                                user.getUserId(), ProfileStatus.APPROVED)
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        // Lấy availability
        TutorAvailability availability =
                tutorAvailabilityRepository
                        .findByUserUserId(user.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.TUTOR_AVAILABILITY_NOT_SET));

        // Lấy tất cả slots từ enum
        List<TeachingSlot> allSlots = TeachingSlot.getAllSlots();

        // Lấy các sessions trong tuần
        LocalDate endDate = startDate.plusDays(6);
        List<ClassSession> sessions = sessionRepo.findByTutorAndDateRange(user.getUserId(), startDate, endDate);

        log.info("Sessions from {} to {}: {}", startDate, endDate, sessions.size());

        // Lấy schedule changes có liên quan đến tuần này
        List<ScheduleChange> scheduleChanges = scheduleChangeRepository
                .findByCreatedByAndStatus(user.getUsername(), "APPROVED")
                .stream()
                .filter(
                        sc -> {
                            LocalDate oldDate = sc.getOldDate();
                            LocalDate newDate = sc.getNewDate();
                            // Lấy những schedule change mà oldDate HOẶC newDate nằm trong
                            // tuần
                            return (!oldDate.isBefore(startDate)
                                    && !oldDate.isAfter(endDate))
                                    || (!newDate.isBefore(startDate)
                                    && !newDate.isAfter(endDate));
                        })
                .toList();

        // Lấy sessionIds từ schedule changes để query thêm sessions bị thiếu
        Set<Long> scheduleChangeSessionIds = scheduleChanges.stream()
                .map(sc -> sc.getSession().getId())
                .collect(Collectors.toSet());

        // Lấy thêm các sessions liên quan đến schedule changes (có thể nằm ngoài range)
        List<ClassSession> additionalSessions = new ArrayList<>();
        if (!scheduleChangeSessionIds.isEmpty()) {
            for (Long sessionId : scheduleChangeSessionIds) {
                additionalSessions.add(sessionRepo.findById(sessionId).orElseThrow());
            }
        }

        // Merge sessions
        Map<Long, ClassSession> allSessionsMap = new HashMap<>();
        sessions.forEach(s -> allSessionsMap.put(s.getId(), s));
        additionalSessions.forEach(s -> allSessionsMap.put(s.getId(), s));

        log.info("Total sessions after merge: {}", allSessionsMap.size());

        // Lấy exceptions trong tuần (chỉ lấy APPROVED)
        List<TutorAvailabilityException> exceptions =
                tutorAvailabilityExceptionRepository
                        .findByTutorProfileId(tutorProfile.getId())
                        .stream()
                        .filter(
                                e -> {
                                    LocalDate sessionDate = e.getSession().getSessionDate();
                                    return !sessionDate.isBefore(startDate)
                                            && !sessionDate.isAfter(endDate)
                                            && e.getStatus() == ExceptionStatus.APPROVED;
                                })
                        .toList();

        // Map: sessionId -> exception
        Map<Long, TutorAvailabilityException> exceptionMap =
                exceptions.stream()
                        .collect(
                                Collectors.toMap(
                                        e -> e.getSession().getId(), e -> e, (e1, e2) -> e1));

        // Map: sessionId -> schedule change
        Map<Long, ScheduleChange> scheduleChangeMap =
                scheduleChanges.stream()
                        .collect(
                                Collectors.toMap(
                                        sc -> sc.getSession().getId(),
                                        sc -> sc,
                                        (sc1, sc2) -> sc1));

        // Map: date -> Map<slotNumber, session>
        // CHỈ map những session có sessionDate nằm trong tuần (bỏ qua oldDate của
        // schedule change)
        Map<LocalDate, Map<Integer, ClassSession>> sessionMap = new HashMap<>();
        for (ClassSession session : allSessionsMap.values()) {
            LocalDate sessionDate = session.getSessionDate();

            // Kiểm tra xem session này có bị chuyển đi không
            ScheduleChange change = scheduleChangeMap.get(session.getId());

            if (change != null && change.getOldDate().equals(sessionDate)) {
                // Session này đã bị chuyển đi, chỉ map vào oldDate (để hiển thị MOVED_FROM)
                if (!sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)) {
                    sessionMap
                            .computeIfAbsent(sessionDate, k -> new HashMap<>())
                            .put(session.getSlotNumber(), session);
                }
            } else {
                // Session bình thường, map vào sessionDate
                if (!sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)) {
                    sessionMap
                            .computeIfAbsent(sessionDate, k -> new HashMap<>())
                            .put(session.getSlotNumber(), session);
                }
            }
        }

        // Map: (newDate, newSlot) -> ScheduleChange (để hiển thị MOVED_TO)
        Map<String, ScheduleChange> movedToMap = new HashMap<>();
        for (ScheduleChange sc : scheduleChanges) {
            LocalDate newDate = sc.getNewDate();
            // Chỉ map nếu newDate nằm trong tuần
            if (!newDate.isBefore(startDate) && !newDate.isAfter(endDate)) {
                String key = newDate + "_" + sc.getNewSLot();
                movedToMap.put(key, sc);
            }
        }

        // Build response
        String[] dayNames = {"Chủ Nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
        List<WeeklyAvailabilityResponse.DaySchedule> days = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7;

            boolean isWorkDay = availability.isWorkOnDay(dayOfWeek);
            List<Integer> availableSlots = availability.getSlotsByDay(dayOfWeek);
            Map<Integer, ClassSession> daySessions = sessionMap.getOrDefault(currentDate, new HashMap<>());

            // Lấy slots: available HOẶC có session HOẶC có lịch chuyển đến
            List<WeeklyAvailabilityResponse.SlotInfo> slotInfos = allSlots.stream()
                    .filter(
                            slot -> {
                                String movedToKey = currentDate + "_"
                                        + slot.getSlotNumber();
                                return availableSlots.contains(slot.getSlotNumber())
                                        || daySessions.containsKey(
                                        slot.getSlotNumber())
                                        || movedToMap.containsKey(movedToKey);
                            })
                    .map(
                            slot -> {
                                ClassSession session = daySessions
                                        .get(slot.getSlotNumber());
                                TutorAvailabilityException exception = session != null
                                        ? exceptionMap.get(session.getId())
                                        : null;
                                ScheduleChange scheduleChange = session != null
                                        ? scheduleChangeMap.get(session.getId())
                                        : null;

                                // Check xem slot này có phải là điểm đến của schedule
                                // change không
                                String movedToKey = currentDate + "_"
                                        + slot.getSlotNumber();
                                ScheduleChange movedToChange = movedToMap
                                        .get(movedToKey);

                                // Xác định trạng thái slot
                                String slotStatus = "AVAILABLE";
                                ScheduleChange finalScheduleChange = scheduleChange;

                                if (session != null) {
                                    if (exception != null) {
                                        slotStatus = "EXCEPTION";
                                    } else if (scheduleChange != null
                                            && scheduleChange
                                            .getOldDate()
                                            .equals(currentDate)) {
                                        slotStatus = "MOVED_FROM";
                                    } else {
                                        slotStatus = "BOOKED";
                                    }
                                } else if (movedToChange != null) {
                                    // Slot này là điểm đến của lịch chuyển
                                    slotStatus = "MOVED_TO";
                                    finalScheduleChange = movedToChange;
                                    session = movedToChange.getSession();
                                } else if (!availableSlots
                                        .contains(slot.getSlotNumber())) {
                                    slotStatus = "UNAVAILABLE";
                                }

                                return WeeklyAvailabilityResponse.SlotInfo.builder()
                                        .slotNumber(slot.getSlotNumber())
                                        .slotName(slot.getSlotName())
                                        .timeRange(slot.getTimeRange())
                                        .isAvailable(
                                                availableSlots.contains(
                                                        slot.getSlotNumber()))
                                        .isBooked(
                                                session != null
                                                        && exception == null
                                                        && !"MOVED_FROM".equals(
                                                        slotStatus))
                                        .sessionId(session != null
                                                ? session.getId()
                                                : null)
                                        .classId(
                                                session != null
                                                        ? session.getTutorClass()
                                                        .getId()
                                                        : null)
                                        .sessionNumber(
                                                session != null
                                                        ? session.getSessionNumber()
                                                        : null)
                                        .hasException(exception != null)
                                        .exceptionReason(
                                                exception != null
                                                        ? exception.getReason()
                                                        : null)
                                        .hasScheduleChange(
                                                finalScheduleChange != null)
                                        .scheduleChangeInfo(
                                                finalScheduleChange != null
                                                        ? buildScheduleChangeInfo(
                                                        finalScheduleChange,
                                                        currentDate)
                                                        : null)
                                        .slotStatus(slotStatus)
                                        .build();
                            })
                    .collect(Collectors.toList());

            // Chỉ thêm ngày nếu có ít nhất 1 slot
            if (!slotInfos.isEmpty()) {
                days.add(
                        WeeklyAvailabilityResponse.DaySchedule.builder()
                                .dayOfWeek(dayOfWeek)
                                .dayName(dayNames[dayOfWeek])
                                .date(currentDate)
                                .isWorkDay(isWorkDay)
                                .slots(slotInfos)
                                .build());
            }
        }

        return WeeklyAvailabilityResponse.builder()
                .userId(user.getUserId())
                .tutorName(
                        availability.getUser().getFirstName()
                                + " "
                                + availability.getUser().getLastName())
                .startDate(startDate)
                .endDate(endDate)
                .days(days)
                .build();
    }

    // Helper method để build thông tin schedule change
    private WeeklyAvailabilityResponse.ScheduleChangeInfo buildScheduleChangeInfo(
            ScheduleChange scheduleChange, LocalDate currentDate) {
        boolean isOldDate = scheduleChange.getOldDate().equals(currentDate);
        boolean isNewDate = scheduleChange.getNewDate().equals(currentDate);

        return WeeklyAvailabilityResponse.ScheduleChangeInfo.builder()
                .scheduleChangeId(scheduleChange.getId())
                .oldDate(scheduleChange.getOldDate())
                .newDate(scheduleChange.getNewDate())
                .newSlot(scheduleChange.getNewSLot())
                .content(scheduleChange.getContent())
                .isOldDate(isOldDate)
                .changeDirection(isOldDate ? "MOVED_FROM" : isNewDate ? "MOVED_TO" : null)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<StudentBookingListItemResponse> getStudentBookings(
            int page,
            int size,
            BigDecimal amount,
            GroupType groupType,
            BookingStatus bookingStatus,
            CourseType courseType) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository
                .findByUsernameAndNotDeleted(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "booking.createdAt"));

        Page<BookingMember> bookingMembers = bookingMemberRepository.searchByUser(
                user.getUserId(), bookingStatus, groupType, courseType, amount, pageable);

        log.info(
                "Retrieved {} bookings for student: {}",
                bookingMembers.getTotalElements(),
                user.getUserId());

        return bookingMembers.map(this::toStudentBookingItem);
    }

    @Transactional
    public void toggleLikeTutorProfile(Long tutorProfileId) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        User student =
                userRepository
                        .findByUsernameAndNotDeleted(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Get tutor profile
        TutorProfile tutorProfile = tutorProfileRepository
                .findByIdAndStatus(tutorProfileId, ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        // Check if student has been enrolled in any class taught by this tutor
        boolean hasEnrolled = classEnrollmentRepository.existsByStudentUserIdAndTutorUserId(
                student.getUserId(), tutorProfile.getUser().getUserId());

        if (!hasEnrolled) {
            throw new AppException(ErrorCode.STUDENT_NOT_ENROLLED_WITH_TUTOR);
        }

        // Toggle like/unlike
        Optional<StudentLikes> existingLike = studentLikesRepository.findByStudentIdAndTutor_Id(
                student.getUserId(), tutorProfileId);

        if (existingLike.isPresent()) {
            studentLikesRepository.delete(existingLike.get());
        } else {
            StudentLikes newLike = StudentLikes.builder()
                    .studentId(student.getUserId())
                    .tutor(tutorProfile)
                    .build();
            studentLikesRepository.save(newLike);
        }
    }

    private StudentBookingListItemResponse toStudentBookingItem(BookingMember bookingMember) {
        Booking booking = bookingMember.getBooking();
        Course course = booking.getCourse();

        long currentMembers = booking.getBookingMembers().stream()
                .filter(
                        bm -> bm.getStatus() == BookingMemberStatus.APPROVED
                                || bm.getStatus() == BookingMemberStatus.WAITING)
                .count();

        boolean isPaymentRequired = booking.getBookingStatus() == BookingStatus.APPROVED
                && "OWNER".equalsIgnoreCase(bookingMember.getRole());

        boolean hasPaid = booking.getBookingStatus() == BookingStatus.PAID;

        return StudentBookingListItemResponse.builder()
                .id(booking.getId())
                .bookingStatus(booking.getBookingStatus())
                .myStatus(bookingMember.getStatus())
                .registrationType(booking.getRegistrationType())
                .groupType(booking.getGroupType())
                .amount(booking.getTotalAmount())
                .courseName(course != null ? course.getName() : null)
                .scheduleDescription(booking.getScheduleDescription())
                .currentMemberCount((int) currentMembers)
                .isPaymentRequired(isPaymentRequired)
                .hasPaid(hasPaid)
                .bookingDate(booking.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MyCourseResponse> getMyCourses(String status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository
                .findByUsernameAndNotDeleted(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String normalizedStatus = normalizeStatus(status);

        List<MyCourseResponse> responses = new ArrayList<>();
        Set<Long> addedCourseIds = new HashSet<>();

        // Enrolled courses (IN_PROGRESS/COMPLETED) - filtered by status at DB level
        String statusForQuery = "ALL".equals(normalizedStatus) ? null : normalizedStatus;
        List<ClassEnrollment> enrollments = classEnrollmentRepository
                .findByStudentUserIdAndStatusOrderByEnrolledAtDesc(
                        student.getUserId(), statusForQuery);

        for (ClassEnrollment enrollment : enrollments) {
            TutorClass tutorClass = enrollment.getTutorClass();
            if (tutorClass == null || tutorClass.getCourse() == null) {
                continue;
            }

            Course course = tutorClass.getCourse();
            Long courseId = course.getId();

            CourseProgress courseProgress = null;
            try {
                courseProgress = courseProgressRepository.findByEnrollmentId(enrollment.getId()).orElse(null);
            } catch (Exception e) {
                log.warn(
                        "Failed to get course progress for enrollment {}: {}",
                        enrollment.getId(),
                        e.getMessage());
                continue;
            }

            Lesson selectedLesson = findLessonForCourse(courseProgress, course);
            responses.add(courseMapper.toMyCourseResponse(course, tutorClass.getId(), courseProgress, selectedLesson));
            addedCourseIds.add(courseId);
        }

        // WAITING courses (paid booking, no enrollment yet)
        if ("ALL".equals(normalizedStatus) || "WAITING".equals(normalizedStatus)) {
            List<BookingMember> bookingMembers = bookingMemberRepository
                    .findByUserIdOrderByBooking_CreatedAtDesc(
                            student.getUserId());

            for (BookingMember bookingMember : bookingMembers) {
                Booking booking = bookingMember.getBooking();
                if (booking == null || booking.getBookingStatus() != BookingStatus.PAID) {
                    continue;
                }

                Course course = booking.getCourse();
                if (course == null) {
                    continue;
                }

                Long courseId = course.getId();

                if (addedCourseIds.contains(courseId)) {
                    continue;
                }

                boolean hasEnrollment = classEnrollmentRepository
                        .findByStudent_UserIdAndTutorClass_Course_Id(
                                student.getUserId(), courseId)
                        .isPresent();
                if (hasEnrollment) {
                    continue;
                }

                Lesson firstLesson = course.getSyllabus() != null && course.getSyllabus().getSyllabusId() != null
                        ? findFirstLessonOfFirstModule(
                        course.getSyllabus().getSyllabusId())
                        : null;
                responses.add(courseMapper.toMyCourseResponse(course, null, null, firstLesson));
                addedCourseIds.add(courseId);
            }
        }

        return responses;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }
        String normalized = status.trim().toUpperCase();
        Set<String> allowedStatuses = Set.of("ALL", "WAITING", "IN_PROGRESS", "COMPLETED");
        return allowedStatuses.contains(normalized) ? normalized : "ALL";
    }

    private Lesson findFirstLessonOfFirstModule(Long syllabusId) {
        Optional<Module> firstModule = moduleRepository.findFirstBySyllabusIdOrderByOrderNumberAsc(syllabusId);
        if (firstModule.isEmpty()) {
            return null;
        }

        Long moduleId = firstModule.get().getModuleId();
        return lessonRepository.findFirstByModuleIdOrderByOrderNumberAsc(moduleId).orElse(null);
    }

    private Lesson findLessonForCourse(CourseProgress courseProgress, Course course) {
        if (courseProgress == null) {
            return Optional.ofNullable(course)
                    .map(Course::getSyllabus)
                    .map(Syllabus::getSyllabusId)
                    .map(this::findFirstLessonOfFirstModule)
                    .orElse(null);
        }

        // Try to find latest completed lesson first
        Page<Lesson> latestCompletedPage = lessonProgressRepository.findLatestCompletedLessonByCourseProgressId(
                courseProgress.getId(), PageRequest.of(0, 1));

        if (!latestCompletedPage.isEmpty()) {
            return latestCompletedPage.getContent().get(0);
        }

        // Nếu không có lesson đã hoàn thành -> trả lesson đầu tiên của module đầu tiên
        return Optional.ofNullable(course)
                .map(Course::getSyllabus)
                .map(Syllabus::getSyllabusId)
                .map(this::findFirstLessonOfFirstModule)
                .orElse(null);
    }

    @Transactional
    public void setPreferences(SetPreferencesRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User student =
                userRepository
                        .findByUsernameAndNotDeleted(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Long> tagIds = request.getTagIds();
        if (tagIds == null) {
            tagIds = new ArrayList<>();
        }

        // Limit to max 4 tags and filter out non-existent tags
        List<Long> validTagIds = tagIds.stream()
                .distinct()
                .limit(4)
                .filter(tagId -> tagRepository.findById(tagId)
                        .map(tag -> !tag.getIsDeleted())
                        .orElse(false))
                .collect(Collectors.toList());
        student.setHasChosenPreferences(true);
        student.setPreferences(validTagIds);
        userRepository.save(student);

        log.info("Student {} set subject preferences: {}", student.getUserId(), validTagIds);
    }

    public List<TagResponse> getMyPreferredTags() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository
                .findByUsernameAndNotDeleted(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Long> preferences = user.getPreferences(); // assuming this comes from the entity

        List<Tag> filteredTags = tagRepository.findAll().stream()
                .filter(tag -> !tag.getIsDeleted())
                .filter(tag -> preferences != null && preferences.contains(tag.getId()))
                .toList();

        return filteredTags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
}
