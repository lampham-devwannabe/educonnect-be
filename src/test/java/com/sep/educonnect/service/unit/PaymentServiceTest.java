package com.sep.educonnect.service.unit;

import com.sep.educonnect.configuration.PayOSConfig;
import com.sep.educonnect.dto.payment.PaymentRequest;
import com.sep.educonnect.dto.payment.PaymentResponse;
import com.sep.educonnect.dto.transaction.TransactionSummaryResponse;
import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.enums.PaymentStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.PaymentService;
import com.sep.educonnect.service.ProgressService;
import com.sep.educonnect.util.MockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @Mock private TutorClassRepository tutorClassRepository;

    @Mock private BookingRepository bookingRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PayOS payOS;

    @Mock private PayOSConfig payOSConfig;

    @Mock private UserRepository userRepository;

    @Mock private ProgressService progressService;

    @InjectMocks private PaymentService paymentService;

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    @Test
    @DisplayName("Should create payment successfully")
    void should_createPayment_successfully() throws Exception {
        // Given
        Long bookingId = 1L;
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBookingStatus(BookingStatus.APPROVED);
        booking.setTotalAmount(new BigDecimal("100000"));

        CreatePaymentLinkResponse payOSResponse = new CreatePaymentLinkResponse();
        payOSResponse.setCheckoutUrl("http://checkout.url");
        payOSResponse.setPaymentLinkId("payment-link-id");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(payOSConfig.getReturnUrl()).thenReturn("http://return.url");
        when(payOSConfig.getCancelUrl()).thenReturn("http://cancel.url");
        when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class)))
                .thenReturn(payOSResponse);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // When
        PaymentResponse response = paymentService.createPayment(request);

        // Then
        assertNotNull(response);
        assertEquals("http://checkout.url", response.getCheckoutUrl());
        assertEquals("payment-link-id", response.getPaymentLinkId());
        assertEquals(PaymentStatus.PENDING.name(), response.getStatus());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when booking not found")
    void should_throwException_whenBookingNotFound() {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> paymentService.createPayment(request));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when booking already paid")
    void should_throwException_whenBookingAlreadyPaid() {
        // Given
        Long bookingId = 1L;
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        Booking booking = new Booking();
        booking.setBookingStatus(BookingStatus.PAID);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> paymentService.createPayment(request));
        assertEquals(ErrorCode.BOOKING_ALREADY_PAID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when booking not approved")
    void should_throwException_whenBookingNotApproved() {
        // Given
        Long bookingId = 1L;
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        Booking booking = new Booking();
        booking.setBookingStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> paymentService.createPayment(request));
        assertEquals(ErrorCode.BOOKING_MUST_BE_APPROVED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle payment return successfully (PAID)")
    void should_handlePaymentReturn_successfully_PAID() throws Exception {
        // Given
        Long orderCode = 123456L;

        // Create real Booking object
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingStatus(BookingStatus.PENDING);

        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setBooking(booking);

        // Create Course with SELF_PACED type and TutorClass
        Course course = new Course();
        course.setId(1L);
        course.setType(CourseType.SELF_PACED);

        TutorClass tutorClass = new TutorClass();
        tutorClass.setId(1L);
        tutorClass.setEnrollments(new ArrayList<>()); // Initialize enrollments list

        course.setTutorClasses(List.of(tutorClass));

        // Create BookingMember
        BookingMember bookingMember = new BookingMember();
        bookingMember.setUserId("1L");

        User student = new User();
        student.setUserId("1L");

        // Create Booking with details for notification
        Booking bookingWithDetails = new Booking();
        bookingWithDetails.setId(1L);
        bookingWithDetails.setBookingStatus(BookingStatus.PENDING);
        bookingWithDetails.setCourse(course);
        bookingWithDetails.setBookingMembers(new HashSet<>(List.of(bookingMember)));

        // Mock PaymentLink
        PaymentLink paymentLink = mock(PaymentLink.class, Answers.RETURNS_DEEP_STUBS);
        when(paymentLink.getStatus().toString()).thenReturn("PAID");

        // Mock repository calls
        when(transactionRepository.findByPayosOrderCode(orderCode))
                .thenReturn(Optional.of(transaction));
        when(payOS.paymentRequests().get(orderCode)).thenReturn(paymentLink);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingRepository.findWithClassDetailsById(booking.getId()))
                .thenReturn(Optional.of(bookingWithDetails)); // Fixed method name
        when(userRepository.findById(bookingMember.getUserId())).thenReturn(Optional.of(student));
        when(tutorClassRepository.save(any(TutorClass.class))).thenReturn(tutorClass);
        // Ensure saved enrollment is returned to avoid NPE in service when savedEnrollment.getId() is accessed
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> {
                    ClassEnrollment en = invocation.getArgument(0);
                    en.setId(100L);
                    return en;
                });

        // When
        PaymentResponse response = paymentService.handlePaymentReturn(orderCode, null);

        // Then
        assertEquals(PaymentStatus.PAID.name(), response.getStatus());
        assertEquals(PaymentStatus.PAID, transaction.getStatus());
        assertEquals(BookingStatus.PAID, transaction.getBooking().getBookingStatus());
        assertNotNull(transaction.getPaymentDate());

        verify(transactionRepository, times(1)).save(transaction);
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(bookingRepository, times(1))
                .findWithClassDetailsById(booking.getId()); // Fixed method name
        verify(userRepository, times(1)).findById(bookingMember.getUserId());
        verify(tutorClassRepository, times(1)).save(any(TutorClass.class));
    }

    @Test
    @DisplayName("Should handle payment return successfully (CANCELLED)")
    void should_handlePaymentReturn_successfully_CANCELLED() throws Exception {
        // Given
        Long orderCode = 123456L;
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PENDING);

        PaymentLink paymentLink = mock(PaymentLink.class, Answers.RETURNS_DEEP_STUBS);
        when(paymentLink.getStatus().toString()).thenReturn("CANCELLED");

        when(transactionRepository.findByPayosOrderCode(orderCode))
                .thenReturn(Optional.of(transaction));
        when(payOS.paymentRequests().get(orderCode)).thenReturn(paymentLink);

        // When
        PaymentResponse response = paymentService.handlePaymentReturn(orderCode, null);

        // Then
        assertEquals(PaymentStatus.CANCELLED.name(), response.getStatus());
        assertEquals(PaymentStatus.CANCELLED, transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("Should return immediately if transaction already paid")
    void should_returnImmediately_ifTransactionAlreadyPaid() {
        // Given
        Long orderCode = 123456L;
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PAID);

        when(transactionRepository.findByPayosOrderCode(orderCode))
                .thenReturn(Optional.of(transaction));

        // When
        PaymentResponse response = paymentService.handlePaymentReturn(orderCode, null);

        // Then
        assertEquals(PaymentStatus.PAID.name(), response.getStatus());
        verify(payOS, never()).paymentRequests();
    }

    @Test
    @DisplayName("Should cancel payment successfully")
    void should_cancelPayment_successfully() {
        // Given
        Long orderCode = 123456L;
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PENDING);

        when(transactionRepository.findByPayosOrderCode(orderCode))
                .thenReturn(Optional.of(transaction));

        // When
        paymentService.cancelPayment(orderCode);

        // Then
        assertEquals(PaymentStatus.CANCELLED, transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("Should throw exception when cancelling paid transaction")
    void should_throwException_whenCancellingPaidTransaction() {
        // Given
        Long orderCode = 123456L;
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PAID);

        when(transactionRepository.findByPayosOrderCode(orderCode))
                .thenReturn(Optional.of(transaction));

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> paymentService.cancelPayment(orderCode));
        assertEquals(ErrorCode.CANNOT_CANCEL_PAID_TRANSACTION, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get all transactions with pagination")
    void should_getAllTransactions_successfully() {
        // Given
        MockHelper.mockSecurityContext("user");
        User user = new User();
        user.setUsername("user");

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(BigDecimal.TEN);
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setBooking(new Booking());

        Page<Transaction> page = new PageImpl<>(Collections.singletonList(transaction));

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(transactionRepository.searchByCreator(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<TransactionSummaryResponse> result =
                paymentService.getAllTransactions(0, 10, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should get transaction detail successfully")
    void should_getTransactionDetail_successfully() {
        // Given
        String transactionId = "txn-1";
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setAmount(BigDecimal.TEN);
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setPayosPaymentLinkId("link-id");

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(transaction));

        // When
        PaymentResponse response = paymentService.getTransactionDetail(transactionId);

        // Then
        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
    }
}
