package com.sep.educonnect.dto.attendance;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceResponse {
    Long attendanceId;
    Long sessionId;
    Long enrollmentId;
    String studentId;
    String studentName;
    Boolean attended;
    String notes;
}
