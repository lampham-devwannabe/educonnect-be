package com.sep.educonnect.dto.admin.response;

import com.sep.educonnect.entity.VerificationProcess;
import com.sep.educonnect.enums.ProcessPriority;
import com.sep.educonnect.enums.VerificationStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationProcessResponse {
    Long processId;
    VerificationStage currentStage;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    ProcessPriority priority;
    LocalDateTime estimatedCompletionDate;

    String reviewerId;
    Long profileId;

    public static VerificationProcessResponse fromEntity(VerificationProcess vp) {
        return new VerificationProcessResponse(
                vp.getProcessId(),
                vp.getCurrentStage(),
                vp.getStartedAt(),
                vp.getCompletedAt(),
                vp.getPriority(),
                vp.getEstimatedCompletionDate(),
                vp.getReviewer() != null ? vp.getReviewer().getUserId() : null,
                vp.getProfile() != null ? vp.getProfile().getId() : null
        );
    }
}
