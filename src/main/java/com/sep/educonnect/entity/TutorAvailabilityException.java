package com.sep.educonnect.entity;

import com.sep.educonnect.enums.ExceptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "tutor_availability_exceptions")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class TutorAvailabilityException extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    TutorProfile tutorProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    ClassSession session;

    @Column(name = "reason", length = 500)
    String reason;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;

    @Column(name = "is_approved")
    Boolean isApproved = false;

    @Column(name = "approved_by")
    String approvedBy;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    ExceptionStatus status = ExceptionStatus.PENDING;

}
