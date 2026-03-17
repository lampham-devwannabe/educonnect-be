package com.sep.educonnect.dto.exception.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExceptionListResponse {
    List<ExceptionResponse> exceptions;
    Long totalCount;
    Long pendingCount;
    Long approvedCount;
    Long rejectedCount;
}
