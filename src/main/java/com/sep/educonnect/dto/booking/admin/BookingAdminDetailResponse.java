package com.sep.educonnect.dto.booking.admin;

import com.sep.educonnect.dto.booking.BookingResponse;
import com.sep.educonnect.dto.booking.BookingResponse.MemberInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingAdminDetailResponse {
    BookingResponse booking;

    String bookingOwnerUserId;
    MemberInfo bookingOwner;

    Integer memberCount;
    Integer waitingMemberCount;
    Integer approvedMemberCount;
    Integer rejectedMemberCount;

    List<MemberInfo> rejectedMembers;

    BigDecimal totalAmount;
    Boolean isPaid;

    List<TransactionSummary> transactions;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String lastModifiedBy;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TransactionSummary {
        String reference;
        String gateway;
        String status;
        BigDecimal amount;
        Instant occurredAt;
    }
}

