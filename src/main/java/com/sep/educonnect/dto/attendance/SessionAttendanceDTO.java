package com.sep.educonnect.dto.attendance;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionAttendanceDTO {
    Long id;
    Boolean attended;
    String notes;

    // Thông tin session
    Long sessionId;
    LocalDate sessionDate;
    Integer sessionNumber;
    String topic;
    LocalDateTime startTime;
    LocalDateTime endTime;

    // Thông tin enrollment
    Long enrollmentId;
    String studentId;
    String studentName;

    // Thông tin class
    Long classId;
    String className;
}
