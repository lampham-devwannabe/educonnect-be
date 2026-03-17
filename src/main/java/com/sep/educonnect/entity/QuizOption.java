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
@Table(name = "quiz_options")
public class QuizOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    Long optionId;

    @Column(name = "quiz_id", nullable = false)
    Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    Quiz quiz;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    String text;

    @Column(name = "isCorrect", nullable = false)
    @Builder.Default
    Boolean isCorrect = false;
}
