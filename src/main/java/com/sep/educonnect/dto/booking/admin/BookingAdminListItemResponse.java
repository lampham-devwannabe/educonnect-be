package com.sep.educonnect.dto.booking.admin;

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
public class BookingAdminListItemResponse {
    Long id;
    BookingStatus bookingStatus;
    RegistrationType registrationType;
    GroupType groupType;
    BigDecimal totalAmount;
    String scheduleDescription;
    String courseName;

    Integer memberCount;
    Integer pendingMemberCount;

    Boolean hasUnpaidTransactions;
    LocalDateTime createdAt;
    String createdBy;
}

