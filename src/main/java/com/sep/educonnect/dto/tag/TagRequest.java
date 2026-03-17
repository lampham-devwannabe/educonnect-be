package com.sep.educonnect.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TagRequest {
    @NotBlank(message = "Tag name in Vietnamese is required")
    @Size(max = 50, message = "Tag name in Vietnamese must not exceed 50 characters")
    String nameVi;

    @NotBlank(message = "Tag name in English is required")
    @Size(max = 50, message = "Tag name in English must not exceed 50 characters")
    String nameEn;
}
