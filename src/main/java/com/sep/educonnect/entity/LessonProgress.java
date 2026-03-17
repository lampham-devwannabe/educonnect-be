package com.sep.educonnect.entity;

import com.sep.educonnect.enums.LessonProgressStatus;
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
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_progress_course_lesson", columnNames = {"course_progress_id", "lesson_id"})
        }
)
public class LessonProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_progress_id", nullable = false)
    CourseProgress courseProgress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    Lesson lesson;

    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    Boolean optionalLesson = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    LessonProgressStatus status = LessonProgressStatus.NOT_STARTED;

    @Column(name = "result")
    String result;

    @Column(name = "last_accessed_at")
    LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;
}

