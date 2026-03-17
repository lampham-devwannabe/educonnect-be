package com.sep.educonnect.dto.classsession;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassSessionResponse {
    Long id;
    LocalDate sessionDate;
    Integer slotNumber; // 1-10 (ca mấy)
    Integer sessionNumber; // Buổi thứ mấy trong lớp (1, 2, 3...)
    String topic;
    String meetingJoinUrl;
    String meetingStartUrl;
    String meetingPassword;
    String meetingId;
}
