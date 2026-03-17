package com.sep.educonnect.dto.verification;

import com.sep.educonnect.enums.ProcessPriority;
import com.sep.educonnect.enums.VerificationStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationStatisticsResponse {
    Long totalPending;
    Long totalApproved;
    Long totalRejected;
    Long totalInReview;
    Long totalRevisionRequired;
    Map<VerificationStage, Long> byStage;
    Map<ProcessPriority, Long> byPriority;
    Double averageProcessingTimeHours;
    LocalDate periodStart;
    LocalDate periodEnd;
}
