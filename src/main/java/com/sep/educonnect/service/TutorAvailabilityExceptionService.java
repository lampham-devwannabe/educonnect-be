package com.sep.educonnect.service;

import com.sep.educonnect.dto.exception.request.ApproveExceptionRequest;
import com.sep.educonnect.dto.exception.request.BatchCreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.CreateExceptionRequest;
import com.sep.educonnect.dto.exception.request.UpdateExceptionRequest;
import com.sep.educonnect.dto.exception.response.ExceptionListResponse;
import com.sep.educonnect.dto.exception.response.ExceptionResponse;
import com.sep.educonnect.dto.tutor.request.CreateScheduleChangeRequest;
import com.sep.educonnect.dto.tutor.response.ScheduleChangeResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ExceptionMapper;
import com.sep.educonnect.mapper.ScheduleChangeMapper;
import com.sep.educonnect.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorAvailabilityExceptionService {

    TutorAvailabilityExceptionRepository exceptionRepository;
    TutorProfileRepository tutorProfileRepository;
    ClassSessionRepository classSessionRepository;
    UserRepository userRepository;
    ExceptionMapper exceptionMapper;
    ScheduleChangeRepository scheduleChangeRepository;
    ScheduleChangeMapper scheduleChangeMapper;
    NotificationService notificationService;
    ClassEnrollmentRepository classEnrollmentRepository;

    private static final int MIN_HOURS_BEFORE_SESSION = 24;
    private static final int MIN_DAYS_BEFORE_SCHEDULE_CHANGE = 7;

    // ==================== EXISTING METHODS ====================

    /**
     * Tutor xin nghỉ 1 buổi học
     */
    public ExceptionResponse createException(CreateExceptionRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        ClassSession session = classSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

        // Validate: session phải thuộc về tutor này
        if (!session.getTutorClass().getTutor().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate: không thể xin nghỉ buổi học đã qua
        if (session.getSessionDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_PAST_SESSION);
        }

        // Validate: phải xin nghỉ trước ít nhất 24h
        if (session.getStartTime().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_SESSION))) {
            throw new AppException(ErrorCode.EXCEPTION_TOO_LATE);
        }

        // Kiểm tra đã xin nghỉ buổi này chưa
        if (exceptionRepository.existsBySessionIdAndTutorProfileIdAndStatusNot(
                session.getId(), tutorProfile.getId(), ExceptionStatus.CANCELLED)) {
            throw new AppException(ErrorCode.EXCEPTION_ALREADY_EXISTS);
        }

        // Kiểm tra số lần xin nghỉ trong tháng (giới hạn 5 lần/tháng)
        Long exceptionCount = exceptionRepository.countExceptionsSince(
                tutorProfile.getId(),
                LocalDateTime.now().minusMonths(1));

        if (exceptionCount >= 5) {
            log.warn("Tutor {} exceeded monthly exception limit: {}", username, exceptionCount);
            throw new AppException(ErrorCode.EXCEPTION_LIMIT_EXCEEDED);
        }

        TutorAvailabilityException exception = TutorAvailabilityException.builder()
                .tutorProfile(tutorProfile)
                .session(session)
                .reason(request.getReason())
                .status(ExceptionStatus.PENDING)
                .isApproved(false)
                .build();

        return exceptionMapper.toResponse(exceptionRepository.save(exception));
    }

    /**
     * Tutor xin nghỉ nhiều buổi học cùng lúc
     */
    public List<ExceptionResponse> createBatchExceptions(BatchCreateExceptionRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        List<TutorAvailabilityException> exceptions = new ArrayList<>();

        for (Long sessionId : request.getSessionIds()) {
            try {
                ClassSession session = classSessionRepository.findById(sessionId)
                        .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

                // Skip nếu không phải session của tutor này
                if (!session.getTutorClass().getTutor().getUserId().equals(user.getUserId())) {
                    log.warn("Session {} does not belong to tutor {}", sessionId, username);
                    continue;
                }

                // Skip nếu đã có exception
                if (exceptionRepository.existsBySessionIdAndTutorProfileId(
                        sessionId, tutorProfile.getId())) {
                    log.warn("Exception already exists for session {}", sessionId);
                    continue;
                }

                // Skip nếu quá khứ hoặc quá gần
                if (session.getSessionDate().isBefore(LocalDate.now()) ||
                        session.getStartTime().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_SESSION))) {
                    log.warn("Session {} is in the past or too close", sessionId);
                    continue;
                }

                TutorAvailabilityException exception = TutorAvailabilityException.builder()
                        .tutorProfile(tutorProfile)
                        .session(session)
                        .reason(request.getReason())
                        .status(ExceptionStatus.PENDING)
                        .isApproved(false)
                        .build();

                exceptions.add(exception);

            } catch (Exception e) {
                log.error("Error creating exception for session {}: {}", sessionId, e.getMessage());
            }
        }

        if (exceptions.isEmpty()) {
            throw new AppException(ErrorCode.NO_VALID_SESSIONS);
        }

        exceptions = exceptionRepository.saveAll(exceptions);
        log.info("Created {} exceptions by tutor {}", exceptions.size(), username);

        return exceptionMapper.toResponseList(exceptions);
    }

    /**
     * Tutor cập nhật lý do xin nghỉ (chỉ khi PENDING)
     */
    public ExceptionResponse updateException(Long exceptionId, UpdateExceptionRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        TutorAvailabilityException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCEPTION_NOT_FOUND));

        // Validate ownership
        if (!exception.getTutorProfile().getId().equals(tutorProfile.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chỉ update được khi PENDING
        if (exception.getStatus() != ExceptionStatus.PENDING) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_PROCESSED_EXCEPTION);
        }

        if (request.getReason() != null) {
            exception.setReason(request.getReason());
        }

        exception = exceptionRepository.save(exception);
        return exceptionMapper.toResponse(exception);
    }

    /**
     * Tutor hủy đơn xin nghỉ (chỉ khi PENDING)
     */
    public void cancelException(Long exceptionId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        TutorAvailabilityException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCEPTION_NOT_FOUND));

        if (!exception.getTutorProfile().getId().equals(tutorProfile.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (exception.getStatus() != ExceptionStatus.PENDING) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_PROCESSED_EXCEPTION);
        }

        exception.setStatus(ExceptionStatus.CANCELLED);
        exceptionRepository.save(exception);

        log.info("Cancelled exception {} by tutor {}", exceptionId, username);
    }

    /**
     * Lấy danh sách đơn xin nghỉ của tutor
     */
    public ExceptionListResponse getMyExceptions(ExceptionStatus status) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        List<TutorAvailabilityException> exceptions;

        if (status != null) {
            exceptions = exceptionRepository.findByTutorProfileIdAndStatus(
                    tutorProfile.getId(), status);
        } else {
            exceptions = exceptionRepository.findByTutorProfileId(tutorProfile.getId());
        }

        List<ExceptionResponse> responses = exceptionMapper.toResponseList(exceptions);

        Map<ExceptionStatus, Long> statusCounts = exceptions.stream()
                .collect(Collectors.groupingBy(
                        TutorAvailabilityException::getStatus,
                        Collectors.counting()));

        return ExceptionListResponse.builder()
                .exceptions(responses)
                .totalCount((long) exceptions.size())
                .pendingCount(statusCounts.getOrDefault(ExceptionStatus.PENDING, 0L))
                .approvedCount(statusCounts.getOrDefault(ExceptionStatus.APPROVED, 0L))
                .rejectedCount(statusCounts.getOrDefault(ExceptionStatus.REJECTED, 0L))
                .build();
    }

    /**
     * Admin duyệt/từ chối đơn xin nghỉ
     */
    public ExceptionResponse approveException(ApproveExceptionRequest request) {
        var context = SecurityContextHolder.getContext();
        String adminUsername = context.getAuthentication().getName();

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorAvailabilityException exception = exceptionRepository.findById(request.getExceptionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXCEPTION_NOT_FOUND));

        if (exception.getStatus() != ExceptionStatus.PENDING) {
            throw new AppException(ErrorCode.EXCEPTION_ALREADY_PROCESSED);
        }

        if (request.getApproved()) {
            exception.setStatus(ExceptionStatus.APPROVED);
            exception.setIsApproved(true);

            log.info("Admin {} approved exception {}", adminUsername, exception.getId());

            // TODO: Cập nhật session status, notify students, refund nếu cần

        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
            }

            exception.setStatus(ExceptionStatus.REJECTED);
            exception.setIsApproved(false);
            exception.setRejectionReason(request.getRejectionReason());

            log.info("Admin {} rejected exception {}", adminUsername, exception.getId());
        }

        exception.setApprovedBy(admin.getUserId());
        exception.setApprovedAt(LocalDateTime.now());

        exception = exceptionRepository.save(exception);

        // Send notifications only when approved
        if (request.getApproved()) {
            String tutorId = exception.getTutorProfile().getUser().getUserId();
            ClassSession session = exception.getSession();
            String sessionTitle = session.getTutorClass() != null ? session.getTutorClass().getTitle() : "Session";
            String actionLink = NotificationLink.TUTOR_SCHEDULE.getPrefix() + "/" + exception.getId();

            // Send notification to tutor
            String tutorMessage = "Your exception request for " + sessionTitle + " has been approved";
            notificationService.createAndSendNotification(
                    tutorId,
                    tutorMessage,
                    NotificationType.TYPICAL,
                    null,
                    actionLink
            );

            // Send notifications to students enrolled in the class
            if (session.getTutorClass() != null) {
                List<ClassEnrollment> enrollments = classEnrollmentRepository.findByTutorClassId(session.getTutorClass().getId());
                if (!enrollments.isEmpty()) {
                    String studentMessage = "Session " + sessionTitle + " has been cancelled by the tutor";
                    List<String> studentIds = enrollments.stream()
                            .filter(e -> e.getStudent() != null)
                            .map(e -> e.getStudent().getUserId())
                            .toList();

                    notificationService.createAndSendNotifications(
                            studentIds,
                            studentMessage,
                            NotificationType.TYPICAL,
                            null,
                            null
                    );
                }
            }
        }

        return exceptionMapper.toResponse(exception);
    }

    /**
     * Admin lấy tất cả đơn chờ duyệt
     */
    public Page<ExceptionResponse> getPendingExceptions(int page, int size, String sortBy) {
        // Mặc định sort theo createdAt DESC nếu không có sortBy
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        org.springframework.data.domain.Sort sort =
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TutorAvailabilityException> exceptions = exceptionRepository.findPendingExceptions(pageable);
        return exceptions.map(exceptionMapper::toResponse);
    }

    public Page<ExceptionResponse> getAllExceptions(ExceptionStatus status, int page, int size, String sortBy) {
        // Mặc định sort theo createdAt DESC nếu không có sortBy
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        org.springframework.data.domain.Sort sort =
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TutorAvailabilityException> exceptions;

        if (status != null) {
            exceptions = exceptionRepository.findByStatus(status, pageable);
        } else {
            exceptions = exceptionRepository.findAllWithDetails(pageable);
        }

        return exceptions.map(exceptionMapper::toResponse);
    }

    // ==================== NEW SCHEDULE CHANGE METHODS ====================

    /**
     * Tutor tạo yêu cầu thay đổi lịch dạy cố định
     * Đổi từ ngày/slot cũ sang ngày/slot mới
     */
    public ScheduleChangeResponse createScheduleChange(CreateScheduleChangeRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        ClassSession session = classSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

        // Validate: session phải thuộc về tutor này
        if (!session.getTutorClass().getTutor().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Validate: session phải được đăng kí exception
        if (exceptionRepository.findBySessionAndIsApproved(request.getSessionId(),true).isEmpty()) {
            throw new AppException(ErrorCode.SESSION_MUST_REGISTER_EXCEPTION);
        }

        // Validate: ngày đổi phải là trong tương lai
        if (request.getNewDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.NEW_DATE_MUST_BE_FUTURE);
        }

        // Validate: phải yêu cầu trước ít nhất 7 ngày
        if (request.getNewDate().isBefore(LocalDate.now().plusDays(MIN_DAYS_BEFORE_SCHEDULE_CHANGE))) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_TOO_LATE);
        }

        // Validate: ngày cũ và ngày mới không được giống nhau
        if (request.getOldDate().equals(request.getNewDate())) {
            throw new AppException(ErrorCode.OLD_AND_NEW_DATE_SAME);
        }

        // Kiểm tra đã có yêu cầu thay đổi pending cho session này chưa
        if (scheduleChangeRepository.existsBySessionIdAndStatusPending(session.getId())) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_ALREADY_EXISTS);
        }

        if (!(request.getNewSlot() >= 1 && request.getNewSlot() <= 10)) {
            throw new AppException(ErrorCode.INVALID_TEACHING_SLOT);
        }

        String overlap = isOverlap(user.getUserId(), request.getNewDate(), request.getNewSlot(), session.getTutorClass().getId());
        if (!overlap.equals("OK")) {
            if (overlap.equals("TUTOR"))
                throw new AppException(ErrorCode.OVERLAP_TUTOR_SCHEDULE);
            throw new AppException(ErrorCode.OVERLAP_STUDENT_SCHEDULE);
        }

        ScheduleChange scheduleChange = ScheduleChange.builder()
                .session(session)
                .oldDate(request.getOldDate())
                .newDate(request.getNewDate())
                .newSLot(TeachingSlot.findBySlotNumber(request.getNewSlot()).orElseThrow(() -> new AppException(ErrorCode.INVALID_TEACHING_SLOT)).getSlotNumber())
                .content(request.getContent())
                .status("PENDING")
                .build();

        scheduleChange = scheduleChangeRepository.save(scheduleChange);

        log.info("Created schedule change {} for session {} by tutor {}. Old date: {}, New date: {}",
                scheduleChange.getId(), session.getId(), username,
                request.getOldDate(), request.getNewDate());

        // TODO: Gửi notification cho admin và students

        log.info("{}", scheduleChangeMapper.toResponse(scheduleChange));

        return scheduleChangeMapper.toResponse(scheduleChange);
    }

    private String isOverlap(String tutorId, LocalDate newDate, int newSlot, long classId) {
        // 1. Check tutor's regular session conflict
        if (classSessionRepository.findBySessionDateAndSlotNumber(tutorId, newDate, newSlot).isPresent()) {
            return "TUTOR";
        }

        // 2. Check tutor's APPROVED exception (time-off) conflicts
        if (exceptionRepository.existsByTutorUserIdAndDateAndSlotAndStatus(
                tutorId, newDate, newSlot, ExceptionStatus.APPROVED)) {
            return "TUTOR"; // Tutor has approved time-off on this date/slot
        }

        // 3. Check tutor's APPROVED schedule changes that move sessions TO this date/slot
        if (scheduleChangeRepository.existsByTutorAndNewDateAndNewSlotAndStatus(
                tutorId, newDate, newSlot, "APPROVED")) {
            return "TUTOR"; // Tutor already has a rescheduled session on this date/slot
        }

        // 4. Get all students enrolled in this class
        List<ClassEnrollment> studentInClass = classEnrollmentRepository.findByTutorClassId(classId);
        List<String> studentIds = studentInClass.stream()
                .map(s -> s.getStudent().getUserId())
                .toList();

        if (studentIds.isEmpty()) {
            return "OK"; // No students to check
        }

        // 5. Check students' regular session conflicts
        if (classSessionRepository.countStudentConflicts(studentIds, newDate, newSlot, classId) > 0) {
            return "STUDENT";
        }

        // 6. Check students' APPROVED exceptions across all their enrolled classes
        if (exceptionRepository.countStudentExceptionConflicts(
                studentIds, newDate, newSlot, ExceptionStatus.APPROVED) > 0) {
            return "STUDENT"; // One or more students have approved time-off
        }

        // 7. Check students' APPROVED schedule changes across all their enrolled classes
        if (scheduleChangeRepository.countStudentScheduleChangeConflicts(
                studentIds, newDate, newSlot, "APPROVED") > 0) {
            return "STUDENT"; // One or more students have sessions rescheduled to this slot
        }

        return "OK";
    }

    /**
     * Tutor cập nhật yêu cầu thay đổi lịch (chỉ khi PENDING)
     */
    public ScheduleChangeResponse updateScheduleChange(Long scheduleChangeId, CreateScheduleChangeRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ScheduleChange scheduleChange = scheduleChangeRepository.findById(scheduleChangeId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND));

        // Validate ownership
        if (!scheduleChange.getCreatedBy().equals(user.getUsername())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getOldDate().equals(request.getNewDate())) {
            throw new AppException(ErrorCode.OLD_AND_NEW_DATE_SAME);
        }

        if (!(request.getNewSlot() >= 1 && request.getNewSlot() <= 10)) {
            throw new AppException(ErrorCode.INVALID_TEACHING_SLOT);
        }

        // Chỉ update được khi PENDING
        if (!"PENDING".equals(scheduleChange.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_PROCESSED_SCHEDULE_CHANGE);
        }

        String overlap = isOverlap(user.getUserId(), request.getNewDate(), request.getNewSlot(), scheduleChange.getSession().getTutorClass().getId());
        if (!overlap.equals("OK")) {
            if (overlap.equals("TUTOR"))
                throw new AppException(ErrorCode.OVERLAP_TUTOR_SCHEDULE);
            throw new AppException(ErrorCode.OVERLAP_STUDENT_SCHEDULE);
        }

        if (request.getNewDate() != null) {
            if (request.getNewDate().isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.NEW_DATE_MUST_BE_FUTURE);
            }

            if (request.getNewDate().isBefore(LocalDate.now().plusDays(MIN_DAYS_BEFORE_SCHEDULE_CHANGE))) {
                throw new AppException(ErrorCode.SCHEDULE_CHANGE_TOO_LATE);
            }

            scheduleChange.setNewDate(request.getNewDate());
        }

        if (request.getNewSlot() != null) {
            scheduleChange.setNewSLot(TeachingSlot.findBySlotNumber(request.getNewSlot()).orElseThrow(
                    () -> new AppException(ErrorCode.INVALID_TEACHING_SLOT)
            ).getSlotNumber());
        }

        if (request.getContent() != null) {
            scheduleChange.setContent(request.getContent());
        }

        scheduleChange.setModifiedBy(user.getUserId());

        scheduleChange = scheduleChangeRepository.save(scheduleChange);
        return scheduleChangeMapper.toResponse(scheduleChange);
    }

    /**
     * Tutor hủy yêu cầu thay đổi lịch (chỉ khi PENDING)
     */
    public void cancelScheduleChange(Long scheduleChangeId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ScheduleChange scheduleChange = scheduleChangeRepository.findById(scheduleChangeId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND));

        if (!scheduleChange.getCreatedBy().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!"PENDING".equals(scheduleChange.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_PROCESSED_SCHEDULE_CHANGE);
        }

        scheduleChange.setStatus("CANCELLED");
        scheduleChange.setModifiedBy(user.getUserId());
        scheduleChangeRepository.save(scheduleChange);

        log.info("Cancelled schedule change {} by tutor {}", scheduleChangeId, username);
    }

    /**
     * Lấy danh sách yêu cầu thay đổi lịch của tutor
     */
    public List<ScheduleChangeResponse> getMyScheduleChanges(String status, LocalDate startDate) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        LocalDate endDate = startDate.plusDays(6);
        List<ScheduleChange> scheduleChanges;

        if (status != null && !status.isBlank()) {
            scheduleChanges = scheduleChangeRepository.findByCreatedByAndStatusAndNewDateBetween(
                    user.getUsername(), status, startDate, endDate);
        } else {
            scheduleChanges = scheduleChangeRepository.findByCreatedByAndNewDateBetween(
                    user.getUsername(), startDate, endDate);
        }

        return scheduleChangeMapper.toResponseList(scheduleChanges);
    }

    /**
     * Admin duyệt/từ chối yêu cầu thay đổi lịch
     */
    public ScheduleChangeResponse approveScheduleChange(Long scheduleChangeId, Boolean approved, String rejectionReason) {
        var context = SecurityContextHolder.getContext();
        String adminUsername = context.getAuthentication().getName();

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ScheduleChange scheduleChange = scheduleChangeRepository.findById(scheduleChangeId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_CHANGE_NOT_FOUND));

        if (!"PENDING".equals(scheduleChange.getStatus())) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_ALREADY_PROCESSED);
        }

        if (approved) {
            scheduleChange.setStatus("APPROVED");
            log.info("Admin {} approved schedule change {}", adminUsername, scheduleChangeId);

            // TODO: Cập nhật session date, notify tutor và students

        } else {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
            }

            scheduleChange.setStatus("REJECTED");
            scheduleChange.setContent(rejectionReason);
            log.info("Admin {} rejected schedule change {}", adminUsername, scheduleChangeId);
        }

        scheduleChange.setModifiedBy(admin.getUserId());
        scheduleChange = scheduleChangeRepository.save(scheduleChange);

        // Send notifications only when approved
        if (approved) {
            ClassSession session = scheduleChange.getSession();
            if (session != null && session.getTutorClass() != null) {
                String sessionTitle = session.getTutorClass().getTitle() != null 
                        ? session.getTutorClass().getTitle() 
                        : "Session";
                String actionLink = NotificationLink.TUTOR_SCHEDULE.getPrefix() + "/" + scheduleChange.getId();

                // Send notification to tutor
                if (session.getTutorClass().getTutor() != null) {
                    String tutorId = session.getTutorClass().getTutor().getUserId();
                    String tutorMessage = String.format(
                            "Your schedule change request for %s has been approved. New date: %s",
                            sessionTitle,
                            scheduleChange.getNewDate()
                    );
                    notificationService.createAndSendNotification(
                            tutorId,
                            tutorMessage,
                            NotificationType.TYPICAL,
                            null,
                            actionLink
                    );
                }

                // Send notifications to students enrolled in the class
                List<ClassEnrollment> enrollments = classEnrollmentRepository.findByTutorClassId(session.getTutorClass().getId());
                if (!enrollments.isEmpty()) {
                    String studentMessage = String.format(
                            "Schedule change for %s has been approved. New date: %s",
                            sessionTitle,
                            scheduleChange.getNewDate()
                    );
                    List<String> studentIds = enrollments.stream()
                            .filter(e -> e.getStudent() != null)
                            .map(e -> e.getStudent().getUserId())
                            .toList();

                    notificationService.createAndSendNotifications(
                            studentIds,
                            studentMessage,
                            NotificationType.TYPICAL,
                            null,
                            null
                    );
                }
            }
        }

        return scheduleChangeMapper.toResponse(scheduleChange);
    }

    /**
     * Admin lấy tất cả yêu cầu thay đổi lịch chờ duyệt
     */
    public Page<ScheduleChangeResponse> getPendingScheduleChanges(String className, int page, int size, String sortBy) {
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        org.springframework.data.domain.Sort sort =
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ScheduleChange> scheduleChanges =
                scheduleChangeRepository.searchScheduleChanges(
                        "PENDING", normalize(className), pageable);
        return scheduleChanges.map(scheduleChangeMapper::toResponse);
    }

    /**
     * Admin lấy tất cả yêu cầu thay đổi lịch
     */
    public Page<ScheduleChangeResponse> getAllScheduleChanges(
            String status, String className, int page, int size, String sortBy) {
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        org.springframework.data.domain.Sort sort =
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ScheduleChange> scheduleChanges =
                scheduleChangeRepository.searchScheduleChanges(
                        normalize(status), normalize(className), pageable);

        return scheduleChanges.map(scheduleChangeMapper::toResponse);
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

}