package com.sep.educonnect.repository;

import com.sep.educonnect.dto.admin.statistic.MonthlyRevenueDTO;
import com.sep.educonnect.entity.Booking;
import com.sep.educonnect.entity.Transaction;
import com.sep.educonnect.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByPayosOrderCode(Long orderCode);

    Optional<Transaction> findByBookingIdAndStatus(Long booking, PaymentStatus status);

    @Query(
            """
                    SELECT t FROM Transaction t
                    WHERE t.createdBy = :createdBy
                      AND t.isDeleted = false
                      AND (:status IS NULL OR t.status = :status)
                      AND (:bookingId IS NULL OR t.booking.id = :bookingId)
                      AND (:amount IS NULL OR t.amount = :amount)
                    """)
    Page<Transaction> searchByCreator(
            @Param("createdBy") String createdBy,
            @Param("status") PaymentStatus status,
            @Param("bookingId") Long bookingId,
            @Param("amount") BigDecimal amount,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.status = 'PAID' AND t.paymentDate >= :startDate")
    BigDecimal calculateRevenueSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    Long countTransactionsByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t " +
            "WHERE t.status = 'PAID'")
    BigDecimal calculateAverageOrderValue();

    @Query("SELECT new com.sep.educonnect.dto.admin.statistic.MonthlyRevenueDTO(" +
            "YEAR(t.paymentDate), MONTH(t.paymentDate), " +
            "SUM(t.amount), COUNT(t)) " +
            "FROM Transaction t " +
            "WHERE t.status = 'PAID' " +
            "AND t.paymentDate >= :startDate " +
            "GROUP BY YEAR(t.paymentDate), MONTH(t.paymentDate) " +
            "ORDER BY YEAR(t.paymentDate) DESC, MONTH(t.paymentDate) DESC")
    List<MonthlyRevenueDTO> getMonthlyRevenue(@Param("startDate") LocalDateTime startDate);
}
