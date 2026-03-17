package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.exception.response.ExceptionResponse;
import com.sep.educonnect.entity.TutorAvailabilityException;
import com.sep.educonnect.enums.ExceptionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ExceptionMapper {

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "sessionDate", source = "session.sessionDate")
    @Mapping(target = "sessionStartTime", source = "session.startTime")
    @Mapping(target = "sessionEndTime", source = "session.endTime")
    @Mapping(target = "sessionTopic", source = "session.topic")
    @Mapping(target = "sessionNumber", source = "session.sessionNumber")
    @Mapping(target = "className", source = "session.tutorClass.title")
    @Mapping(target = "dayOfWeek", expression = "java(getDayOfWeek(exception.getSession().getSessionDate()))")
    @Mapping(target = "daysUntilSession", expression = "java(getDaysUntilSession(exception.getSession().getSessionDate()))")
    @Mapping(target = "canModify", expression = "java(canModify(exception))")
    @Mapping(target = "approvedByName", ignore = true)
    ExceptionResponse toResponse(TutorAvailabilityException exception);

    List<ExceptionResponse> toResponseList(List<TutorAvailabilityException> exceptions);

    default String getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL,
                        java.util.Locale.forLanguageTag("vi"));
    }

    default Long getDaysUntilSession(LocalDate sessionDate) {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), sessionDate);
    }

    default Boolean canModify(TutorAvailabilityException exception) {
        return exception.getStatus() == ExceptionStatus.PENDING &&
                exception.getSession().getSessionDate().isAfter(LocalDate.now());
    }
}
