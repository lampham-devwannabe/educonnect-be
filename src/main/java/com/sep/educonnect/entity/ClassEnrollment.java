package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "class_enrollment")
public class ClassEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "class_id")
    TutorClass tutorClass;

    @ManyToOne
    @JoinColumn(name = "student_id")
    User student;

    @Column(name = "enrolled_at")
    LocalDateTime enrolledAt;

    @Column(name = "notes", length = 1000)
    String notes; // Ghi chú của học sinh khi đăng ký

    @Column(name = "attendance")
    Boolean hasJoined; // Có tham gia không

    @JsonIgnore
    @OneToOne(mappedBy = "enrollment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    CourseProgress courseProgress;
}
