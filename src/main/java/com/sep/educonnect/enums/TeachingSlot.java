package com.sep.educonnect.enums;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum TeachingSlot {
    SLOT_1(1, "Ca 1", LocalTime.of(7, 0), LocalTime.of(8, 30)),
    SLOT_2(2, "Ca 2", LocalTime.of(8, 30), LocalTime.of(10, 0)),
    SLOT_3(3, "Ca 3", LocalTime.of(10, 0), LocalTime.of(11, 30)),
    SLOT_4(4, "Ca 4", LocalTime.of(11, 30), LocalTime.of(13, 0)),
    SLOT_5(5, "Ca 5", LocalTime.of(13, 0), LocalTime.of(14, 30)),
    SLOT_6(6, "Ca 6", LocalTime.of(14, 30), LocalTime.of(16, 0)),
    SLOT_7(7, "Ca 7", LocalTime.of(16, 0), LocalTime.of(17, 30)),
    SLOT_8(8, "Ca 8", LocalTime.of(17, 30), LocalTime.of(19, 0)),
    SLOT_9(9, "Ca 9", LocalTime.of(19, 0), LocalTime.of(20, 30)),
    SLOT_10(10, "Ca 10", LocalTime.of(20, 30), LocalTime.of(22, 0));

    private final int slotNumber;
    private final String slotName;
    private final LocalTime startTime;
    private final LocalTime endTime;

    TeachingSlot(int slotNumber, String slotName, LocalTime startTime, LocalTime endTime) {
        this.slotNumber = slotNumber;
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static List<TeachingSlot> getAllSlots() {
        return Arrays.asList(values());
    }

    public static Optional<TeachingSlot> findBySlotNumber(Integer slotNumber) {
        if (slotNumber == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(slot -> slot.slotNumber == slotNumber)
                .findFirst();
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    public static LocalTime getSlotStartTime(String slotNumber) {
        LocalTime[] times = new LocalTime[] {TeachingSlot.valueOf(slotNumber).startTime, TeachingSlot.valueOf(slotNumber).endTime};
        return times != null ? times[0] : null;
    }

    public static LocalTime getSlotEndTime(String slotNumber) {
        LocalTime[] times = new LocalTime[] {TeachingSlot.valueOf(slotNumber).startTime, TeachingSlot.valueOf(slotNumber).endTime};
        return times != null ? times[1] : null;
    }

    public static LocalDateTime getSlotStartDateTime(LocalDate date, String slotNumber) {
        LocalTime startTime = getSlotStartTime(slotNumber);
        return startTime != null ? LocalDateTime.of(date, startTime) : null;
    }

    public static LocalDateTime getSlotEndDateTime(LocalDate date, String slotNumber) {
        LocalTime endTime = getSlotEndTime(slotNumber);
        return endTime != null ? LocalDateTime.of(date, endTime) : null;
    }
}