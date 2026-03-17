package com.sep.educonnect.dto.tutor.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TutorGeneralResponse {
    String id;
    String tutorName;
    String avatar;
    String email;
    BigDecimal hourlyRate;
}
