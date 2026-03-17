package com.sep.educonnect.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id")
    String tokenId;

    @Column(name = "token", nullable = false, unique = true)
    String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    Boolean isUsed = false;

    @Column(name = "used_at")
    LocalDateTime usedAt;

    @Column(name = "last_sent_at", nullable = false)
    LocalDateTime lastSentAt;

    @Column(name = "resend_count", nullable = false)
    @Builder.Default
    Integer resendCount = 0;
}

