package com.sep.educonnect.repository;

import com.sep.educonnect.entity.DiscussionReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    List<DiscussionReply> findByDiscussion_Id(Long discussionId);

    Page<DiscussionReply> findByDiscussionId(
            Long discussionId,
            Pageable pageable
    );

    void deleteAllByDiscussion_Id(Long discussionId);

}