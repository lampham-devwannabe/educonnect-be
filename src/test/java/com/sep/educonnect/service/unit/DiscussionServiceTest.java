package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.sep.educonnect.repository.DiscussionReplyRepository;
import com.sep.educonnect.repository.DiscussionRepository;
import com.sep.educonnect.repository.LessonRepository;
import com.sep.educonnect.repository.UserRepository;
import com.sep.educonnect.service.DiscussionService;
import com.sep.educonnect.util.MockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscussionService Unit Tests")
class DiscussionServiceTest {

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private DiscussionReplyRepository discussionReplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private DiscussionService discussionService;

    private User studentUser;
    private User adminUser;
    private Role studentRole;
    private Role adminRole;
    private Lesson lesson;
    private Discussion discussion;
    private DiscussionReply reply;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder().id(1L).name(PredefinedRole.STUDENT_ROLE).build();
        adminRole = Role.builder().id(2L).name(PredefinedRole.ADMIN_ROLE).build();

        studentUser = User.builder()
                .userId("student-1")
                .username("student")
                .role(studentRole)
                .build();

        adminUser = User.builder()
                .userId("admin-1")
                .username("admin")
                .role(adminRole)
                .build();

        lesson = Lesson.builder()
                .lessonId(1L)
                .title("Lesson 1")
                .build();

        discussion = Discussion.builder()
                .id(1L)
                .lessonId(lesson.getLessonId())
                .content("Discussion content")
                .userId(studentUser.getUserId())
                .build();
        discussion.setCreatedAt(LocalDateTime.now());

