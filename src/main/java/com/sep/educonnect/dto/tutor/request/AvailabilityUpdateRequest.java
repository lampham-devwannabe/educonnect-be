package com.sep.educonnect.dto.tutor.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailabilityUpdateRequest {
    Map<Integer, List<Integer>> weekSchedule;
}
