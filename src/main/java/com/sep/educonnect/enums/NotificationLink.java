package com.sep.educonnect.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationLink {
    STUDENT_BOOKING_INVITATION("/app/student/update-invite"),
    TUTOR_BOOKING("/bookings"),
    SESSION_ATTENDANCE("/sessions"),
    TUTOR_SCHEDULE("/dashboard/schedule"),
    VERIFICATION_PROCESS("/app/tutor/verification"),
    SYSTEM_ANNOUNCEMENT("/app/announcements");

    private final String prefix;
}

