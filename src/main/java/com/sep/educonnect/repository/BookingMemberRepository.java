package com.sep.educonnect.repository;

import com.sep.educonnect.entity.BookingMember;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.CourseType;
import com.sep.educonnect.enums.GroupType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookingMemberRepository extends JpaRepository<BookingMember, Long> {

    @Query("SELECT CASE WHEN COUNT(bm) > 0 THEN true ELSE false END " +
           "FROM BookingMember bm " +
           "WHERE bm.userId = :userId " +
           "AND bm.booking.course.id = :courseId " +
           "AND bm.booking.bookingStatus != 'REJECTED'")
    boolean existsByUserIdAndBooking_Course_Id(
            @Param("userId") String userId,
            @Param("courseId") Long courseId);

    @Query("SELECT CASE WHEN COUNT(bm) > 0 THEN true ELSE false END " +
           "FROM BookingMember bm " +
           "WHERE bm.userId = :userId " +
           "AND bm.booking.course.id = :courseId " +
            "AND bm.booking.registrationType = 'REGULAR'" +
           "AND bm.booking.bookingStatus != 'REJECTED'")
    boolean existsByUserIdAndBooking_Course_IdAndBookingStatusNotRejected(
            @Param("userId") String userId,
            @Param("courseId") Long courseId);

    @Query("SELECT bm FROM BookingMember bm WHERE bm.booking.course.id = :courseId AND bm.booking.bookingStatus = 'PAID'")
    List<BookingMember> findByBooking_Course_Id(Long courseId);

    @Query("SELECT bm FROM BookingMember bm " +
           "WHERE bm.booking.course.id = :courseId " +
           "AND (bm.booking.bookingStatus = 'PAID' " +
           "     OR (bm.booking.bookingStatus = 'APPROVED' AND bm.booking.registrationType = 'TRIAL'))")
    List<BookingMember> findPaidOrApprovedTrialByCourseId(@Param("courseId") Long courseId);

    Optional<BookingMember> findByUserIdAndBookingId(String userId, Long bookingId);

    @EntityGraph(attributePaths = { "booking", "booking.course", "booking.bookingMembers" })
    List<BookingMember> findByUserIdOrderByBooking_CreatedAtDesc(String userId);

    @EntityGraph(attributePaths = { "booking", "booking.course", "booking.bookingMembers" })
    @Query(value = """
                    SELECT bm FROM BookingMember bm
                    JOIN bm.booking b
                    LEFT JOIN b.course c
                    WHERE bm.userId = :userId
                      AND b.isDeleted = false
                      AND (:status IS NULL OR b.bookingStatus = :status)
                      AND (:groupType IS NULL OR b.groupType = :groupType)
                      AND (:courseType IS NULL OR c.type = :courseType)
                      AND (:amount IS NULL OR b.totalAmount = :amount)
                    """, countQuery = """
                    SELECT COUNT(bm) FROM BookingMember bm
                    JOIN bm.booking b
                    LEFT JOIN b.course c
                    WHERE bm.userId = :userId
                      AND b.isDeleted = false
                      AND (:status IS NULL OR b.bookingStatus = :status)
                      AND (:groupType IS NULL OR b.groupType = :groupType)
                      AND (:courseType IS NULL OR c.type = :courseType)
                      AND (:amount IS NULL OR b.totalAmount = :amount)
                    """)
    Page<BookingMember> searchByUser(
                    @Param("userId") String userId,
                    @Param("status") BookingStatus status,
                    @Param("groupType") GroupType groupType,
                    @Param("courseType") CourseType courseType,
                    @Param("amount") java.math.BigDecimal amount,
                    Pageable pageable);
}