        reply = DiscussionReply.builder()
                .id(1L)
                .discussion(discussion)
                .content("Reply content")
                .createdBy(studentUser.getUserId())
                .build();
        reply.setCreatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        MockHelper.clearSecurityContext();
    }

    // ==================== createDiscussion ====================

    @Test
    @DisplayName("DS01 - createDiscussion: should create discussion successfully")
    void createDiscussion_shouldCreateSuccessfully() {
        // Given
        MockHelper.mockSecurityContext("student");
        DiscussionRequest request = DiscussionRequest.builder()
                .lessonId(lesson.getLessonId())
                .content("New discussion")
                .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(lessonRepository.findById(lesson.getLessonId())).thenReturn(Optional.of(lesson));

        // When
        discussionService.createDiscussion(request);

        // Then
        verify(discussionRepository).save(argThat(saved -> saved.getLessonId().equals(lesson.getLessonId())
                && saved.getUserId().equals(studentUser.getUserId())
                && saved.getContent().equals("New discussion")));
    }

    @Test
    @DisplayName("DS02 - createDiscussion: should throw USER_NOT_EXISTED when user not found")
    void createDiscussion_shouldThrowUserNotExisted_whenUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        DiscussionRequest request = DiscussionRequest.builder()
                .lessonId(lesson.getLessonId())
                .content("New discussion")
                .build();

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.createDiscussion(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        verify(discussionRepository, never()).save(any());
    }

    @Test
    @DisplayName("DS03 - createDiscussion: should throw LESSON_NOT_EXISTED when lesson not found")
    void createDiscussion_shouldThrowLessonNotExisted_whenLessonNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        DiscussionRequest request = DiscussionRequest.builder()
                .lessonId(999L)
                .content("New discussion")
                .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.createDiscussion(request));
        assertEquals(ErrorCode.LESSON_NOT_EXISTED, ex.getErrorCode());
        verify(discussionRepository, never()).save(any());
    }

    // ==================== getDiscussionsByLesson ====================

    @Test
    @DisplayName("DS04 - getDiscussionsByLesson: should return paged discussions")
    void getDiscussionsByLesson_shouldReturnPagedResult() {
        // Given
        DiscussionResponse response = DiscussionResponse.builder()
                .id(discussion.getId())
                .lessonId(discussion.getLessonId())
                .content(discussion.getContent())
                .username(studentUser.getUsername())
                .createdAt(discussion.getCreatedAt())
                .replyCount(0L)
                .build();
        Page<DiscussionResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(discussionRepository.findDiscussionResponsesByLessonId(eq(lesson.getLessonId()), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<DiscussionResponse> result = discussionService.getDiscussionsByLesson(lesson.getLessonId(), 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(discussion.getId(), result.getContent().get(0).getId());
        verify(discussionRepository)
                .findDiscussionResponsesByLessonId(eq(lesson.getLessonId()), any(Pageable.class));
    }

    // ==================== getRepliesByDiscussion ====================

    @Test
    @DisplayName("DS05 - getRepliesByDiscussion: should map replies to DTO with username")
    void getRepliesByDiscussion_shouldMapToDtoWithUsername() {
        // Given
        Page<DiscussionReply> page = new PageImpl<>(List.of(reply), PageRequest.of(0, 10), 1);

        when(discussionReplyRepository.findByDiscussionId(eq(discussion.getId()), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findById(studentUser.getUserId()))
                .thenReturn(Optional.of(studentUser));

        // When
        Page<DiscussionReplyResponse> result = discussionService.getRepliesByDiscussion(discussion.getId(), 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        DiscussionReplyResponse dto = result.getContent().get(0);
        assertEquals(reply.getId(), dto.getId());
        assertEquals(discussion.getId(), dto.getDiscussionId());
        assertEquals("Reply content", dto.getContent());
        assertEquals(studentUser.getUsername(), dto.getUsername());
    }

    @Test
    @DisplayName("DS06 - getRepliesByDiscussion: should throw USER_NOT_EXISTED when reply user missing")
    void getRepliesByDiscussion_shouldThrowUserNotExisted_whenUserMissing() {
        // Given
        Page<DiscussionReply> page = new PageImpl<>(List.of(reply), PageRequest.of(0, 10), 1);

        when(discussionReplyRepository.findByDiscussionId(eq(discussion.getId()), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findById(studentUser.getUserId()))
                .thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.getRepliesByDiscussion(discussion.getId(), 0, 10));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
    }

    // ==================== addReply ====================

    @Test
    @DisplayName("DS07 - addReply: should create reply with createdBy = current user")
    void addReply_shouldCreateReplyWithCreatedBy() {
        // Given
        MockHelper.mockSecurityContext("student");
        DiscussionReplyRequest request = DiscussionReplyRequest.builder()
                .discussionId(discussion.getId())
                .content("New reply")
                .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionRepository.findById(discussion.getId()))
                .thenReturn(Optional.of(discussion));
        when(discussionReplyRepository.save(any(DiscussionReply.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DiscussionReply result = discussionService.addReply(request);

        // Then
        assertNotNull(result);
        assertEquals("New reply", result.getContent());
        assertEquals(studentUser.getUserId(), result.getCreatedBy());
        assertEquals(discussion, result.getDiscussion());
        verify(discussionReplyRepository).save(any(DiscussionReply.class));
    }

    @Test
    @DisplayName("DS08 - addReply: should throw USER_NOT_EXISTED when current user not found")
    void addReply_shouldThrowUserNotExisted_whenCurrentUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        DiscussionReplyRequest request = DiscussionReplyRequest.builder()
                .discussionId(discussion.getId())
                .content("New reply")
                .build();

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.addReply(request));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        verify(discussionReplyRepository, never()).save(any());
    }

    @Test
    @DisplayName("DS09 - addReply: should throw EXCEPTION_NOT_FOUND when discussion not found")
    void addReply_shouldThrowExceptionNotFound_whenDiscussionNotFound() {
        // Given
        MockHelper.mockSecurityContext("student");
        DiscussionReplyRequest request = DiscussionReplyRequest.builder()
                .discussionId(999L)
                .content("New reply")
                .build();

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.addReply(request));
        assertEquals(ErrorCode.EXCEPTION_NOT_FOUND, ex.getErrorCode());
        verify(discussionReplyRepository, never()).save(any());
    }

    // ==================== deleteDiscussion ====================

    @Test
    @DisplayName("DS10 - deleteDiscussion: should delete when current user is owner")
    void deleteDiscussion_shouldDelete_whenOwner() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionRepository.findById(discussion.getId()))
                .thenReturn(Optional.of(discussion));

        // When
        discussionService.deleteDiscussion(discussion.getId());

        // Then
        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("DS11 - deleteDiscussion: should delete when current user is admin")
    void deleteDiscussion_shouldDelete_whenAdmin() {
        // Given
        MockHelper.mockSecurityContext("admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(discussionRepository.findById(discussion.getId()))
                .thenReturn(Optional.of(discussion));

        // When
        discussionService.deleteDiscussion(discussion.getId());

        // Then
        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("DS12 - deleteDiscussion: should throw USER_NOT_EXISTED when current user not found")
    void deleteDiscussion_shouldThrowUserNotExisted_whenCurrentUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteDiscussion(discussion.getId()));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        verify(discussionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DS13 - deleteDiscussion: should throw DISCUSSION_NOT_FOUND when discussion missing")
    void deleteDiscussion_shouldThrowDiscussionNotFound_whenDiscussionMissing() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteDiscussion(999L));
        assertEquals(ErrorCode.DISCUSSION_NOT_FOUND, ex.getErrorCode());
        verify(discussionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DS14 - deleteDiscussion: should throw UNAUTHORIZED when not owner and not admin")
    void deleteDiscussion_shouldThrowUnauthorized_whenNotOwnerAndNotAdmin() {
        // Given
        MockHelper.mockSecurityContext("another");
        User anotherUser = User.builder()
                .userId("another-1")
                .username("another")
                .role(studentRole)
                .build();

        when(userRepository.findByUsername("another")).thenReturn(Optional.of(anotherUser));
        when(discussionRepository.findById(discussion.getId()))
                .thenReturn(Optional.of(discussion));

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteDiscussion(discussion.getId()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(discussionRepository, never()).delete(any());
    }

    // ==================== deleteReply ====================

    @Test
    @DisplayName("DS15 - deleteReply: should delete when current user is owner")
    void deleteReply_shouldDelete_whenOwner() {
        // Given
        MockHelper.mockSecurityContext("student");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionReplyRepository.findById(reply.getId()))
                .thenReturn(Optional.of(reply));

        // When
        discussionService.deleteReply(reply.getId());

        // Then
        verify(discussionReplyRepository).delete(reply);
    }

    @Test
    @DisplayName("DS16 - deleteReply: should delete when current user is admin")
    void deleteReply_shouldDelete_whenAdmin() {
        // Given
        MockHelper.mockSecurityContext("admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(discussionReplyRepository.findById(reply.getId()))
                .thenReturn(Optional.of(reply));

        // When
        discussionService.deleteReply(reply.getId());

        // Then
        verify(discussionReplyRepository).delete(reply);
    }

    @Test
    @DisplayName("DS17 - deleteReply: should throw USER_NOT_EXISTED when current user not found")
    void deleteReply_shouldThrowUserNotExisted_whenCurrentUserNotFound() {
        // Given
        MockHelper.mockSecurityContext("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteReply(reply.getId()));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        verify(discussionReplyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DS18 - deleteReply: should throw DISCUSSION_REPLY_NOT_FOUND when reply missing")
    void deleteReply_shouldThrowDiscussionReplyNotFound_whenReplyMissing() {
        // Given
        MockHelper.mockSecurityContext("student");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(studentUser));
        when(discussionReplyRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteReply(999L));
        assertEquals(ErrorCode.DISCUSSION_REPLY_NOT_FOUND, ex.getErrorCode());
        verify(discussionReplyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DS19 - deleteReply: should throw UNAUTHORIZED when not owner and not admin")
    void deleteReply_shouldThrowUnauthorized_whenNotOwnerAndNotAdmin() {
        // Given
        MockHelper.mockSecurityContext("another");
        User anotherUser = User.builder()
                .userId("another-1")
                .username("another")
                .role(studentRole)
                .build();

        when(userRepository.findByUsername("another")).thenReturn(Optional.of(anotherUser));
        when(discussionReplyRepository.findById(reply.getId()))
                .thenReturn(Optional.of(reply));

        // When & Then
        AppException ex = assertThrows(AppException.class,
                () -> discussionService.deleteReply(reply.getId()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(discussionReplyRepository, never()).delete(any());
    }
}
