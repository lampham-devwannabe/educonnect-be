package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tutor_class")
public class TutorClass extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    User tutor;

    @ManyToOne
    @JoinColumn(name = "course_id")
    Course course;

    @Column(name = "start_date")
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(name = "last_join_date")
    LocalDate lastJoinDate = null; // Hạn cuối đăng ký lớp

    @Column(name = "max_students")
    Integer maxStudents = 5; // Tối đa bao nhiêu học sinh

    @Column(name = "current_students")
    Integer currentStudents = 0; // Số học sinh hiện tại

    @Column(name = "title")
    String title; // Tên lớp học: "Toán lớp 10 - Hàm số"

    @Column(name = "description", length = 2000)
    String description;

    // Danh sách học sinh đã đăng ký
    @JsonIgnore
    @OneToMany(mappedBy = "tutorClass", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ClassEnrollment> enrollments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tutorClass", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ClassSession> sessions = new ArrayList<>();

    // Helper methods
    public boolean isFull() {
        return maxStudents != null && currentStudents >= maxStudents;
    }

    public boolean canEnroll() {
        return !isFull() &&
                (lastJoinDate == null || LocalDate.now().isBefore(lastJoinDate));
    }

    public Integer getTotalSessions() {
        return sessions != null ? sessions.size() : 0;
    }
}
