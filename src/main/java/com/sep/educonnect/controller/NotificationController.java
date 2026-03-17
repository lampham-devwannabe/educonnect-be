package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.notification.NotificationResponse;
import com.sep.educonnect.dto.notification.request.NotificationUpdateRequest;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {
    NotificationService notificationService;
    UserRepository userRepository;

    /**
     * Get current authenticated user's ID
     */
    private String getCurrentUserId() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndNotDeleted(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED))
                .getUserId();
    }

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(notificationService.getUserNotifications(userId, pageable))
                .build();
    }

    @PutMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> updateNotification(
            @PathVariable Long notificationId,
            @RequestBody @Valid NotificationUpdateRequest request) {
        String userId = getCurrentUserId();
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.updateNotification(notificationId, userId, request))
                .build();
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        String userId = getCurrentUserId();
        notificationService.markAsRead(notificationId, userId);
        return ApiResponse.<Void>builder().build();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        String userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadNotificationCount() {
        String userId = getCurrentUserId();
        Long count = notificationService.countUnreadNotifications(userId);
        return ApiResponse.<Long>builder()
                .result(count)
                .build();
    }
}

