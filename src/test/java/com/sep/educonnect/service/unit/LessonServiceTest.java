package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.lesson.LessonRequest;
import com.sep.educonnect.dto.lesson.LessonResponse;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.LessonProgress;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.LessonMapper;
import com.sep.educonnect.repository.LessonProgressRepository;
import com.sep.educonnect.repository.LessonRepository;
import com.sep.educonnect.service.LessonService;
import com.sep.educonnect.service.ProgressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonService Unit Tests")
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private LessonService lessonService;

    @Test
    @DisplayName("Should get lesson by ID successfully")
    void should_getById_successfully() {
        // Given
        Long lessonId = 1L;
        Lesson lesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Introduction to Java")
                .orderNumber(1)
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Introduction to Java")
                .orderNumber(1)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonMapper.toResponse(lesson)).thenReturn(expectedResponse);

        // When
        LessonResponse result = lessonService.getById(lessonId);

        // Then
        assertNotNull(result);
        assertEquals(lessonId, result.getLessonId());
        assertEquals("Introduction to Java", result.getTitle());
        verify(lessonRepository).findById(lessonId);
        verify(lessonMapper).toResponse(lesson);
    }

    @Test
    @DisplayName("Should throw AppException when lesson not found for getById")
    void should_throwException_when_lessonNotFoundForGetById() {
        // Given
        Long lessonId = 999L;
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            lessonService.getById(lessonId);
        });

        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).findById(lessonId);
        verify(lessonMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should get lessons by module with pagination and ascending sort")
    void should_getByModule_withAscendingSort() {
        // Given
        Long moduleId = 1L;
        int page = 0;
        int size = 10;
        String sortBy = "orderNumber";
        String direction = "asc";

        Lesson lesson1 = Lesson.builder()
                .lessonId(1L)
                .moduleId(moduleId)
                .title("Lesson 1")
                .orderNumber(1)
                .build();

        Lesson lesson2 = Lesson.builder()
                .lessonId(2L)
                .moduleId(moduleId)
                .title("Lesson 2")
                .orderNumber(2)
                .build();

        List<Lesson> lessons = List.of(lesson1, lesson2);
        Page<Lesson> lessonPage = new PageImpl<>(lessons, PageRequest.of(page, size), lessons.size());

        LessonResponse response1 = LessonResponse.builder()
                .lessonId(1L)
                .moduleId(moduleId)
                .title("Lesson 1")
                .orderNumber(1)
                .build();

        LessonResponse response2 = LessonResponse.builder()
                .lessonId(2L)
                .moduleId(moduleId)
                .title("Lesson 2")
                .orderNumber(2)
                .build();

        when(lessonRepository.findByModuleIdWithPaging(eq(moduleId), any(Pageable.class))).thenReturn(lessonPage);
        when(lessonMapper.toResponse(any(Lesson.class))).thenReturn(response1, response2);

        // When
        Page<LessonResponse> result = lessonService.getByModule(moduleId, page, size, sortBy, direction);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getLessonId());
        assertEquals(2L, result.getContent().get(1).getLessonId());
        verify(lessonRepository).findByModuleIdWithPaging(eq(moduleId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get lessons by module with pagination and descending sort")
    void should_getByModule_withDescendingSort() {
        // Given
        Long moduleId = 1L;
        int page = 0;
        int size = 10;
        String sortBy = "orderNumber";
        String direction = "desc";

        Lesson lesson1 = Lesson.builder()
                .lessonId(1L)
                .moduleId(moduleId)
                .title("Lesson 1")
                .orderNumber(1)
                .build();

        Lesson lesson2 = Lesson.builder()
                .lessonId(2L)
                .moduleId(moduleId)
                .title("Lesson 2")
                .orderNumber(2)
                .build();

        List<Lesson> lessons = List.of(lesson1, lesson2);
        Page<Lesson> lessonPage = new PageImpl<>(lessons, PageRequest.of(page, size), lessons.size());

        LessonResponse response1 = LessonResponse.builder()
                .lessonId(1L)
                .moduleId(moduleId)
                .title("Lesson 1")
                .orderNumber(1)
                .build();

        LessonResponse response2 = LessonResponse.builder()
                .lessonId(2L)
                .moduleId(moduleId)
                .title("Lesson 2")
                .orderNumber(2)
                .build();

        when(lessonRepository.findByModuleIdWithPaging(eq(moduleId), any(Pageable.class))).thenReturn(lessonPage);
        when(lessonMapper.toResponse(any(Lesson.class))).thenReturn(response1, response2);

        // When
        Page<LessonResponse> result = lessonService.getByModule(moduleId, page, size, sortBy, direction);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(lessonRepository).findByModuleIdWithPaging(eq(moduleId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should create lesson with orderNumber provided")
    void should_createLesson_withOrderNumberProvided() {
        // Given
        LessonRequest request = LessonRequest.builder()
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(5)
                .status("DRAFT")
                .build();

        Lesson lesson = Lesson.builder()
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(5)
                .status("DRAFT")
                .build();

        Lesson savedLesson = Lesson.builder()
                .lessonId(1L)
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(5)
                .status("DRAFT")
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .lessonId(1L)
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(5)
                .status("DRAFT")
                .build();

        when(lessonMapper.toEntity(request)).thenReturn(lesson);
        when(lessonRepository.save(lesson)).thenReturn(savedLesson);
        when(lessonMapper.toResponse(savedLesson)).thenReturn(expectedResponse);

        // When
        LessonResponse result = lessonService.create(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getLessonId());
        assertEquals("New Lesson", result.getTitle());
        assertEquals(5, result.getOrderNumber());
        verify(lessonMapper).toEntity(request);
        verify(lessonRepository).save(lesson);
        verify(lessonRepository, never()).findByModuleIdOrderByOrderNumberAsc(anyLong());
        verify(lessonMapper).toResponse(savedLesson);
    }

    @Test
    @DisplayName("Should create lesson with auto-generated orderNumber when orderNumber is null and moduleId exists")
    void should_createLesson_withAutoGeneratedOrderNumber_whenModuleIdExists() {
        // Given
        LessonRequest request = LessonRequest.builder()
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(null)
                .build();

        Lesson lesson = Lesson.builder()
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(null)
                .build();

        Lesson existingLesson1 = Lesson.builder()
                .lessonId(1L)
                .moduleId(1L)
                .orderNumber(1)
                .build();

        Lesson existingLesson2 = Lesson.builder()
                .lessonId(2L)
                .moduleId(1L)
                .orderNumber(2)
                .build();

        List<Lesson> existingLessons = List.of(existingLesson1, existingLesson2);

        Lesson savedLesson = Lesson.builder()
                .lessonId(3L)
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(3)
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .lessonId(3L)
                .moduleId(1L)
                .title("New Lesson")
                .orderNumber(3)
                .build();

        when(lessonMapper.toEntity(request)).thenReturn(lesson);
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(1L)).thenReturn(existingLessons);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
        when(lessonMapper.toResponse(savedLesson)).thenReturn(expectedResponse);

        // When
        LessonResponse result = lessonService.create(request);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getLessonId());
        assertEquals(3, result.getOrderNumber());
        
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCaptor.capture());
        assertEquals(3, lessonCaptor.getValue().getOrderNumber());
    }

    @Test
    @DisplayName("Should create lesson with orderNumber 1 when orderNumber is null and moduleId is null")
    void should_createLesson_withOrderNumber1_whenModuleIdIsNull() {
        // Given
        LessonRequest request = LessonRequest.builder()
                .moduleId(null)
                .title("First Lesson")
                .orderNumber(null)
                .build();

        Lesson lesson = Lesson.builder()
                .moduleId(null)
                .title("First Lesson")
                .orderNumber(null)
                .build();

        Lesson savedLesson = Lesson.builder()
                .lessonId(1L)
                .moduleId(null)
                .title("First Lesson")
                .orderNumber(1)
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .lessonId(1L)
                .moduleId(null)
                .title("First Lesson")
                .orderNumber(1)
                .build();

        when(lessonMapper.toEntity(request)).thenReturn(lesson);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
        when(lessonMapper.toResponse(savedLesson)).thenReturn(expectedResponse);

        // When
        LessonResponse result = lessonService.create(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrderNumber());
        
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCaptor.capture());
        assertEquals(1, lessonCaptor.getValue().getOrderNumber());
        verify(lessonRepository, never()).findByModuleIdOrderByOrderNumberAsc(anyLong());
    }

    @Test
    @DisplayName("Should update lesson successfully")
    void should_updateLesson_successfully() {
        // Given
        Long lessonId = 1L;
        LessonRequest request = LessonRequest.builder()
                .title("Updated Lesson")
                .orderNumber(2)
                .status("PUBLISHED")
                .build();

        Lesson existingLesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Original Lesson")
                .orderNumber(1)
                .status("DRAFT")
                .build();

        Lesson updatedLesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Updated Lesson")
                .orderNumber(2)
                .status("PUBLISHED")
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Updated Lesson")
                .orderNumber(2)
                .status("PUBLISHED")
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        doNothing().when(lessonMapper).updateEntity(existingLesson, request);
        when(lessonRepository.save(existingLesson)).thenReturn(updatedLesson);
        when(lessonMapper.toResponse(updatedLesson)).thenReturn(expectedResponse);

        // When
        LessonResponse result = lessonService.update(lessonId, request);

        // Then
        assertNotNull(result);
        assertEquals(lessonId, result.getLessonId());
        assertEquals("Updated Lesson", result.getTitle());
        assertEquals(2, result.getOrderNumber());
        assertEquals("PUBLISHED", result.getStatus());
        verify(lessonRepository).findById(lessonId);
        verify(lessonMapper).updateEntity(existingLesson, request);
        verify(lessonRepository).save(existingLesson);
        verify(lessonMapper).toResponse(updatedLesson);
    }

    @Test
    @DisplayName("Should throw AppException when lesson not found for update")
    void should_throwException_when_lessonNotFoundForUpdate() {
        // Given
        Long lessonId = 999L;
        LessonRequest request = LessonRequest.builder()
                .title("Updated Lesson")
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            lessonService.update(lessonId, request);
        });

        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).findById(lessonId);
        verify(lessonMapper, never()).updateEntity(any(), any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete lesson successfully")
    void should_deleteLesson_successfully() {
        // Given
        Long lessonId = 1L;
        Lesson lesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Lesson to Delete")
                .orderNumber(1)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByLesson_LessonId(lessonId)).thenReturn(List.of());
        doNothing().when(lessonRepository).delete(lesson);

        // When
        lessonService.delete(lessonId);

        // Then
        verify(lessonRepository).findById(lessonId);
        verify(lessonProgressRepository).findByLesson_LessonId(lessonId);
        verify(lessonRepository).delete(lesson);
    }

    @Test
    @DisplayName("Should delete lesson with lesson progress successfully")
    void should_deleteLesson_withLessonProgress_successfully() {
        // Given
        Long lessonId = 1L;
        Long enrollmentId = 100L;
        Lesson lesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Lesson to Delete")
                .orderNumber(1)
                .build();

        com.sep.educonnect.entity.ClassEnrollment enrollment = com.sep.educonnect.entity.ClassEnrollment.builder()
                .id(enrollmentId)
                .build();

        com.sep.educonnect.entity.CourseProgress courseProgress = com.sep.educonnect.entity.CourseProgress.builder()
                .id(1L)
                .enrollment(enrollment)
                .build();

        LessonProgress lessonProgress = LessonProgress.builder()
                .id(1L)
                .lesson(lesson)
                .courseProgress(courseProgress)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByLesson_LessonId(lessonId)).thenReturn(List.of(lessonProgress));
        doNothing().when(lessonProgressRepository).deleteAll(anyList());
        when(progressService.recalculateCourseProgress(enrollmentId)).thenReturn(courseProgress);
        doNothing().when(lessonRepository).delete(lesson);

        // When
        lessonService.delete(lessonId);

        // Then
        verify(lessonRepository).findById(lessonId);
        verify(lessonProgressRepository).findByLesson_LessonId(lessonId);
        verify(lessonProgressRepository).deleteAll(anyList());
        verify(progressService).recalculateCourseProgress(enrollmentId);
        verify(lessonRepository).delete(lesson);
    }

    @Test
    @DisplayName("Should throw AppException when lesson not found for delete")
    void should_throwException_when_lessonNotFoundForDelete() {
        // Given
        Long lessonId = 999L;
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            lessonService.delete(lessonId);
        });

        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).findById(lessonId);
        verify(lessonRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should reorder lesson successfully")
    void should_reorderLesson_successfully() {
        // Given
        Long lessonId = 1L;
        Integer newOrderNumber = 5;
        Lesson lesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Lesson")
                .orderNumber(1)
                .build();

        Lesson reorderedLesson = Lesson.builder()
                .lessonId(lessonId)
                .moduleId(1L)
                .title("Lesson")
                .orderNumber(newOrderNumber)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(reorderedLesson);

        // When
        lessonService.reorder(lessonId, newOrderNumber);

        // Then
        verify(lessonRepository).findById(lessonId);
        verify(lessonRepository).save(lesson);
        assertEquals(newOrderNumber, lesson.getOrderNumber());
    }

    @Test
    @DisplayName("Should throw AppException when lesson not found for reorder")
    void should_throwException_when_lessonNotFoundForReorder() {
        // Given
        Long lessonId = 999L;
        Integer newOrderNumber = 5;
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            lessonService.reorder(lessonId, newOrderNumber);
        });

        assertEquals(ErrorCode.LESSON_NOT_EXISTED, exception.getErrorCode());
        verify(lessonRepository).findById(lessonId);
        verify(lessonRepository, never()).save(any());
    }
}

