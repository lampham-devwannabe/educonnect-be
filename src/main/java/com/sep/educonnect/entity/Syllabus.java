package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "syllabus")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Syllabus extends BaseEntity {

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<Course> courses;

    @OneToMany(mappedBy = "syllabus", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    List<Module> modules = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabus_id")
    private Long syllabusId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "level_vi")
    private String levelVi;

    @Column(name = "level_en")
    private String levelEn;

    // Target audience - not goals
    @Column(name = "target_vi")
    private String targetVi;

    @Column(name = "target_en")
    private String targetEn;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description_vi", columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "status")
    private String status;
}
