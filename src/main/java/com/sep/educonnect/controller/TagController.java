package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.tag.TagRequest;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.service.TagService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class TagController {
    TagService tagService;

    @PostMapping
    ApiResponse<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.<TagResponse>builder().result(tagService.createTag(request)).build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'STUDENT', 'TUTOR')")
    ApiResponse<Page<TagResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nameEn") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String name) {
        return ApiResponse.<Page<TagResponse>>builder()
                .result(tagService.getAllTags(page, size, sortBy, direction, name))
                .build();
    }

    @GetMapping("/{tagId}")
    ApiResponse<TagResponse> getById(@PathVariable Long tagId) {
        return ApiResponse.<TagResponse>builder().result(tagService.getTagById(tagId)).build();
    }

    @PutMapping("/{tagId}")
    ApiResponse<TagResponse> update(
            @PathVariable Long tagId, @Valid @RequestBody TagRequest request) {
        return ApiResponse.<TagResponse>builder().result(tagService.updateTag(tagId, request)).build();
    }

    @DeleteMapping("/{tagId}")
    ApiResponse<Void> delete(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.<Void>builder().build();
    }
}
