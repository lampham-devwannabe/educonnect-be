package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.search.request.SearchTutorParams;
import com.sep.educonnect.dto.search.response.SearchTutorResponse;
import com.sep.educonnect.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/tutors")
    public ApiResponse<SearchTutorResponse> searchTutors(@RequestBody SearchTutorParams params) {
        SearchTutorResponse res = searchService.searchTutor(params);
        return ApiResponse.<SearchTutorResponse>builder()
                .code(1000)
                .result(res)
                .build();
    }

    @GetMapping("/tutors/by-subject")
    public ApiResponse<SearchTutorResponse> searchTutorsBySubject(
            @RequestParam String subject,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        SearchTutorResponse res = searchService.searchTutorsBySubject(subject, page, size);
        return ApiResponse.<SearchTutorResponse>builder()
                .code(1000)
                .result(res)
                .build();
    }
}
