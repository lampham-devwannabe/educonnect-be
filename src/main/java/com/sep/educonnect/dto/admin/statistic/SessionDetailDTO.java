package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SessionDetailDTO {
    Long sessionId;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Double durationHours;
    String topic;
}