package com.sep.educonnect.dto.admin.response;

import com.sep.educonnect.entity.TutorDocument;
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
public class TutorDocumentResponse {

    Long id;

    Long profile;

    DocumentType type;

    String fileName;

    DocumentStatus status;

    String rejectionReason;

    LocalDateTime uploadedAt;

    LocalDateTime verifiedAt;

    String verifiedBy;

    public static TutorDocumentResponse fromEntity(TutorDocument td) {
        return new TutorDocumentResponse(
                td.getId(),
                td.getProfile().getId(),
                td.getType(),
                td.getFileName(),
                td.getStatus(),
                td.getRejectionReason(),
                td.getUploadedAt(),
                td.getVerifiedAt(),
                td.getVerifiedBy().getFirstName() + " " + td.getVerifiedBy().getLastName()
        );
    }
}
