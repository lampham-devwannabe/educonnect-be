package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    String userId;

    @Column(name = "username", unique = true)
    String username;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "password")
    String password;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "phone_number")
    String phoneNumber;

    @Column(name = "address")
    String address;

    @Column(name = "dob")
    LocalDate dob;

    String avatar;

    @Column(name = "login_type")
    String loginType;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    LocalDateTime emailVerifiedAt;

    // For preferred teaching styles - only for student now
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences")
    List<Long> preferences;

    // Null = never chosen
    @Column(name = "has_chosen_preferences")
    Boolean hasChosenPreferences = null;

    @Column
    @JsonIgnore
    @OneToMany(mappedBy = "reviewedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<TutorProfile> list = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    Role role;

    @OneToMany(mappedBy = "tutor")
    List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    List<Notification> notifications = new ArrayList<>();
}
