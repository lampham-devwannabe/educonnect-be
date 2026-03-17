package com.sep.educonnect.dto.discussion.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscussionReplyRequest {

    @NotNull
    Long discussionId;

    @NotNull
    @Size(min = 1, max = 5000)
    String content;
}