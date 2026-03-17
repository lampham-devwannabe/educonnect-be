package com.sep.educonnect.dto.exception.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateExceptionRequest {
    @Size(max = 1000, message = "error.reason.length")
    String reason;
}
