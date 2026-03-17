package com.sep.educonnect.dto.tutorclass;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceSummary {
    Integer totalSessions;
    Integer averagePresent;
    Integer averageAbsent;
}
