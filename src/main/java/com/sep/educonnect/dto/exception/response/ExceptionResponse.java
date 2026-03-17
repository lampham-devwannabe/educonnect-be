package com.sep.educonnect.dto.exception.response;


import com.sep.educonnect.enums.ExceptionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExceptionResponse {

     Long id;
     Long sessionId;
     LocalDate sessionDate;
     LocalDateTime sessionStartTime;
     LocalDateTime sessionEndTime;
     String sessionTopic;
     Integer sessionNumber;
     String className;
     String reason;
     ExceptionStatus status;
     Boolean isApproved;
     String rejectionReason;
     String approvedByName;
     LocalDateTime approvedAt;
     LocalDateTime createdAt;

    // Computed fields
     String dayOfWeek;
     Long daysUntilSession;
     Boolean canModify;

}
