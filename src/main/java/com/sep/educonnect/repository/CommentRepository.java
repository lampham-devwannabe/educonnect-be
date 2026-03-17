package com.sep.educonnect.repository;

import com.sep.educonnect.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByProcessIdOrderByCreatedAtDesc(Long processId);
}
