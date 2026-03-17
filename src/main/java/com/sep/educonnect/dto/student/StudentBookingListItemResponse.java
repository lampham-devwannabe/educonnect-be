package com.sep.educonnect.dto.student;

import com.sep.educonnect.enums.BookingMemberStatus;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.GroupType;
import com.sep.educonnect.enums.RegistrationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentBookingListItemResponse {
    Long id;
    BookingStatus bookingStatus;
    BookingMemberStatus myStatus; // The student's status in this booking
    RegistrationType registrationType;
    GroupType groupType;
    BigDecimal amount;
    String courseName;
    String scheduleDescription;
    Integer currentMemberCount;
    Boolean isPaymentRequired; // True if status is APPROVED and user is OWNER
    Boolean hasPaid; // True if status is PAID
    LocalDateTime bookingDate;
}

