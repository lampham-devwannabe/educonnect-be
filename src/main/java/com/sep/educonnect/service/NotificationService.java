package com.sep.educonnect.service;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    /**
     * Creates a notification and sends it via WebSocket to the user
     */
    @Transactional
    public Notification createAndSendNotification(
            String userId,
            String message,
            NotificationType type,
            String imageUrl,
            String actionLink
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .read(false)
                .timestamp(LocalDateTime.now())
                .imageUrl(imageUrl)
                .actionLink(actionLink)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Send notification via WebSocket to the specific user
        NotificationResponse notificationResponse = notificationMapper.toNotificationResponse(savedNotification);
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notificationResponse
        );

        log.info("Notification sent to user {} via WebSocket", userId);
        return savedNotification;
    }

    /**
     * Creates notifications for multiple users
     */
    @Transactional
    public void createAndSendNotifications(
            List<String> userIds,
            String message,
            NotificationType type,
            String imageUrl,
            String actionLink
    ) {
        for (String userId : userIds) {
            try {
                createAndSendNotification(userId, message, type, imageUrl, actionLink);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * Marks a notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        unreadNotifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Updates a notification
     */
    @Transactional
    public NotificationResponse updateNotification(Long notificationId, String userId, NotificationUpdateRequest request) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notificationMapper.updateNotification(notification, request);
        Notification updatedNotification = notificationRepository.save(notification);

        // Send updated notification via WebSocket
        NotificationResponse notificationResponse = notificationMapper.toNotificationResponse(updatedNotification);
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notificationResponse
        );

        log.info("Notification {} updated for user {}", notificationId, userId);
        return notificationResponse;
    }

    /**
     * Gets paginated notifications for a user
     */
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        return notifications.map(notificationMapper::toNotificationResponse);
    }

    /**
     * Finds and updates a notification by actionLink pattern for a user.
     * Useful for updating booking invite notifications when they accept/reject.
     *
     * @param userId            The user ID
     * @param actionLinkPattern The action link pattern to match (should include SQL wildcard %, e.g., "/app/booking/123%")
     * @param request           The update request with new notification data
     */
    @Transactional
    public void updateNotificationByActionLink(String userId, String actionLinkPattern, NotificationUpdateRequest request) {
        List<Notification> notifications = notificationRepository.findByUserIdAndActionLinkPattern(userId, actionLinkPattern);
        if (!notifications.isEmpty()) {
            // Update the most recent one
            Notification notification = notifications.get(0);
            notificationMapper.updateNotification(notification, request);
            Notification updatedNotification = notificationRepository.save(notification);

            // Send updated notification via WebSocket
            NotificationResponse notificationResponse = notificationMapper.toNotificationResponse(updatedNotification);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    notificationResponse
            );

            log.info("Notification {} updated for user {} by actionLink pattern", notification.getId(), userId);
        }
    }

    public Long countUnreadNotifications(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
}

