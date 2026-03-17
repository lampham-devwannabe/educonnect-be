package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.payment.PaymentRequest;
import com.sep.educonnect.dto.payment.PaymentResponse;
import com.sep.educonnect.dto.transaction.TransactionSummaryResponse;
import com.sep.educonnect.enums.PaymentStatus;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.PaymentService;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentController {
    PaymentService paymentService;
    I18nService i18nService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TUTOR','STUDENT')")
    public ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ApiResponse.<PaymentResponse>builder().result(response).build();
    }

    @PostMapping("/cancel/{orderCode}")
    public ApiResponse<String> cancelPayment(@PathVariable Long orderCode) {
        paymentService.cancelPayment(orderCode);
        return ApiResponse.<String>builder().result(i18nService.msg("msg.payment.cancel")).build();
    }

    @PostMapping("/return")
    public ApiResponse<PaymentResponse> handlePaymentReturn(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Boolean cancel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderCode) {
        log.info(
                "Payment return callback - code: {}, id: {}, cancel: {}, status: {}, orderCode: {}",
                code,
                id,
                cancel,
                status,
                orderCode);
        PaymentResponse response = paymentService.handlePaymentReturn(orderCode, cancel);
        return ApiResponse.<PaymentResponse>builder().result(response).build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TUTOR','STUDENT')")
    public ApiResponse<Page<TransactionSummaryResponse>> getPaymentsByTutor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long bookingId) {

        Page<TransactionSummaryResponse> txs =
                paymentService.getAllTransactions(page, size, amount, status, bookingId);

        return ApiResponse.<Page<TransactionSummaryResponse>>builder().result(txs).build();
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('TUTOR','STUDENT')")
    public ApiResponse<PaymentResponse> getPaymentDetail(@PathVariable String transactionId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.getTransactionDetail(transactionId))
                .build();
    }
}
