package com.sep.educonnect.dto.tag;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TagImageResponse {
    Long id;
    String nameEn;
    String nameVi;
    String imageUrl;
}
