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
@Table(name = "quiz")
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    Long quizId;

    @Column(name = "exam_id", nullable = false)
    Long examId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", insertable = false, updatable = false)
    Exam exam;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    String text;

    @Column(name = "order_no", nullable = false)
    Integer orderNo;

    @Column(name = "type")
    String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, etc.

    @Column(name = "valid_answer")
    String validAnswer;

    @Column(name = "explanation")
    String explanation;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<QuizOption> options = new ArrayList<>();
}
