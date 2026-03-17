package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.subject.response.SubjectSyllabusResponse;
import com.sep.educonnect.dto.syllabus.request.SyllabusCreationRequest;
import com.sep.educonnect.dto.syllabus.request.SyllabusUpdateRequest;
import com.sep.educonnect.dto.syllabus.response.SyllabusResponse;
import com.sep.educonnect.service.SyllabusService;
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
@RequestMapping("/api/syllabus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SyllabusController {
    SyllabusService syllabusService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<SyllabusResponse> createSyllabus(@RequestBody @Valid SyllabusCreationRequest request,
                                                 @RequestHeader(name = "Accept-Language", defaultValue = "vi") String language) {
        log.info("Creating syllabus with name: {}", request.getName());
        return ApiResponse.<SyllabusResponse>builder()
                .result(syllabusService.createSyllabus(request, language))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<Page<SyllabusResponse>> getAllSyllabus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "syllabusId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {
        log.info("Fetching all active syllabus with pagination");
        return ApiResponse.<Page<SyllabusResponse>>builder()
                .result(syllabusService.getAllActiveSyllabus(page, size, sortBy, direction, name, level, status))
                .build();
    }


    @GetMapping("/{syllabusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<SyllabusResponse> getSyllabus(@PathVariable Long syllabusId) {
        log.info("Fetching syllabus with ID: {}", syllabusId);
        return ApiResponse.<SyllabusResponse>builder()
                .result(syllabusService.getSyllabusById(syllabusId))
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<List<SyllabusResponse>> getSyllabusBySubject(@PathVariable Long subjectId) {
        log.info("Fetching syllabus for subject ID: {}", subjectId);
        return ApiResponse.<List<SyllabusResponse>>builder()
                .result(syllabusService.getSyllabusBySubjectId(subjectId))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<List<SyllabusResponse>> searchSyllabus(@RequestParam String name) {
        log.info("Searching syllabus with name containing: {}", name);
        return ApiResponse.<List<SyllabusResponse>>builder()
                .result(syllabusService.searchSyllabusByName(name))
                .build();
    }

    @GetMapping("/by-name/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<SyllabusResponse> getSyllabusByName(@PathVariable String name) {
        log.info("Fetching syllabus with name: {}", name);
        return ApiResponse.<SyllabusResponse>builder()
                .result(syllabusService.getSyllabusByName(name))
                .build();
    }

    @GetMapping("/level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<List<SyllabusResponse>> getSyllabusByLevel(@PathVariable String level) {
        log.info("Fetching syllabus with level: {}", level);
        return ApiResponse.<List<SyllabusResponse>>builder()
                .result(syllabusService.getSyllabusByLevel(level))
                .build();
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<List<SyllabusResponse>> getSyllabusByStatus(@PathVariable String status) {
        log.info("Fetching syllabus with status: {}", status);
        return ApiResponse.<List<SyllabusResponse>>builder()
                .result(syllabusService.getSyllabusByStatus(status))
                .build();
    }

    @GetMapping("/subject/{subjectId}/level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<List<SyllabusResponse>> getSyllabusBySubjectAndLevel(
            @PathVariable Long subjectId,
            @PathVariable String level) {
        log.info("Fetching syllabus for subject ID: {} and level: {}", subjectId, level);
        return ApiResponse.<List<SyllabusResponse>>builder()
                .result(syllabusService.getSyllabusBySubjectAndLevel(subjectId, level))
                .build();
    }

    @PutMapping("/{syllabusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<SyllabusResponse> updateSyllabus(
            @PathVariable Long syllabusId,
            @RequestBody @Valid SyllabusUpdateRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "vi") String language) {
        log.info("Updating syllabus with ID: {}", syllabusId);
        return ApiResponse.<SyllabusResponse>builder()
                .result(syllabusService.updateSyllabus(syllabusId, request, language))
                .build();
    }

    @DeleteMapping("/{syllabusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<String> deleteSyllabus(@PathVariable Long syllabusId) {
        log.info("Deleting syllabus with ID: {}", syllabusId);
        syllabusService.deleteSyllabus(syllabusId);
        return ApiResponse.<String>builder()
                .result("Syllabus has been deleted successfully")
                .build();
    }

    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<Boolean> checkSyllabusExists(@RequestParam String name) {
        log.info("Checking if syllabus exists with name: {}", name);
        return ApiResponse.<Boolean>builder()
                .result(syllabusService.existsByName(name))
                .build();
    }

    @GetMapping("/count/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ApiResponse<Long> countSyllabusBySubject(@PathVariable Long subjectId) {
        log.info("Counting syllabus for subject ID: {}", subjectId);
        return ApiResponse.<Long>builder()
                .result(syllabusService.countSyllabusBySubject(subjectId))
                .build();
    }

    @GetMapping("/and-subject")
    ApiResponse<Page<SubjectSyllabusResponse>> getAllSyllabusWithSubject(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "subjectId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("Fetching all syllabus with their associated subjects with pagination");
        return ApiResponse.<Page<SubjectSyllabusResponse>>builder()
                .result(syllabusService.getAllSubjectsWithSyllabuses(page, size, sortBy, direction))
                .build();
    }

}
