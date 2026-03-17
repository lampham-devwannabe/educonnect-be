package com.sep.educonnect.dto.tutor.response;

import com.sep.educonnect.enums.DocumentStatus;
import com.sep.educonnect.enums.DocumentType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileDocumentResponse {
    Long documentId;
    DocumentType documentType;
    String fileName;
    DocumentStatus status;
    LocalDateTime uploadedAt;
    LocalDateTime verifiedAt;
    String rejectionReason;
}
