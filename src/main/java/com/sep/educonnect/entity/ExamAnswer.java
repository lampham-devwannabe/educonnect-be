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
@Table(name = "exam_answer")
public class ExamAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    Long answerId;

    @Column(name = "submission_id", nullable = false)
    Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", insertable = false, updatable = false)
    ExamSubmission submission;

    @Column(name = "quiz_id", nullable = false)
    Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    Quiz quiz;

    @Column(name = "student_answer", columnDefinition = "TEXT")
    String studentAnswer;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    String correctAnswer;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    Boolean isCorrect = false;
}

