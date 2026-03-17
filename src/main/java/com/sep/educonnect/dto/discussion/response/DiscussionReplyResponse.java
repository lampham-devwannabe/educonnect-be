package com.sep.educonnect.dto.discussion.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscussionReplyResponse {
    Long id;
    Long discussionId;
    String content;
    String username;
    LocalDateTime createdAt;
}