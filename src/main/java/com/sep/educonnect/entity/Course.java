package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.CourseStatus;
import com.sep.educonnect.enums.CourseType;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "course")
public class Course extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name")
    String name;

    BigDecimal price;

    Boolean isCombo;

    String pictureUrl;

    @Enumerated(EnumType.STRING)
    CourseStatus status;

    @Enumerated(EnumType.STRING)
    CourseType type;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    Integer totalLessons = 0;

    @ManyToOne
    @JoinColumn(name = "syllabus_id")
    Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private User tutor;

    @OneToMany(mappedBy = "course")
    List<TutorClass> tutorClasses;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonIgnore
    List<Booking> bookings;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonIgnore
    List<CourseReview> reviews;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonIgnore
    List<Wishlist> wishlists;
}
