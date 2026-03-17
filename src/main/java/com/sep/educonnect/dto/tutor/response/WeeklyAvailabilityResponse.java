package com.sep.educonnect.dto.tutor.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyAvailabilityResponse {

    String userId;
    String tutorName;
    LocalDate startDate;
    LocalDate endDate;
    List<DaySchedule> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {
        Integer dayOfWeek;
        String dayName;
        LocalDate date;
        Boolean isWorkDay;
        List<SlotInfo> slots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotInfo {
        Integer slotNumber;
        String slotName;
        String timeRange;
        Boolean isAvailable;
        Boolean isBooked;

        // Session info
        Long sessionId;
        Long classId;
        Integer sessionNumber;

        // Exception info
        Boolean hasException;
        String exceptionReason;

        // Schedule change info
        Boolean hasScheduleChange;
        ScheduleChangeInfo scheduleChangeInfo;

        // Slot status: AVAILABLE, BOOKED, EXCEPTION, MOVED_FROM, MOVED_TO, UNAVAILABLE
        String slotStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleChangeInfo {
        Long scheduleChangeId;
        LocalDate oldDate;
        LocalDate newDate;
        Integer newSlot;
        String content;
        Boolean isOldDate; // true nếu đây là ngày gốc (lịch đã chuyển đi)
        String changeDirection; // MOVED_FROM hoặc MOVED_TO
    }
}
