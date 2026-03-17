package com.sep.educonnect.repository;

import com.sep.educonnect.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.email = :email AND prt.isUsed = false AND prt.expiresAt > :now")
    List<PasswordResetToken> findActiveTokensByEmail(String email, LocalDateTime now);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.userId = :userId AND prt.isUsed = false AND prt.expiresAt > :now")
    List<PasswordResetToken> findActiveTokensByUserId(String userId, LocalDateTime now);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    List<PasswordResetToken> findExpiredTokens(LocalDateTime now);
}