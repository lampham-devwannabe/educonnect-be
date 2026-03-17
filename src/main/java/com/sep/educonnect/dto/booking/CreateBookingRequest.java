package com.sep.educonnect.dto.booking;

import com.sep.educonnect.dto.tutor.request.WeeklySchedule;
import com.sep.educonnect.enums.GroupType;
import com.sep.educonnect.enums.RegistrationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBookingRequest {
    @NotNull(message = "{error.booking.syllabusId.required}")
    Long courseId;

    @NotNull(message = "{error.booking.groupType.required}")
    GroupType groupType;

    @NotNull(message = "{error.booking.registrationType.required}")
    RegistrationType registrationType;

    @NotNull(message = "{error.booking.lessons.min}")
    @Min(value = 0, message = "{error.booking.lessons.min}")
    Integer lessons;
    Set<String> memberIds;

    List<WeeklySchedule> weeklySchedules;
}
