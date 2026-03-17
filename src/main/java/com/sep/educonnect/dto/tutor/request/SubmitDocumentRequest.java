package com.sep.educonnect.dto.tutor.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitDocumentRequest {
    String fileId;
    String documentType;
}
