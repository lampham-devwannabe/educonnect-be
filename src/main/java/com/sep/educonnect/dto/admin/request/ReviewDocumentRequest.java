package com.sep.educonnect.dto.admin.request;

import com.sep.educonnect.enums.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewDocumentRequest {
    @NotNull(message = "Document status is required")
    @Schema(description = "Status to set for the document", example = "APPROVED")
    DocumentStatus status;

    @Schema(description = "Reason for rejection (required if status is REJECTED)",
            example = "Image quality is too low, text is not readable")
    String rejectionReason;

    @Schema(description = "Specific issues found in the document")
    List<String> issues;

    @Schema(description = "Internal notes about the document review")
    String internalNotes;
}
