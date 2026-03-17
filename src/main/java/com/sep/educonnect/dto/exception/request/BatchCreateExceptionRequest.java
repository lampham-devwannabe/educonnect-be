package com.sep.educonnect.dto.exception.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchCreateExceptionRequest {
    @NotEmpty(message = "error.session.required")
    List<Long> sessionIds; // Nghỉ nhiều buổi học

    @NotBlank(message = "error.reason.required")
    @Size(max = 1000, message = "error.reason.length")
    String reason;
}
