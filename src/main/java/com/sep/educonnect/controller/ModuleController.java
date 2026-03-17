package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.module.ModuleRequest;
import com.sep.educonnect.dto.module.ModuleResponse;
import com.sep.educonnect.service.ModuleService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleController {
    ModuleService moduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ModuleResponse> create(@RequestBody ModuleRequest req) {
        return ApiResponse.<ModuleResponse>builder().result(moduleService.create(req)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ModuleResponse> get(@PathVariable Long id) {
        return ApiResponse.<ModuleResponse>builder()
                .result(moduleService.getResponseById(id))
                .build();
    }

    @GetMapping("/syllabuses/{syllabusId}")
    public ApiResponse<Page<ModuleResponse>> listBySyllabus(
            @PathVariable Long syllabusId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.<Page<ModuleResponse>>builder()
                .result(
                        moduleService.getModulesBySyllabus(
                                syllabusId, page, size, sortBy, direction))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ModuleResponse> update(
            @PathVariable Long id, @RequestBody ModuleRequest req) {
        return ApiResponse.<ModuleResponse>builder().result(moduleService.update(id, req)).build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        moduleService.delete(id);
        return ApiResponse.<Void>builder().build();
    }

    @PutMapping("/{id}/reorder")
    public ApiResponse<Void> reorder(@PathVariable Long id, @RequestParam Integer orderNumber) {
        moduleService.reorder(id, orderNumber);
        return ApiResponse.<Void>builder().build();
    }
}
