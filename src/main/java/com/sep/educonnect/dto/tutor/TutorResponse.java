package com.sep.educonnect.dto.tutor;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TutorResponse {
    Long id;
    String tutorName;
    String avatar;
    BigDecimal hourlyRate;
    String currencyCode;
    Double rating;
    Integer studentCount;
    Integer reviewCount;
    // This will be populated with the localized description/bio based on current locale
    String desc;
    String bio;
}