package com.sep.educonnect.dto.zoom;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ZoomMeetingResponse {
    String meetingId;
    String joinUrl;
    String startUrl;
    String topic;
    String startTime;
    String password;
}
