package com.sep.educonnect.dto.student;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class StudentGeneralResponse {
    String userId;
    String name;
    String email;
}
