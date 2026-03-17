package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.notification.NotificationResponse;
import com.sep.educonnect.dto.notification.request.NotificationUpdateRequest;
import com.sep.educonnect.entity.Notification;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toNotificationResponse(Notification notification);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "message", source = "request.message")
    @Mapping(target = "type", source = "request.type")
    @Mapping(target = "read", source = "request.read")
    @Mapping(target = "imageUrl", source = "request.imageUrl")
    @Mapping(target = "actionLink", source = "request.actionLink")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    void updateNotification(@MappingTarget Notification notification, NotificationUpdateRequest request);
}

