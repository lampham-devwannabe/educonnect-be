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
@Table(name = "exam")
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    Long examId;

    @Column(name = "lesson_id", nullable = false)
    Long lessonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
    Lesson lesson;

    @Column(name = "status")
    String status; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "field")
    String field;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Quiz> quizzes = new ArrayList<>();

    @Column(name = "tutor_class_id")
    Long tutorClassId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_class_id", insertable = false, updatable = false)
    TutorClass tutorClass;
}
