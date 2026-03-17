package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.CurrencyCode;
import com.sep.educonnect.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tutor_profile")
public class TutorProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String bioEn;
    String bioVi;
    String experience;
    BigDecimal hourlyRate;
    CurrencyCode currencyCode;
    @Enumerated(EnumType.STRING)
    ProfileStatus submissionStatus = ProfileStatus.DRAFT;
    @Column(name = "desc_en", columnDefinition = "TEXT")
    String descEn;
    @Column(name = "desc_vi", columnDefinition = "TEXT")
    String descVi;
    String rejectionReason;
    LocalDate lastReject;
    String videoLink;
    Integer studentCount = 0;
    Integer reviewCount = 0;
    Double rating = 0.0;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    LocalDateTime reviewedAt;
    @Enumerated(EnumType.STRING)

    @ManyToMany
    @JoinTable(
            name = "tutor_profile_tags",
            joinColumns = @JoinColumn(name = "tutor_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    Set<Tag> tags = new HashSet<>();
    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    User user;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<VerificationProcess> verificationProcesses = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<TutorDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "tutorProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<TutorAvailabilityException> availabilityExceptions = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "tutor_subject",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    Set<Subject> subjects;

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<StudentLikes> likes = new ArrayList<>();
}