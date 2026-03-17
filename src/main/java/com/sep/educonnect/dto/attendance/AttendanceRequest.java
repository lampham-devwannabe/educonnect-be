package com.sep.educonnect.dto.attendance;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceRequest {
    Long sessionId;
    Long enrollmentId;
    Boolean attended;
    String notes;
}
