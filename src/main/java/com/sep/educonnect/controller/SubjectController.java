package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.subject.request.SubjectCreationRequest;
import com.sep.educonnect.dto.subject.request.SubjectUpdateRequest;
import com.sep.educonnect.dto.subject.response.SubjectResponse;
import com.sep.educonnect.service.I18nService;
import com.sep.educonnect.service.SubjectService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubjectController {
    SubjectService subjectService;
    I18nService i18nService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SubjectResponse> createSubject(
            @RequestBody @Valid SubjectCreationRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "vi") String language) {
        log.info("Creating subject with name: {}", request.getSubjectName());
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.createSubject(request, language))
                .build();
    }

    @GetMapping
    ApiResponse<Page<SubjectResponse>> getAllActiveSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "subjectId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String name) {
        log.info("Fetching all active subjects with pagination");
        return ApiResponse.<Page<SubjectResponse>>builder()
                .result(subjectService.getAllActiveSubjects(page, size, sortBy, direction, name))
                .build();
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SubjectResponse> getSubject(@PathVariable Long subjectId) {
        log.info("Fetching subject with ID: {}", subjectId);
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.getSubjectById(subjectId))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<SubjectResponse>> searchSubjects(@RequestParam String name) {
        log.info("Searching subjects with name containing: {}", name);
        return ApiResponse.<List<SubjectResponse>>builder()
                .result(subjectService.searchSubjectsByName(name))
                .build();
    }

    @GetMapping("/by-name/{subjectName}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SubjectResponse> getSubjectByName(@PathVariable String subjectName) {
        log.info("Fetching subject with name: {}", subjectName);
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.getSubjectByName(subjectName))
                .build();
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<SubjectResponse> updateSubject(
            @PathVariable Long subjectId,
            @RequestBody @Valid SubjectUpdateRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "vi") String language) {
        log.info("Updating subject with ID: {}", subjectId);
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.updateSubject(subjectId, request, language))
                .build();
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<String> deleteSubject(@PathVariable Long subjectId) {
        log.info("Deleting subject with ID: {}", subjectId);
        subjectService.deleteSubject(subjectId);
        return ApiResponse.<String>builder().result(i18nService.msg("msg.subject.delete")).build();
    }

    @GetMapping("/exists")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Boolean> checkSubjectExists(@RequestParam String name) {
        log.info("Checking if subject exists with name: {}", name);
        return ApiResponse.<Boolean>builder().result(subjectService.existsByName(name)).build();
    }
}
