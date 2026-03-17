package com.sep.educonnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sep.educonnect.enums.CommentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "review_comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewComment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    Long commentId;

    @Column(name = "process_id")
    Long processId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "process_id", insertable = false, updatable = false)
    VerificationProcess process;

    @Column(name = "document_id")
    Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type")
    CommentType commentType;

    @Column(columnDefinition = "TEXT")
    String comment;

    @Column(name = "is_visible_to_tutor")
    Boolean isVisibleToTutor = false;

}
