package com.sep.educonnect.dto.booking;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTrialSessionsResponse {
    private Long sessionId;
    private Integer sessionNumber;
    private String topic;
    private LocalDate sessionDate;
    private String dayOfWeek;
    private Integer slotNumber;
    private String timeRange;
}

