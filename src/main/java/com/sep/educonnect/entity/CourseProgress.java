package com.sep.educonnect.entity;

import com.sep.educonnect.enums.CourseProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
@Table(name = "course_progress")
public class CourseProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    ClassEnrollment enrollment;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    Integer totalLessons = 0;

    @Column(name = "completed_lessons", nullable = false)
    @Builder.Default
    Integer completedLessons = 0;

    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    Integer progressPercentage = 0;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    CourseProgressStatus status = CourseProgressStatus.NOT_STARTED;

    @OneToMany(mappedBy = "courseProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<LessonProgress> lessonProgresses = new ArrayList<>();

    public void incrementCompletedLessons() {
        completedLessons = Math.min(completedLessons + 1, totalLessons);
    }

    public void decrementCompletedLessons() {
        completedLessons = Math.max(completedLessons - 1, 0);
    }
}

