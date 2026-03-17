package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.BookingMemberStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "booking_member")
public class BookingMember{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    Booking booking;

    String userId;
    String role; // e.g., OWNER, MEMBER
    BookingMemberStatus status;
}
