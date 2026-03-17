package com.sep.educonnect.service;

import com.sep.educonnect.constant.PredefinedRole;
import com.sep.educonnect.dto.discussion.request.DiscussionReplyRequest;
import com.sep.educonnect.dto.discussion.request.DiscussionRequest;
import com.sep.educonnect.dto.discussion.response.DiscussionReplyResponse;
import com.sep.educonnect.dto.discussion.response.DiscussionResponse;
import com.sep.educonnect.entity.Discussion;
import com.sep.educonnect.entity.DiscussionReply;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.Role;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscussionService {

    DiscussionRepository discussionRepository;
    DiscussionReplyRepository discussionReplyRepository;
    UserRepository userRepository;
    LessonRepository lessonRepository;

    @Transactional
    public void createDiscussion(DiscussionRequest request) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));

        Discussion discussion = Discussion.builder()
                .lessonId(lesson.getLessonId())
                .content(request.getContent())
                .userId(user.getUserId())
                .build();

        discussionRepository.save(discussion);
    }

    @Transactional(readOnly = true)
    public Page<DiscussionResponse> getDiscussionsByLesson(Long lessonId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return discussionRepository.findDiscussionResponsesByLessonId(lessonId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DiscussionReplyResponse> getRepliesByDiscussion(Long discussionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        return discussionReplyRepository.findByDiscussionId(discussionId, pageable)
                .map(reply -> {
                    User user = userRepository.findById(reply.getCreatedBy())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                    return DiscussionReplyResponse.builder()
                            .id(reply.getId())
                            .discussionId(reply.getDiscussion().getId())
                            .content(reply.getContent())
                            .username(user.getUsername())
                            .createdAt(reply.getCreatedAt())
                            .build();
                });
    }

    @Transactional
    public DiscussionReply addReply(DiscussionReplyRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Discussion discussion = discussionRepository
                .findById(request.getDiscussionId())
                .orElseThrow(() -> new AppException(ErrorCode.EXCEPTION_NOT_FOUND));

        DiscussionReply reply = DiscussionReply.builder()
                .discussion(discussion)
                .content(request.getContent())
                .createdBy(user.getUserId())
                .build();

        return discussionReplyRepository.save(reply);
    }

    @Transactional
    public void deleteDiscussion(Long discussionId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Discussion discussion = discussionRepository
                .findById(discussionId)
                .orElseThrow(() -> new AppException(ErrorCode.DISCUSSION_NOT_FOUND));

        boolean isOwner = discussion.getUserId().equals(currentUser.getUserId());
        Role role = currentUser.getRole();
        boolean isAdmin = role != null && PredefinedRole.ADMIN_ROLE.equalsIgnoreCase(role.getName());

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        discussionRepository.delete(discussion);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        DiscussionReply reply = discussionReplyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.DISCUSSION_REPLY_NOT_FOUND));

        boolean isOwner = reply.getCreatedBy().equals(currentUser.getUserId());
        Role role = currentUser.getRole();
        boolean isAdmin = role != null && PredefinedRole.ADMIN_ROLE.equalsIgnoreCase(role.getName());

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        discussionReplyRepository.delete(reply);
    }
}