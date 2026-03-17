# Notification Frontend Implementation Guide

## Overview
Real-time notifications are delivered via WebSocket using STOMP protocol. Notifications are sent to user-specific queues.

## WebSocket Connection

### Endpoint
```
ws://your-domain/ws/notifications
```

### Connection Setup (using SockJS + STOMP.js)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const socket = new SockJS('http://your-domain/ws/notifications');
const stompClient = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});

// Connect with JWT token
stompClient.configure({
  connectHeaders: {
    Authorization: `Bearer ${yourJwtToken}`
  }
});

stompClient.activate();
```

### Subscribe to Notifications

```javascript
stompClient.onConnect = () => {
  // Subscribe to user-specific notification queue
  stompClient.subscribe('/user/queue/notifications', (message) => {
    const notification = JSON.parse(message.body);
    handleNotification(notification);
  });
};
```

## Notification Object Structure

```typescript
interface NotificationResponse {
  id: number;
  title?: string;           // Optional, may be null
  message: string;          // Required
  type: 'TYPICAL' | 'BOOKING_INVITE';
  isRead: boolean;
  timestamp: string;        // ISO 8601 format
  imageUrl?: string;        // Optional
  actionLink?: string;      // Optional - frontend route
}
```

## Notification Types

### TYPICAL
Standard notification. Display normally.

### BOOKING_INVITE
Special handling for booking invitations:
- Show accept/reject buttons
- Navigate to actionLink when clicked
- Update to TYPICAL after user responds

## Action Links

Action links are relative paths that your frontend should handle:

| Link Pattern | Route |
|------------|-------|
| `/app/student/update-invite/{bookingId}` | Booking invitation page |
| `/app/tutor/bookings` | Tutor bookings page |
| `/app/sessions/{sessionId}` | Session attendance page |
| `/app/schedule/changes/{changeId}` | Schedule change details |
| `/app/tutor/verification` | Verification process page |
| `/app/announcements` | Announcements page |

**Note:** Append IDs manually (e.g., `/app/sessions/123`)

## REST API Endpoints

### Get Notifications (Paginated)
```
GET /api/notifications?page=0&size=10
```

### Mark as Read
```
PUT /api/notifications/{notificationId}/read
```

### Mark All as Read
```
PUT /api/notifications/read-all
```

### Update Notification
```
PUT /api/notifications/{notificationId}
Body: {
  "message": "Updated message",
  "type": "TYPICAL",
  "isRead": true,
  "imageUrl": "url",
  "actionLink": "link"
}
```

## Implementation Example

```typescript
// Notification handler
function handleNotification(notification: NotificationResponse) {
  // Show toast/alert
  showToast(notification.message);
  
  // Update notification list
  updateNotificationList(notification);
  
  // Update badge count
  updateUnreadCount();
  
  // Handle action link
  if (notification.actionLink) {
    // Navigate based on actionLink
    navigateToAction(notification.actionLink);
  }
}

// Navigate based on action link
function navigateToAction(actionLink: string) {
  if (actionLink.startsWith('/app/student/update-invite/')) {
    const bookingId = actionLink.split('/').pop();
    router.push(`/bookings/${bookingId}/invite`);
  } else if (actionLink.startsWith('/app/sessions/')) {
    const sessionId = actionLink.split('/').pop();
    router.push(`/sessions/${sessionId}`);
  }
  // ... handle other routes
}
```

## Best Practices

1. **Reconnection**: Implement automatic reconnection on disconnect
2. **Token Refresh**: Update WebSocket connection headers when JWT token refreshes
3. **Badge Count**: Poll `/api/notifications/unread-count` periodically or update on each notification
4. **Error Handling**: Handle connection failures gracefully
5. **Performance**: Debounce notification list updates if receiving many notifications

