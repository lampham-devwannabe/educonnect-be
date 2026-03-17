package com.sep.educonnect.dto.exception.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateExceptionRequest {
    @NotNull(message = "error.session.required")
    Long sessionId;

    @NotBlank(message = "error.reason.required")
    @Size(max = 1000, message = "error.reason.length")
    String reason;
}
