package com.sep.educonnect.dto.discussion.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscussionResponse {
    Long id;
    Long lessonId;
    String content;
    String username;
    LocalDateTime createdAt;
    Long replyCount;
}
