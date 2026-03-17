package com.sep.educonnect.dto.exception.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproveExceptionRequest {
    @NotNull
    Long exceptionId;

    @NotNull(message = "error.decision.required")
    Boolean approved;

    @Size(max = 500)
    String rejectionReason;
}
