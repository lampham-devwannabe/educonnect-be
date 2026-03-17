package com.sep.educonnect.dto.admin.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproveProfileRequest {
    @Schema(description = "Optional approval comments/notes", example = "All documents verified successfully")
    String comments;

}
