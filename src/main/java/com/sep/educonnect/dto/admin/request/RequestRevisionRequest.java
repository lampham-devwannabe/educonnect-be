package com.sep.educonnect.dto.admin.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestRevisionRequest {
    @NotBlank(message = "Revision notes are required")
    @Size(min = 10, max = 1000, message = "Revision notes must be between 10 and 1000 characters")
    @Schema(description = "Detailed notes about what needs to be revised",
            example = "Please update your bio to include more details about your teaching experience")
    String revisionNotes;

}
