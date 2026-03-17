package com.sep.educonnect.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "schedule_change")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleChange extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    ClassSession session;

    @Column(name = "old_date", nullable = false)
    LocalDate oldDate;

    @Column(name = "new_date", nullable = false)
    LocalDate newDate;

    @Column(name = "new_slot", nullable = false)
    Integer newSLot;

    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Column(name = "status", length = 50)
    String status; // PENDING, APPROVED, REJECTED, CANCELLED

}
