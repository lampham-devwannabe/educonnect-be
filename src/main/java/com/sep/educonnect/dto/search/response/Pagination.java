package com.sep.educonnect.dto.search.response;

public record Pagination(int page, int size, long totalElements, int totalPages) {
}
