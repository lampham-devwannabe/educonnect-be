# Notification Backend Integration Guide

## Overview
This guide shows how to integrate notifications into your services. Notifications are automatically sent via WebSocket when created.

## Basic Setup

### 1. Inject NotificationService

```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final NotificationService notificationService;
    // ... other dependencies
}
```

### 2. Import Required Classes

```java
import com.sep.educonnect.service.NotificationService;
import com.sep.educonnect.enums.NotificationType;
import com.sep.educonnect.enums.NotificationLink;
```

## Creating Notifications

### Single User Notification

```java
notificationService.createAndSendNotification(
    userId,                    // String: User ID
    "Your message here",       // String: Notification message
    NotificationType.TYPICAL,  // NotificationType: TYPICAL or BOOKING_INVITE
    null,                      // String: Image URL (optional)
    null                       // String: Action link (optional)
);
```

### Multiple Users Notification

```java
List<String> userIds = Arrays.asList("user1", "user2", "user3");
notificationService.createAndSendNotifications(
    userIds,
    "Broadcast message",
    NotificationType.TYPICAL,
    null,
    null
);
```

## Common Integration Patterns

### Pattern 1: Booking Creation

```java
@Transactional
public BookingResponse createBooking(CreateBookingRequest request) {
    // ... booking creation logic
    Booking savedBooking = bookingRepository.save(booking);
    
    // Notify owner
    notificationService.createAndSendNotification(
        ownerId,
        "You have booked " + tutorName + " for course " + courseName,
        NotificationType.TYPICAL,
        null,
        null
    );
    
    // Notify invited members
    String actionLink = NotificationLink.STUDENT_BOOKING_INVITATION.getPrefix() + "/" + savedBooking.getId();
    for (String memberId : memberIds) {
        if (!memberId.equals(ownerId)) {
            notificationService.createAndSendNotification(
                memberId,
                ownerName + " has invited you to study with " + tutorName,
                NotificationType.BOOKING_INVITE,
                null,
                actionLink
            );
        }
    }
    
    return toBookingResponse(savedBooking);
}
```

### Pattern 2: Status Change Notification

```java
@Transactional
public void approveBooking(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    
    booking.setBookingStatus(BookingStatus.APPROVED);
    bookingRepository.save(booking);
    
    // Notify all members
    for (BookingMember member : booking.getBookingMembers()) {
        if (member.getStatus() != BookingMemberStatus.REJECTED) {
            notificationService.createAndSendNotification(
                member.getUserId(),
                "Your booking for course " + courseName + " has been approved",
                NotificationType.TYPICAL,
                null,
                null
            );
        }
    }
}
```

### Pattern 3: Update Existing Notification

```java
@Transactional
public void updateOrRejectInvite(Long bookingId, boolean accept) {
    // ... business logic
    
    // Update notification from BOOKING_INVITE to TYPICAL
    String actionLinkPattern = NotificationLink.STUDENT_BOOKING_INVITATION.getPrefix() + "/" + bookingId + "%";
    NotificationUpdateRequest updateRequest = NotificationUpdateRequest.builder()
        .type(NotificationType.TYPICAL)
        .actionLink(null)
        .message(accept ? "You have accepted the booking invitation" : "You have rejected the booking invitation")
        .build();
    
    notificationService.updateNotificationByActionLink(
        userId,
        actionLinkPattern,
        updateRequest
    );
}
```

### Pattern 4: Action Link with ID

```java
// For session attendance
String actionLink = NotificationLink.SESSION_ATTENDANCE.getPrefix() + "/" + sessionId;
notificationService.createAndSendNotification(
    studentId,
    "Attendance has been recorded for " + sessionTitle + " session",
    NotificationType.TYPICAL,
    null,
    actionLink
);

// For schedule changes
String actionLink = NotificationLink.SCHEDULE_CHANGE.getPrefix() + "/" + changeId;
notificationService.createAndSendNotification(
    userId,
    "Schedule change request has been processed",
    NotificationType.TYPICAL,
    null,
    actionLink
);
```

## Notification Types

### TYPICAL
Use for:
- Status updates
- Confirmations
- General announcements
- Completed actions

### BOOKING_INVITE
Use for:
- Booking invitations
- Requires user action (accept/reject)
- Should be updated to TYPICAL after user responds

## Available Notification Links

```java
NotificationLink.STUDENT_BOOKING_INVITATION  // /app/student/update-invite
NotificationLink.TUTOR_BOOKING               // /app/tutor/bookings
NotificationLink.SESSION_ATTENDANCE          // /app/sessions
NotificationLink.SCHEDULE_CHANGE             // /app/schedule/changes
NotificationLink.VERIFICATION_PROCESS       // /app/tutor/verification
NotificationLink.SYSTEM_ANNOUNCEMENT         // /app/announcements
```

## Message Guidelines

- Keep messages concise and natural
- Include relevant context (names, course titles, etc.)
- Use present tense for actions
- Examples:
  - ✅ "You have booked John Doe for course Mathematics"
  - ✅ "Your booking for course Mathematics has been approved"
  - ❌ "Booking approval notification"
  - ❌ "The system has processed your request"

## Error Handling

Notifications are sent asynchronously. If notification creation fails, it won't affect the main business logic:

```java
try {
    notificationService.createAndSendNotification(...);
} catch (Exception e) {
    log.error("Failed to send notification: {}", e.getMessage());
    // Continue with business logic
}
```

## Testing

When testing, notifications will be:
1. Saved to database
2. Sent via WebSocket (if connected)
3. Available via REST API (`GET /api/notifications`)

## Complete Example: Exception Approval

```java
@Transactional
public ExceptionResponse approveException(ApproveExceptionRequest request) {
    TutorAvailabilityException exception = exceptionRepository.findById(...)
        .orElseThrow(...);
    
    // Update exception status
    exception.setStatus(request.getApproved() ? APPROVED : REJECTED);
    exception = exceptionRepository.save(exception);
    
    // Notify tutor
    String tutorId = exception.getTutorProfile().getUser().getUserId();
    String actionLink = NotificationLink.SCHEDULE_CHANGE.getPrefix() + "/" + exception.getId();
    String tutorMessage = request.getApproved()
        ? "Your exception request has been approved"
        : "Your exception request has been rejected: " + request.getRejectionReason();
    
    notificationService.createAndSendNotification(
        tutorId,
        tutorMessage,
        NotificationType.TYPICAL,
        null,
        actionLink
    );
    
    // Notify students
    List<ClassEnrollment> enrollments = classEnrollmentRepository.findByTutorClassId(...);
    String studentMessage = request.getApproved()
        ? "Session has been cancelled by the tutor"
        : "The tutor's exception request has been rejected";
    
    for (ClassEnrollment enrollment : enrollments) {
        notificationService.createAndSendNotification(
            enrollment.getStudent().getUserId(),
            studentMessage,
            NotificationType.TYPICAL,
            null,
            actionLink
        );
    }
    
    return exceptionMapper.toResponse(exception);
}
```

