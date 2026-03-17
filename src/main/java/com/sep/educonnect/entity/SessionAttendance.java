package com.sep.educonnect.entity;


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
@Table(name = "session_attendance")
public class SessionAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    ClassSession session;

    @ManyToOne
    @JoinColumn(name = "enrollment_id")
    ClassEnrollment enrollment;

    @Column(name = "attended")
    Boolean attended; // true/false/null (chưa điểm danh)

    @Column(name = "notes", length = 500)
    String notes;
}
