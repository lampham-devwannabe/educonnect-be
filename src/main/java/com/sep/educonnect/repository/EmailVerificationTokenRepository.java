package com.sep.educonnect.repository;

import com.sep.educonnect.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Query("""
            SELECT evt FROM EmailVerificationToken evt
            WHERE evt.user.userId = :userId
              AND evt.isUsed = false
              AND evt.expiresAt > :now
            ORDER BY evt.createdAt DESC
            """)
    List<EmailVerificationToken> findActiveTokensByUserId(String userId, LocalDateTime now);
}

