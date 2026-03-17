package com.sep.educonnect.dto.subject.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubjectSyllabusResponse {
    private Long subjectId;
    private String name;
    private List<SyllabusDto> syllabuses;

    @Data
    @Builder
    public static class SyllabusDto {
        private Long syllabusId;
        private String level;
    }
}
