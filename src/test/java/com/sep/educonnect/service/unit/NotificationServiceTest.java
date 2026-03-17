package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.notification.NotificationResponse;
import com.sep.educonnect.dto.notification.request.NotificationUpdateRequest;
import com.sep.educonnect.entity.Notification;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.enums.NotificationType;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.NotificationMapper;
import com.sep.educonnect.repository.NotificationRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should create and send notification successfully")
    void should_createAndSendNotification_successfully() {
        // Given
        String userId = "user-1";
        String message = "Test message";
        NotificationType type = NotificationType.TYPICAL;
        String imageUrl = "http://image.url";
        String actionLink = "/action";

        User user = User.builder().userId(userId).build();
        Notification notification = Notification.builder()
                .id(1L)
                .user(user)
                .message(message)
                .type(type)
                .build();
        NotificationResponse response = new NotificationResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toNotificationResponse(notification)).thenReturn(response);

        // When
        Notification result = notificationService.createAndSendNotification(userId, message, type, imageUrl,
                actionLink);

        // Then
        assertNotNull(result);
        assertEquals(notification, result);
        verify(userRepository).findById(userId);
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/notifications"), eq(response));
    }

    @Test
    @DisplayName("Should throw exception when user not found in createAndSendNotification")
    void should_throwException_whenUserNotFound_inCreateAndSendNotification() {
        // Given
        String userId = "non-existent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> notificationService.createAndSendNotification(userId, "msg",
                NotificationType.TYPICAL, null, null));
    }

    @Test
    @DisplayName("Should create and send notifications for multiple users")
    void should_createAndSendNotifications_successfully() {
        // Given
        List<String> userIds = List.of("user-1", "user-2");
        User user1 = User.builder().userId("user-1").build();
        User user2 = User.builder().userId("user-2").build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(user2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        notificationService.createAndSendNotifications(userIds, "msg", NotificationType.TYPICAL, null, null);

        // Then
        verify(userRepository, times(2)).findById(anyString());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle exception for individual user in batch notification")
    void should_handleException_inBatchNotification() {
        // Given
        List<String> userIds = List.of("user-1", "user-2");
        User user1 = User.builder().userId("user-1").build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user-2")).thenReturn(Optional.empty()); // Will throw exception
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        notificationService.createAndSendNotifications(userIds, "msg", NotificationType.TYPICAL, null, null);

        // Then
        verify(userRepository).findById("user-1");
        verify(userRepository).findById("user-2");
        verify(notificationRepository, times(1)).save(any(Notification.class)); // Only 1 saved
    }

    @Test
    @DisplayName("Should mark notification as read successfully")
    void should_markAsRead_successfully() {
        // Given
        Long notificationId = 1L;
        String userId = "user-1";
        User user = User.builder().userId(userId).build();
        Notification notification = Notification.builder().id(notificationId).user(user).read(false).build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When
        notificationService.markAsRead(notificationId, userId);

        // Then
        assertTrue(notification.getRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should throw exception when notification not found in markAsRead")
    void should_throwException_whenNotificationNotFound_inMarkAsRead() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> notificationService.markAsRead(1L, "user-1"));
        assertEquals(ErrorCode.NOTIFICATION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when unauthorized in markAsRead")
    void should_throwException_whenUnauthorized_inMarkAsRead() {
        // Given
        Long notificationId = 1L;
        String userId = "user-1";
        User otherUser = User.builder().userId("other-user").build();
        Notification notification = Notification.builder().id(notificationId).user(otherUser).build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> notificationService.markAsRead(notificationId, userId));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void should_markAllAsRead_successfully() {
        // Given
        String userId = "user-1";
        Notification n1 = Notification.builder().read(false).build();
        Notification n2 = Notification.builder().read(false).build();
        List<Notification> notifications = List.of(n1, n2);

        when(notificationRepository.findUnreadByUserId(userId)).thenReturn(notifications);

        // When
        notificationService.markAllAsRead(userId);

        // Then
        assertTrue(n1.getRead());
        assertTrue(n2.getRead());
        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    @DisplayName("Should update notification successfully")
    void should_updateNotification_successfully() {
        // Given
        Long notificationId = 1L;
        String userId = "user-1";
        User user = User.builder().userId(userId).build();
        Notification notification = Notification.builder().id(notificationId).user(user).build();
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        NotificationResponse response = new NotificationResponse();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toNotificationResponse(notification)).thenReturn(response);

        // When
        NotificationResponse result = notificationService.updateNotification(notificationId, userId, request);

        // Then
        assertNotNull(result);
        verify(notificationMapper).updateNotification(notification, request);
        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/notifications"), eq(response));
    }

    @Test
    @DisplayName("Should throw AppException when notification not found in updateNotification")
    void should_throwException_whenNotificationNotFound_inUpdateNotification() {
        // Given
        Long notificationId = 999L;
        String userId = "user-1";
        NotificationUpdateRequest request = new NotificationUpdateRequest();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            notificationService.updateNotification(notificationId, userId, request);
        });

        assertEquals(ErrorCode.NOTIFICATION_NOT_EXISTED, exception.getErrorCode());
        verify(notificationRepository).findById(notificationId);
        verify(notificationMapper, never()).updateNotification(any(), any());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw AppException when unauthorized in updateNotification")
    void should_throwException_whenUnauthorized_inUpdateNotification() {
        // Given
        Long notificationId = 1L;
        String userId = "user-1";
        User otherUser = User.builder().userId("other-user").build();
        Notification notification = Notification.builder().id(notificationId).user(otherUser).build();
        NotificationUpdateRequest request = new NotificationUpdateRequest();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            notificationService.updateNotification(notificationId, userId, request);
        });

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(notificationRepository).findById(notificationId);
        verify(notificationMapper, never()).updateNotification(any(), any());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should get user notifications with pagination")
    void should_getUserNotifications_successfully() {
        // Given
        String userId = "user-1";
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = new Notification();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        NotificationResponse response = new NotificationResponse();

        when(notificationRepository.findByUserIdOrderByTimestampDesc(userId, pageable)).thenReturn(page);
        when(notificationMapper.toNotificationResponse(notification)).thenReturn(response);

        // When
        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(notificationRepository).findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Test
    @DisplayName("Should update notification by action link pattern")
    void should_updateNotificationByActionLink_successfully() {
        // Given
        String userId = "user-1";
        String pattern = "%/booking/123%";
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        Notification notification = Notification.builder().id(1L).build();
        NotificationResponse response = new NotificationResponse();

        when(notificationRepository.findByUserIdAndActionLinkPattern(userId, pattern))
                .thenReturn(Collections.singletonList(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toNotificationResponse(notification)).thenReturn(response);

        // When
        notificationService.updateNotificationByActionLink(userId, pattern, request);

        // Then
        verify(notificationMapper).updateNotification(notification, request);
        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/notifications"), eq(response));
    }

    @Test
    @DisplayName("Should count unread notifications")
    void should_countUnreadNotifications() {
        // Given
        String userId = "user-1";
        Long count = 5L;
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(count);

        // When
        Long result = notificationService.countUnreadNotifications(userId);

        // Then
        assertEquals(count, result);
        verify(notificationRepository).countUnreadByUserId(userId);
    }
}
