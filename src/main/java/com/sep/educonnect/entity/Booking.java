package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.BookingStatus;
import com.sep.educonnect.enums.GroupType;
import com.sep.educonnect.enums.RegistrationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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
@Table(name = "booking")
public class Booking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", unique = false)
    @JsonIgnore
    Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    GroupType groupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type")
    RegistrationType registrationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    BookingStatus bookingStatus;

    @Column(name = "total_amount")
    BigDecimal totalAmount;

    @Column(name = "schedule_description", columnDefinition = "TEXT")
    String scheduleDescription;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    Set<Transaction> transactions = new HashSet<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    Set<BookingMember> bookingMembers = new HashSet<>();
}
