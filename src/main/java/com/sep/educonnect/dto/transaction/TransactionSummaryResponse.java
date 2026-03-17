package com.sep.educonnect.dto.transaction;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionSummaryResponse {
    Long id;
    String transactionId;
    BigDecimal amount;
    String status;
    LocalDateTime paymentDate;
    Long bookingId;
}
