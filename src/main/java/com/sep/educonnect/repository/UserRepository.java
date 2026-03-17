package com.sep.educonnect.repository;

import com.sep.educonnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
        // In UserRepository
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId")
        Optional<User> findByIdWithRole(@Param("userId") String userId);

        boolean existsByUsername(String username);

        boolean existsByEmail(String email);

        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        @Query("SELECT u FROM User u WHERE u.isDeleted = false")
        List<User> findAllActiveUsers();

        @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.isDeleted = false")
        Optional<User> findByIdAndNotDeleted(String userId);

        @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
        Optional<User> findByUsernameAndNotDeleted(String username);

        @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
        Optional<User> findByEmailAndNotDeleted(String email);

        List<User> findByRole_NameAndIsDeletedFalse(String role);

        @Query("SELECT COUNT(u) FROM User u")
        Long countTotalUsers();

        @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
        Long countUsersByRole(@Param("roleName") String roleName);

        @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true")
        Long countVerifiedUsers();

        @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
        Long countNewUsersSince(@Param("startDate") LocalDateTime startDate);

        @Query("SELECT COUNT(DISTINCT u) FROM User u " +
                        "JOIN Booking b ON b.bookingMembers IS NOT EMPTY " +
                        "WHERE b.createdAt >= :startDate")
        Long countActiveUsersSince(@Param("startDate") LocalDateTime startDate);

        @Query("""
                        SELECT u FROM User u
                        WHERE
                          (:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
                          AND (:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
                          AND (:phoneNumber IS NULL OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :phoneNumber, '%')))
                          AND (:roleName IS NULL OR LOWER(u.role.name) LIKE LOWER(CONCAT('%', :roleName, '%')))
                          AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                        """)
        Page<User> searchUsers(
                        @Param("firstName") String firstName,
                        @Param("lastName") String lastName,
                        @Param("phoneNumber") String phoneNumber,
                        @Param("roleName") String roleName,
                        @Param("email") String email,
                        Pageable pageable);
}
