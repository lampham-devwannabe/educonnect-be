package com.sep.educonnect.dto.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    String checkoutUrl;        // URL để redirect user đến trang thanh toán PayOS
    String transactionId;      // Transaction ID của hệ thống
    String paymentLinkId;      // Payment link ID từ PayOS
    BigDecimal amount;
    String status;
}
