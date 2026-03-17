package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "module")
public class Module extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    Long moduleId;

    @Column(name = "syllabus_id", insertable = false, updatable = false, nullable = false)
    Long syllabusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "order_number", nullable = false)
    Integer orderNumber;

    @Column(name = "status")
    String status; // DRAFT, PUBLISHED, ARCHIVED

    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    List<Lesson> lessons = new ArrayList<>();
}
