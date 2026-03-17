package com.sep.educonnect.dto.admin.request;

import com.sep.educonnect.enums.CommentType;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class AddCommentRequest {
    @NotBlank(message = "Comment text is required")
    @Size(min = 5, max = 2000, message = "Comment must be between 5 and 2000 characters")
    @Schema(description = "Comment content", example = "Documents look good, proceeding to next stage")
    String comment;

    @NotNull(message = "Comment type is required")
    @Schema(description = "Type of comment", example = "GENERAL")
    CommentType commentType;

    @Schema(description = "Whether this comment should be visible to the tutor", example = "false")
    boolean visibleToTutor = false;

    @Schema(description = "Document ID if comment is about specific document")
    Long documentId;
}
