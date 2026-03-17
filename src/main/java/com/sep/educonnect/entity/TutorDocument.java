package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.DocumentStatus;
import com.sep.educonnect.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tutor_document")
public class TutorDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    @JsonIgnore
    TutorProfile profile;

    @Enumerated(EnumType.STRING)
    DocumentType type;

    String fileName;

    @Enumerated(EnumType.STRING)
    DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @CreationTimestamp
    LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "verified_by_user_id")
    User verifiedBy;

}
