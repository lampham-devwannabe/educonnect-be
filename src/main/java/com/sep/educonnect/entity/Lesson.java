package com.sep.educonnect.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "lesson")
public class Lesson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    Long lessonId;

    @Column(name = "syllabus_id", nullable = false)
    Long syllabusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", insertable = false, updatable = false)
    Syllabus syllabus;

    @Column(name = "module_id", nullable = false)
    Long moduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", insertable = false, updatable = false)
    Module module;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "order_number")
    Integer orderNumber;

    @Column(name = "duration_minutes")
    Integer durationMinutes;

    @Column(name = "objectives", columnDefinition = "TEXT")
    String objectives;

    @Column(name = "status")
    String status; // DRAFT, PUBLISHED, ARCHIVED

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Exam> exams = new ArrayList<>();
}
