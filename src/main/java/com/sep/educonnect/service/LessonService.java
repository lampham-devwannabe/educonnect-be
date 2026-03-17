package com.sep.educonnect.service;

import com.sep.educonnect.dto.lesson.LessonRequest;
import com.sep.educonnect.dto.lesson.LessonResponse;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.LessonProgress;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.LessonMapper;
import com.sep.educonnect.repository.LessonProgressRepository;
import com.sep.educonnect.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final LessonProgressRepository lessonProgressRepository;
    private final ProgressService progressService;

    public LessonResponse getById(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
        return lessonMapper.toResponse(lesson);
    }

    public Page<LessonResponse> getByModule(Long moduleId, int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Lesson> lessonPage = lessonRepository.findByModuleIdWithPaging(moduleId, pageable);
        return lessonPage.map(lessonMapper::toResponse);
    }

    @Transactional
    public LessonResponse create(LessonRequest request) {
        Lesson lesson = lessonMapper.toEntity(request);
        if (lesson.getOrderNumber() == null) {
            int nextOrder = (lesson.getModuleId() != null)
                    ? lessonRepository.findByModuleIdOrderByOrderNumberAsc(lesson.getModuleId()).size() + 1
                    : 1;
            lesson.setOrderNumber(nextOrder);
        }
        Lesson saved = lessonRepository.save(lesson);

        return lessonMapper.toResponse(saved);
    }

    @Transactional
    public LessonResponse update(Long id, LessonRequest request) {
        Lesson existing = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
        lessonMapper.updateEntity(existing, request);
        Lesson saved = lessonRepository.save(existing);

        return lessonMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
        
        // Tìm tất cả LessonProgress liên quan đến lesson này
        List<LessonProgress> lessonProgresses = lessonProgressRepository.findByLesson_LessonId(id);
        
        if (!lessonProgresses.isEmpty()) {
            // Lấy danh sách các enrollmentId duy nhất để recalculate course progress
            Set<Long> enrollmentIds = lessonProgresses.stream()
                    .map(lp -> lp.getCourseProgress().getEnrollment().getId())
                    .collect(Collectors.toSet());
            
            // Xóa tất cả LessonProgress liên quan
            lessonProgressRepository.deleteAll(lessonProgresses);
            
            // Recalculate course progress cho các enrollment bị ảnh hưởng
            for (Long enrollmentId : enrollmentIds) {
                try {
                    progressService.recalculateCourseProgress(enrollmentId);
                } catch (Exception e) {
                    log.warn("Failed to recalculate course progress for enrollment {} after deleting lesson {}: {}", 
                            enrollmentId, id, e.getMessage());
                }
            }
        }
        
        // Sau đó mới xóa lesson
        lessonRepository.delete(lesson);
    }

    @Transactional
    public void reorder(Long lessonId, Integer newOrderNumber) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_EXISTED));
        lesson.setOrderNumber(newOrderNumber);
        lessonRepository.save(lesson);
    }
}
