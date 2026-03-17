package com.sep.educonnect.entity;

import com.sep.educonnect.enums.ProcessPriority;
import com.sep.educonnect.enums.VerificationStage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verification_process")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationProcess extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_id")
    Long processId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    TutorProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage")
    VerificationStage currentStage = VerificationStage.DOCUMENT_REVIEW;

    @Column(name = "started_at")
    LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    LocalDateTime completedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer")
    User reviewer;

    @Enumerated(EnumType.STRING)
    ProcessPriority priority = ProcessPriority.NORMAL;

    @Column(name = "estimated_completion_date")
    LocalDateTime estimatedCompletionDate;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL)
    List<ReviewComment> comments = new ArrayList<>();
}
