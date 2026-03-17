package com.sep.educonnect.dto.module;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRequest {
    private Long syllabusId;
    private String title;
    private Integer orderNumber;
    private String status;
}
