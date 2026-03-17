package com.sep.educonnect.repository;

import com.sep.educonnect.dto.discussion.response.DiscussionResponse;
import com.sep.educonnect.entity.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    @Query("""
        SELECT new com.sep.educonnect.dto.discussion.response.DiscussionResponse(
            d.id,
            d.lessonId,
            d.content,
            u.username,
            d.createdAt,
            COUNT(r.id)
        )
        FROM Discussion d
        JOIN User u ON u.userId = d.userId
        LEFT JOIN DiscussionReply r ON r.discussion = d
        WHERE d.lessonId = :lessonId
        GROUP BY d.id, d.lessonId, d.content, u.username, d.createdAt
    """)
    Page<DiscussionResponse> findDiscussionResponsesByLessonId(Long lessonId, Pageable pageable);
}