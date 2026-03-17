package com.sep.educonnect.service;

import com.sep.educonnect.dto.tutor.request.AvailabilityUpdateRequest;
import com.sep.educonnect.dto.tutor.response.WeeklyAvailabilityResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.ExceptionStatus;
import com.sep.educonnect.enums.ProfileStatus;
import com.sep.educonnect.enums.TeachingSlot;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TutorAvailabilityService {

    final TutorAvailabilityRepository tutorAvailabilityRepository;
    final TutorAvailabilityExceptionRepository tutorAvailabilityExceptionRepository;
    final TutorProfileRepository tutorProfileRepository;
    final UserRepository userRepository;
    final ClassSessionRepository sessionRepo;
    final ScheduleChangeRepository scheduleChangeRepository;

    public TutorAvailability updateAvailability(AvailabilityUpdateRequest request) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        TutorAvailability availability = tutorAvailabilityRepository
                .findByUserUserId(user.getUserId())
                .orElse(new TutorAvailability());

        if (availability.getUser() == null) {
            availability.setUser(user);
        }


        // Reset tất cả ngày về false
        availability.setIsWorkOnMonday(false);
        availability.setIsWorkOnTuesday(false);
        availability.setIsWorkOnWednesday(false);
        availability.setIsWorkOnThursday(false);
        availability.setIsWorkOnFriday(false);
        availability.setIsWorkOnSaturday(false);
        availability.setIsWorkOnSunday(false);

        // Cập nhật theo request
        for (Map.Entry<Integer, List<Integer>> entry : request.getWeekSchedule().entrySet()) {
            Integer dayOfWeek = entry.getKey();
            List<Integer> slotNumbers = entry.getValue();
            availability.setSlotsByDay(dayOfWeek, slotNumbers);
        }

        return tutorAvailabilityRepository.save(availability);
    }


    public WeeklyAvailabilityResponse getWeeklySchedule(LocalDate startDate) {

        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Lấy tutor profile
        TutorProfile tutorProfile = tutorProfileRepository
                .findByUserUserIdAndSubmissionStatus(user.getUserId(), ProfileStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_PROFILE_NOT_EXISTED));

        // Lấy availability
        TutorAvailability availability = tutorAvailabilityRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.TUTOR_AVAILABILITY_NOT_SET));

        // Lấy tất cả slots từ enum
        List<TeachingSlot> allSlots = TeachingSlot.getAllSlots();

        // Lấy các sessions trong tuần
        LocalDate endDate = startDate.plusDays(6);
        List<ClassSession> sessions = sessionRepo
                .findByTutorAndDateRange(user.getUserId(), startDate, endDate);

        log.info("Sessions from {} to {}: {}", startDate, endDate, sessions.size());

        // Lấy schedule changes có liên quan đến tuần này
        List<ScheduleChange> scheduleChanges = scheduleChangeRepository
                .findByCreatedByAndStatus(user.getUsername(), "APPROVED")
                .stream()
                .filter(sc -> {
                    LocalDate oldDate = sc.getOldDate();
                    LocalDate newDate = sc.getNewDate();
                    // Lấy những schedule change mà oldDate HOẶC newDate nằm trong tuần
                    return (!oldDate.isBefore(startDate) && !oldDate.isAfter(endDate))
                            || (!newDate.isBefore(startDate) && !newDate.isAfter(endDate));
                })
                .collect(Collectors.toList());

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
        List<TutorAvailabilityException> exceptions = tutorAvailabilityExceptionRepository
                .findByTutorProfileId(tutorProfile.getId())
                .stream()
                .filter(e -> {
                    LocalDate sessionDate = e.getSession().getSessionDate();
                    return !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)
                            && e.getStatus() == ExceptionStatus.APPROVED;
                })
                .collect(Collectors.toList());

        // Map: sessionId -> exception
        Map<Long, TutorAvailabilityException> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(
                        e -> e.getSession().getId(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        // Map: sessionId -> schedule change
        Map<Long, ScheduleChange> scheduleChangeMap = scheduleChanges.stream()
                .collect(Collectors.toMap(
                        sc -> sc.getSession().getId(),
                        sc -> sc,
                        (sc1, sc2) -> sc1
                ));

        // Map: date -> Map<slotNumber, session>
        // CHỈ map những session có sessionDate nằm trong tuần (bỏ qua oldDate của schedule change)
        Map<LocalDate, Map<Integer, ClassSession>> sessionMap = new HashMap<>();
        for (ClassSession session : allSessionsMap.values()) {
            LocalDate sessionDate = session.getSessionDate();

            // Kiểm tra xem session này có bị chuyển đi không
            ScheduleChange change = scheduleChangeMap.get(session.getId());

            if (change != null && change.getOldDate().equals(sessionDate)) {
                // Session này đã bị chuyển đi, chỉ map vào oldDate (để hiển thị MOVED_FROM)
                if (!sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)) {
                    sessionMap.computeIfAbsent(sessionDate, k -> new HashMap<>())
                            .put(session.getSlotNumber(), session);
                }
            } else {
                // Session bình thường, map vào sessionDate
                if (!sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)) {
                    sessionMap.computeIfAbsent(sessionDate, k -> new HashMap<>())
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
                    .filter(slot -> {
                        String movedToKey = currentDate + "_" + slot.getSlotNumber();
                        return availableSlots.contains(slot.getSlotNumber())
                                || daySessions.containsKey(slot.getSlotNumber())
                                || movedToMap.containsKey(movedToKey);
                    })
                    .map(slot -> {
                        ClassSession session = daySessions.get(slot.getSlotNumber());
                        TutorAvailabilityException exception = session != null
                                ? exceptionMap.get(session.getId()) : null;
                        ScheduleChange scheduleChange = session != null
                                ? scheduleChangeMap.get(session.getId()) : null;

                        // Check xem slot này có phải là điểm đến của schedule change không
                        String movedToKey = currentDate + "_" + slot.getSlotNumber();
                        ScheduleChange movedToChange = movedToMap.get(movedToKey);

                        // Xác định trạng thái slot
                        String slotStatus = "AVAILABLE";
                        ScheduleChange finalScheduleChange = scheduleChange;

                        if (session != null) {
                            if (exception != null) {
                                slotStatus = "EXCEPTION";
                            } else if (scheduleChange != null && scheduleChange.getOldDate().equals(currentDate)) {
                                slotStatus = "MOVED_FROM";
                            } else {
                                slotStatus = "BOOKED";
                            }
                        } else if (movedToChange != null) {
                            // Slot này là điểm đến của lịch chuyển
                            slotStatus = "MOVED_TO";
                            finalScheduleChange = movedToChange;
                            session = movedToChange.getSession();
                        } else if (!availableSlots.contains(slot.getSlotNumber())) {
                            slotStatus = "UNAVAILABLE";
                        }

                        return WeeklyAvailabilityResponse.SlotInfo.builder()
                                .slotNumber(slot.getSlotNumber())
                                .slotName(slot.getSlotName())
                                .timeRange(slot.getTimeRange())
                                .isAvailable(availableSlots.contains(slot.getSlotNumber()))
                                .isBooked(session != null && exception == null && !"MOVED_FROM".equals(slotStatus))
                                .sessionId(session != null ? session.getId() : null)
                                .classId(session != null ? session.getTutorClass().getId() : null)
                                .sessionNumber(session != null ? session.getSessionNumber() : null)
                                .hasException(exception != null)
                                .exceptionReason(exception != null ? exception.getReason() : null)
                                .hasScheduleChange(finalScheduleChange != null)
                                .scheduleChangeInfo(finalScheduleChange != null
                                        ? buildScheduleChangeInfo(finalScheduleChange, currentDate) : null)
                                .slotStatus(slotStatus)
                                .build();
                    })
                    .collect(Collectors.toList());

            // Chỉ thêm ngày nếu có ít nhất 1 slot
            if (!slotInfos.isEmpty()) {
                days.add(WeeklyAvailabilityResponse.DaySchedule.builder()
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
                .tutorName(availability.getUser().getFirstName() + " " + availability.getUser().getLastName())
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


}
