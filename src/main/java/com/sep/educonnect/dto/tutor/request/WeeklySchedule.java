package com.sep.educonnect.dto.tutor.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklySchedule {
    Integer dayOfWeek;  // 0=Sunday, 1=Monday, 2=Tuesday... 6=Saturday
    List<Integer> slotNumbers;
}
