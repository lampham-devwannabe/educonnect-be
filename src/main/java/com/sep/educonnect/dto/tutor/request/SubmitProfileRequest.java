package com.sep.educonnect.dto.tutor.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitProfileRequest {
    String experience;
    BigDecimal hourlyRate;
    String currencyCode;
    String bio;
    String desc;
    Set<Long> tags;
    Set<Long> subjects;
}
