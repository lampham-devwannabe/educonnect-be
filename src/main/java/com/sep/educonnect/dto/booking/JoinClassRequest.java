package com.sep.educonnect.dto.booking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JoinClassRequest {
    Integer classId;
    @NotNull(message = "{error.booking.lessons.min}")
    @Positive(message = "{error.booking.lessons.min}")
    Integer lessons;
}
