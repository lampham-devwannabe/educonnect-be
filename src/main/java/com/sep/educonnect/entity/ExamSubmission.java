package com.sep.educonnect.entity;

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
@Table(name = "exam_submission")
public class ExamSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    Long submissionId;

    @Column(name = "exam_id", nullable = false)
    Long examId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", insertable = false, updatable = false)
    Exam exam;

    @Column(name = "student_id", nullable = false)
    String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false, referencedColumnName = "user_id")
    User student;

    @Column(name = "score")
    Double score;

    @Column(name = "total_questions", nullable = false)
    Integer totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    Integer correctAnswers;

    @Column(name = "started_at", nullable = false)
    LocalDateTime startedAt;

    @Column(name = "submitted_at", nullable = false)
    LocalDateTime submittedAt;

    @Column(name = "duration_seconds")
    Long durationSeconds;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ExamAnswer> answers = new ArrayList<>();
}

