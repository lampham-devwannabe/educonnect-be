package com.sep.educonnect.service;

import com.sep.educonnect.configuration.PayOSConfig;
import com.sep.educonnect.dto.payment.PaymentRequest;
import com.sep.educonnect.dto.payment.PaymentResponse;
import com.sep.educonnect.dto.transaction.TransactionSummaryResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.*;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {

  TransactionRepository transactionRepository;
  BookingRepository bookingRepository;
  PayOS payOS;
  PayOSConfig payOSConfig;
  UserRepository userRepository;
  NotificationService notificationService;
  TutorClassRepository tutorClassRepository;
  ClassEnrollmentRepository classEnrollmentRepository;
  ProgressService progressService;

  /** Tạo payment link và lưu transaction */
  @Transactional
  public PaymentResponse createPayment(PaymentRequest request) {
    // 1. Validate booking
    Booking booking =
        bookingRepository
            .findById(request.getBookingId())
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

    if (booking.getBookingStatus() == BookingStatus.PAID) {
      throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
    }

    if (booking.getBookingStatus() != BookingStatus.APPROVED) {
      throw new AppException(ErrorCode.BOOKING_MUST_BE_APPROVED);
    }

    // 2. Tính amount
    BigDecimal amount = booking.getTotalAmount();

    // 3. Kiểm tra và hủy transaction cũ nếu có
    Transaction existingTransaction =
        transactionRepository
            .findByBookingIdAndStatus(booking.getId(), PaymentStatus.PENDING)
            .orElse(null);

    if (existingTransaction != null) {
      existingTransaction.setStatus(PaymentStatus.CANCELLED);
      cancelPayment(existingTransaction.getPayosOrderCode());
      transactionRepository.save(existingTransaction);
    }

    // 4. Tạo transaction ID unique và orderCode mới
    String transactionId = generateTransactionId();
    Long orderCode = System.currentTimeMillis();

    // 5. Tạo PayOS payment link
    try {
      PaymentLinkItem linkItem =
          PaymentLinkItem.builder()
              .name("Booking #" + booking.getId())
              .quantity(1)
              .price(amount.longValue())
              .build();

      CreatePaymentLinkRequest createPaymentLinkRequest =
          CreatePaymentLinkRequest.builder()
              .orderCode(orderCode)
              .amount(amount.longValue())
              .description("Payment for Booking #" + booking.getId())
              .items(List.of(linkItem))
              .returnUrl(payOSConfig.getReturnUrl())
              .cancelUrl(payOSConfig.getCancelUrl())
              .build();

      CreatePaymentLinkResponse createPaymentResult =
          payOS.paymentRequests().create(createPaymentLinkRequest);

      // 6. Tạo transaction mới
      Transaction transaction =
          Transaction.builder()
              .booking(booking)
              .transactionId(transactionId)
              .amount(amount)
              .payosOrderCode(orderCode)
              .payosPaymentLinkId(createPaymentResult.getPaymentLinkId())
              .paymentDate(LocalDateTime.now())
              .status(PaymentStatus.PENDING)
              .build();

      transactionRepository.save(transaction);

      // 7. Return response
      return PaymentResponse.builder()
          .checkoutUrl(createPaymentResult.getCheckoutUrl())
          .transactionId(transactionId)
          .paymentLinkId(createPaymentResult.getPaymentLinkId())
          .amount(amount)
          .status(PaymentStatus.PENDING.name())
          .build();

    } catch (Exception e) {
      log.error("Error creating PayOS payment link", e);
      throw new AppException(ErrorCode.CREATE_PAYMENT_QR_FAILED);
    }
  }

  @Transactional
  public PaymentResponse handlePaymentReturn(Long orderCode, Boolean cancel) {
    if (orderCode == null) {
      throw new AppException(ErrorCode.INVALID_PAYMENT_INFO);
    }

    Transaction transaction =
        transactionRepository
            .findByPayosOrderCode(orderCode)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

    // Nếu đã PAID rồi thì không cần xử lý nữa
    if (transaction.getStatus() == PaymentStatus.PAID) {
      return buildPaymentResponse(transaction);
    }

    try {
      // Verify lại với PayOS để chắc chắn
      PaymentLink paymentInfo = payOS.paymentRequests().get(orderCode);

      if (paymentInfo == null) {
        throw new AppException(ErrorCode.INVALID_PAYMENT_INFO);
      }

      String payosStatus = paymentInfo.getStatus().toString();

      // Cập nhật status dựa trên response từ PayOS
      if ("PAID".equals(payosStatus)) {
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setPaymentDate(LocalDateTime.now());

        // Cập nhật booking status
        Booking booking = transaction.getBooking();
        booking.setBookingStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        // Reload booking with details (course, bookingMembers, tutor) for notifications
        Booking bookingWithDetails =
            bookingRepository
                .findWithClassDetailsById(booking.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (bookingWithDetails.getCourse().getType() == CourseType.SELF_PACED) {
          // Safety checks: ensure there are booking members and tutor classes
          if (bookingWithDetails.getBookingMembers() == null
              || bookingWithDetails.getBookingMembers().isEmpty()) {
            log.warn(
                "No booking members found for booking {}: skipping enrollment creation",
                bookingWithDetails.getId());
          } else if (bookingWithDetails.getCourse().getTutorClasses() == null
              || bookingWithDetails.getCourse().getTutorClasses().isEmpty()) {
            log.warn(
                "No tutor classes available for course in booking {}: skipping enrollment creation",
                bookingWithDetails.getId());
          } else {
            BookingMember bookingMember =
                bookingWithDetails.getBookingMembers().stream().findFirst().get();
            TutorClass tutorClass = bookingWithDetails.getCourse().getTutorClasses().getFirst();

            ClassEnrollment enrollment =
                ClassEnrollment.builder()
                    .student(
                        userRepository
                            .findById(bookingMember.getUserId())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)))
                    .enrolledAt(LocalDateTime.now())
                    .tutorClass(tutorClass)
                    .build();

            // Persist enrollment first to ensure it has an ID for progress creation
            ClassEnrollment savedEnrollment = classEnrollmentRepository.save(enrollment);

            // Create course progress after enrollment is saved. Wrap in try/catch so
            // notification/payment flow
            // won't be broken by issues in progress creation.
            try {
              progressService.createCourseProgress(savedEnrollment.getId());
            } catch (Exception ex) {
              log.error(
                  "Failed to create course progress for enrollment {}: {}",
                  savedEnrollment.getId(),
                  ex.getMessage());
              // do not rethrow to avoid failing payment processing
            }

            // Keep tutorClass's enrollments consistent with the saved enrollment
            try {
              tutorClass.getEnrollments().add(savedEnrollment);
              tutorClassRepository.save(tutorClass);
            } catch (Exception ex) {
              log.warn(
                  "Failed to attach enrollment {} to tutorClass {}: {}",
                  savedEnrollment.getId(),
                  tutorClass.getId(),
                  ex.getMessage());
            }
          }
        }

        // Send notifications to booking members and tutor
        sendPaymentConfirmationNotifications(bookingWithDetails);

        log.info("Payment completed for orderCode: {}", orderCode);

      } else if ("CANCELLED".equals(payosStatus) || Boolean.TRUE.equals(cancel)) {
        transaction.setStatus(PaymentStatus.CANCELLED);
        log.info("Payment cancelled for orderCode: {}", orderCode);

      } else if ("PENDING".equals(payosStatus)) {
        transaction.setStatus(PaymentStatus.PENDING);
        log.info("Payment still pending for orderCode: {}", orderCode);
      }

      transactionRepository.save(transaction);
      return buildPaymentResponse(transaction);

    } catch (Exception e) {
      log.error("Error handling payment return for orderCode {}: {}", orderCode, e.getMessage());
      throw new AppException(ErrorCode.FAILED_TO_CHECK_STATUS);
    }
  }

  @Transactional
  public void cancelPayment(Long orderCode) {
    if (orderCode == null) {
      throw new AppException(ErrorCode.INVALID_PAYMENT_INFO);
    }

    Transaction transaction =
        transactionRepository
            .findByPayosOrderCode(orderCode)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

    // Không cho cancel nếu đã thanh toán
    if (transaction.getStatus() == PaymentStatus.PAID) {
      throw new AppException(ErrorCode.CANNOT_CANCEL_PAID_TRANSACTION);
    }

    transaction.setStatus(PaymentStatus.CANCELLED);
    transactionRepository.save(transaction);

    log.info("Payment cancelled via cancel URL for orderCode: {}", orderCode);
  }

  public Page<TransactionSummaryResponse> getAllTransactions(
      int page, int size, BigDecimal amount, PaymentStatus status, Long bookingId) {

    var context = SecurityContextHolder.getContext();
    String name = context.getAuthentication().getName();

    User user =
        userRepository
            .findByUsername(name)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    Pageable pageable = PageRequest.of(page, size);

    Page<Transaction> transactions =
        transactionRepository.searchByCreator(
            user.getUsername(), status, bookingId, amount, pageable);

    return transactions.map(
        tx ->
            TransactionSummaryResponse.builder()
                .id(tx.getId())
                .transactionId(tx.getTransactionId())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .paymentDate(tx.getPaymentDate())
                .bookingId(tx.getBooking().getId())
                .build());
  }

  public PaymentResponse getTransactionDetail(String transactionId) {

    Transaction transaction =
        transactionRepository
            .findByTransactionId(transactionId)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

    return PaymentResponse.builder()
        .transactionId(transaction.getTransactionId())
        .amount(transaction.getAmount())
        .status(transaction.getStatus().name())
        .paymentLinkId(transaction.getPayosPaymentLinkId())
        .build();
  }

  private String generateTransactionId() {
    return "TXN"
        + System.currentTimeMillis()
        + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private PaymentResponse buildPaymentResponse(Transaction transaction) {
    return PaymentResponse.builder()
        .transactionId(transaction.getTransactionId())
        .amount(transaction.getAmount())
        .status(transaction.getStatus().name())
        .paymentLinkId(transaction.getPayosPaymentLinkId())
        .build();
  }

  /** Sends payment confirmation notifications to all booking members and the tutor */
  private void sendPaymentConfirmationNotifications(Booking booking) {
    try {
      if (booking.getCourse() == null) {
        log.warn("Cannot send payment notifications: booking {} has no course", booking.getId());
        return;
      }

      String courseName = booking.getCourse().getName();
      String tutorName =
          booking.getCourse().getTutor() != null
              ? booking.getCourse().getTutor().getFirstName()
                  + " "
                  + booking.getCourse().getTutor().getLastName()
              : "the tutor";
      String tutorId =
          booking.getCourse().getTutor() != null
              ? booking.getCourse().getTutor().getUserId()
              : null;
      String courseImageUrl = booking.getCourse().getPictureUrl();
      String actionLink = NotificationLink.TUTOR_BOOKING.getPrefix();

      // Send notifications to all booking members
      if (booking.getBookingMembers() != null && !booking.getBookingMembers().isEmpty()) {
        for (BookingMember member : booking.getBookingMembers()) {
          try {
            String message =
                String.format(
                    "Payment completed for booking #%d - Course: %s with %s",
                    booking.getId(), courseName, tutorName);
            notificationService.createAndSendNotification(
                member.getUserId(), message, NotificationType.TYPICAL, courseImageUrl, actionLink);
          } catch (Exception e) {
            log.error(
                "Failed to send payment notification to booking member {}: {}",
                member.getUserId(),
                e.getMessage());
          }
        }
      }

      // Send notification to tutor
      if (tutorId != null) {
        try {
          String message =
              String.format(
                  "Payment received for booking #%d - Course: %s", booking.getId(), courseName);
          notificationService.createAndSendNotification(
              tutorId, message, NotificationType.TYPICAL, courseImageUrl, actionLink);
        } catch (Exception e) {
          log.error("Failed to send payment notification to tutor {}: {}", tutorId, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error(
          "Error sending payment confirmation notifications for booking {}: {}",
          booking.getId(),
          e.getMessage());
      // Don't throw exception - payment is already processed, notifications are secondary
    }
  }
}
