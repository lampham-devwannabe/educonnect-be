package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tutor_availability")
public class TutorAvailability extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    User user;

    @Column(name = "is_work_on_monday")
    Boolean isWorkOnMonday = false;

    @Column(name = "monday_slots", length = 500)
    String mondaySlots;

    @Column(name = "is_work_on_tuesday")
    Boolean isWorkOnTuesday = false;

    @Column(name = "tuesday_slots", length = 500)
    String tuesdaySlots;

    @Column(name = "is_work_on_wednesday")
    Boolean isWorkOnWednesday = false;

    @Column(name = "wednesday_slots", length = 500)
    String wednesdaySlots;

    @Column(name = "is_work_on_thursday")
    Boolean isWorkOnThursday = false;

    @Column(name = "thursday_slots", length = 500)
    String thursdaySlots;

    @Column(name = "is_work_on_friday")
    Boolean isWorkOnFriday = false;

    @Column(name = "friday_slots", length = 500)
    String fridaySlots;

    @Column(name = "is_work_on_saturday")
    Boolean isWorkOnSaturday = false;

    @Column(name = "saturday_slots", length = 500)
    String saturdaySlots;

    @Column(name = "is_work_on_sunday")
    Boolean isWorkOnSunday = false;

    @Column(name = "sunday_slots", length = 500)
    String sundaySlots;

    public List<Integer> getSlotsByDay(int dayOfWeek) {
        String slots = switch (dayOfWeek) {
            case 1 -> mondaySlots;
            case 2 -> tuesdaySlots;
            case 3 -> wednesdaySlots;
            case 4 -> thursdaySlots;
            case 5 -> fridaySlots;
            case 6 -> saturdaySlots;
            case 0 -> sundaySlots;
            default -> null;
        };

        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(slots.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void setSlotsByDay(int dayOfWeek, List<Integer> slotIds) {
        String slotsStr = slotIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        switch (dayOfWeek) {
            case 1 -> {
                mondaySlots = slotsStr;
                isWorkOnMonday = !slotIds.isEmpty();
            }
            case 2 -> {
                tuesdaySlots = slotsStr;
                isWorkOnTuesday = !slotIds.isEmpty();
            }
            case 3 -> {
                wednesdaySlots = slotsStr;
                isWorkOnWednesday = !slotIds.isEmpty();
            }
            case 4 -> {
                thursdaySlots = slotsStr;
                isWorkOnThursday = !slotIds.isEmpty();
            }
            case 5 -> {
                fridaySlots = slotsStr;
                isWorkOnFriday = !slotIds.isEmpty();
            }
            case 6 -> {
                saturdaySlots = slotsStr;
                isWorkOnSaturday = !slotIds.isEmpty();
            }
            case 0 -> {
                sundaySlots = slotsStr;
                isWorkOnSunday = !slotIds.isEmpty();
            }
        }
    }

    public boolean isWorkOnDay(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> Boolean.TRUE.equals(isWorkOnMonday);
            case 2 -> Boolean.TRUE.equals(isWorkOnTuesday);
            case 3 -> Boolean.TRUE.equals(isWorkOnWednesday);
            case 4 -> Boolean.TRUE.equals(isWorkOnThursday);
            case 5 -> Boolean.TRUE.equals(isWorkOnFriday);
            case 6 -> Boolean.TRUE.equals(isWorkOnSaturday);
            case 0 -> Boolean.TRUE.equals(isWorkOnSunday);
            default -> false;
        };
    }
}
