package com.sep.educonnect.entity;

import com.sep.educonnect.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "transaction")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    Booking booking;

    @Column(name = "payment_date", nullable = false)
    LocalDateTime paymentDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    BigDecimal amount;

    @Column(name = "transaction_id", unique = true, nullable = false)
    String transactionId;

    @Column(name = "payos_order_code")
    Long payosOrderCode;  // Mã đơn hàng từ PayOS

    @Column(name = "payos_payment_link_id")
    String payosPaymentLinkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    PaymentStatus status;
}
