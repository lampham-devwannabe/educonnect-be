package com.sep.educonnect.dto.tutor.request;

import com.sep.educonnect.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewDocumentRequest {
    @NotNull(message = "Status is required")
    DocumentStatus status;

    String rejectionReason;
}
