package com.sep.educonnect.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDTO {
    Long subjectId;
    // populated with the localized name based on current locale
    String nameVi;
    String nameEn;
    String createdBy;
    LocalDateTime createdAt;
    String modifiedBy;
    LocalDateTime modifiedAt;
}
