package com.sep.educonnect.dto.verification;

import com.sep.educonnect.dto.comment.ReviewCommentResponse;
import com.sep.educonnect.dto.tutor.response.ProfileDocumentResponse;
import com.sep.educonnect.dto.tutor.response.TutorProfileResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.enums.ProcessPriority;
import com.sep.educonnect.enums.VerificationStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationProcessDetailResponse {
    Long processId;
    TutorProfileResponse profile;
    VerificationStage currentStage;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    UserResponse reviewer;
    ProcessPriority priority;
    LocalDateTime estimatedCompletionDate;
    List<ProfileDocumentResponse> documents;
    List<ReviewCommentResponse> comments;
}
