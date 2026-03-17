package com.sep.educonnect.dto.student;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckInviteRequest {
    String email;
    Long courseId;
}
