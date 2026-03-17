package com.sep.educonnect.dto.tutor.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateScheduleChangeRequest {
    @NotNull(message = "Session ID is required")
    Long sessionId;

    @NotNull(message = "Old date is required")
    LocalDate oldDate;

    @NotNull(message = "New date is required")
    LocalDate newDate;

    @NotNull(message = "New slot is required")
    Integer newSlot;

    @Size(max = 1000, message = "Content must not exceed 1000 characters")
    String content;
}
