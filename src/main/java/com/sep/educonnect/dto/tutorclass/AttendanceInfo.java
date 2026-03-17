package com.sep.educonnect.dto.tutorclass;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceInfo {
    Integer totalSessions;
    Integer presentCount;
    Integer absentCount;
    Double attendanceRate;
}
