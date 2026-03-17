package com.sep.educonnect.repository;

import com.sep.educonnect.entity.Booking;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.RegistrationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"course", "bookingMembers", "transactions"})
    Page<Booking> findByBookingStatus(BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "bookingMembers", "transactions"})
    Page<Booking> findAll(Pageable pageable);
    
    @EntityGraph(attributePaths = {"course", "transactions", "bookingMembers"})
    Optional<Booking> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"course", "course.tutorClasses","transactions", "bookingMembers"})
    Optional<Booking> findWithClassDetailsById(Long id);

    @Query("""
        SELECT b FROM Booking b
        WHERE (:status IS NULL OR b.bookingStatus = :status)
          AND (
                LOWER(b.course.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(b.scheduleDescription) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(b.createdBy) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    Page<Booking> searchByStatusAndTerm(@Param("status") BookingStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);
    
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        JOIN b.bookingMembers bm
        WHERE bm.userId = :userId
          AND b.course.id = :courseId
          AND b.registrationType = :registrationType
        """)
    boolean existsByBookingMembers_UserIdAndCourse_IdAndRegistrationType(
        @Param("userId") String userId,
        @Param("courseId") Long courseId,
        @Param("registrationType") com.sep.educonnect.enums.RegistrationType registrationType
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.course.id = :courseId AND b.bookingStatus = :status")
    Long countByCourseIdAndBookingStatus(@Param("courseId") Long courseId, @Param("status") BookingStatus status);

    @EntityGraph(attributePaths = {"course", "course.tutor", "bookingMembers", "transactions"})
    @Query("SELECT DISTINCT b FROM Booking b " +
           "WHERE b.bookingStatus = :status " +
           "AND b.course.tutor.userId = :tutorUserId " +
           "AND b.course.isDeleted = false")
    List<Booking> findByBookingStatusAndCourseTutorUserId(
            @Param("status") BookingStatus status,
            @Param("tutorUserId") String tutorUserId);

    List<Booking> findByRegistrationTypeAndBookingStatus(RegistrationType registrationType, BookingStatus bookingStatus);
}
