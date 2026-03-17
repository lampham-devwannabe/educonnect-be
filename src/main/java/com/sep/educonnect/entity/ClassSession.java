package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
@Table(name = "class_session")
public class ClassSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "class_id")
    TutorClass tutorClass;

    @Column(name = "session_date")
    LocalDate sessionDate;

    @Column(name = "slot_number")
    Integer slotNumber; // 1-10 (ca mấy)

    @Column(name = "session_number")
    Integer sessionNumber; // Buổi thứ mấy trong lớp (1, 2, 3...)

    @Column(name = "topic")
    String topic; // Chủ đề buổi học

    @Column(length = 1000)
    String meetingJoinUrl;
    @Column(length = 1000)
    String meetingStartUrl;
    String meetingPassword;
    String meetingId;
    String eventId;
    LocalDateTime startTime;
    LocalDateTime endTime;

    @Column(name = "notes", length = 2000)
    String notes; // Ghi chú

    // Danh sách attendance của buổi học này
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SessionAttendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @JsonIgnore
    List<TutorAvailabilityException> exceptions = new ArrayList<>();
}
