package com.sep.educonnect.dto.notification.request;

import com.sep.educonnect.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationUpdateRequest {
    private String message;
    private NotificationType type;
    private Boolean read;
    private String imageUrl;
    private String actionLink;
}

