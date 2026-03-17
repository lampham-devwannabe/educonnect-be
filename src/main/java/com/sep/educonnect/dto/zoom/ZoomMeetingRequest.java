package com.sep.educonnect.dto.zoom;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ZoomMeetingRequest {
    Long sessionId;
    String topic;
    String startTime; // ISO-8601 format, e.g. 2025-10-16T09:00:00Z
    int duration;     // minutes
    String password;
}
