package com.sep.educonnect.dto.tutor.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateDocumentRequest {
    Long documentId;
    String fileId;
    String documentType;
}
