package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.lesson.LessonRequest;
import com.sep.educonnect.dto.lesson.LessonResponse;
import com.sep.educonnect.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public ApiResponse<LessonResponse> create(@RequestBody LessonRequest req) {
        return ApiResponse.<LessonResponse>builder()
                .result(lessonService.create(req))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> get(@PathVariable Long id) {
        return ApiResponse.<LessonResponse>builder()
                .result(lessonService.getById(id))
                .build();
    }

    @GetMapping("/modules/{moduleId}")
    public ApiResponse<Page<LessonResponse>> listByModule(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.<Page<LessonResponse>>builder()
                .result(lessonService.getByModule(moduleId, page, size, sortBy, direction))
                .build();
    }


    @PutMapping("/{id}")
    public ApiResponse<LessonResponse> update(@PathVariable Long id,
                                              @RequestBody LessonRequest req) {
        return ApiResponse.<LessonResponse>builder()
                .result(lessonService.update(id, req))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ApiResponse.<Void>builder().build();
    }

    @PutMapping("/{id}/reorder")
    public ApiResponse<Void> reorder(@PathVariable Long id, @RequestParam Integer orderNumber) {
        lessonService.reorder(id, orderNumber);
        return ApiResponse.<Void>builder().build();
    }
}
