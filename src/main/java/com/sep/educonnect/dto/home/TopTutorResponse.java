package com.sep.educonnect.dto.home;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopTutorResponse {
    Long id;
    String tutorName;
    String avatar;
    BigDecimal hourlyRate;
    String currencyCode;
    Double rating;
    Integer studentCount;
    Integer reviewCount;
}

