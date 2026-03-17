package com.sep.educonnect.dto.syllabus.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusCreationRequest {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @Size(max = 50, message = "Level must not exceed 50 characters")
    private String level;

    @Size(max = 100, message = "Target must not exceed 100 characters")
    private String target;

    @NotBlank(message = "Syllabus name is required")
    @Size(min = 2, max = 200, message = "Syllabus name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;
}
