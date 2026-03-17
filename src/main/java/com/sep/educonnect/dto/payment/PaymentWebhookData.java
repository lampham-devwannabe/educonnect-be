package com.sep.educonnect.dto.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentWebhookData {
    String orderCode;          // Mã đơn hàng từ PayOS
    BigDecimal amount;
    String description;
    String accountNumber;
    String reference;
    String transactionDateTime;
    String virtualAccountName;
    String virtualAccountNumber;
    String counterAccountBankId;
    String counterAccountBankName;
    String counterAccountName;
    String counterAccountNumber;
}
