package com.sep.educonnect.dto.comment;

import com.sep.educonnect.enums.CommentType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCommentResponse {
    Long commentId;
    Long processId;
    Long documentId;
    CommentType commentType;
    String comment;
    Boolean isVisibleToTutor;
    LocalDateTime createdAt;
    String createdBy;
}
