package com.sep.educonnect.dto.tutor.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleChangeResponse {
    Long id;
    Long sessionId;
    String sessionInfo; // Thông tin session (class name, time, etc.)
    LocalDate oldDate;
    LocalDate newDate;
    Integer newSlot;
    String content;
    String status; // PENDING, APPROVED, REJECTED, CANCELLED
    String tutorId;
    String tutorName;
    String tutorEmail;
    String className;
}
