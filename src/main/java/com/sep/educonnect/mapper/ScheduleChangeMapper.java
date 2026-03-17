package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.tutor.response.ScheduleChangeResponse;
import com.sep.educonnect.entity.ScheduleChange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleChangeMapper {


    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "sessionInfo", expression = "java(buildSessionInfo(scheduleChange))")
    @Mapping(target = "tutorId", expression = "java(getTutorId(scheduleChange))")
    @Mapping(target = "className", expression = "java(getClassName(scheduleChange))")
    @Mapping(target = "tutorName", expression = "java(getTutorName(scheduleChange))")
    @Mapping(target = "tutorEmail", expression = "java(getTutorEmail(scheduleChange))")
    @Mapping(target = "newSlot", source = "newSLot")
    ScheduleChangeResponse toResponse(ScheduleChange scheduleChange);

    List<ScheduleChangeResponse> toResponseList(List<ScheduleChange> scheduleChanges);



    default String buildSessionInfo(ScheduleChange scheduleChange) {
        if (scheduleChange == null || scheduleChange.getSession() == null) {
            return "N/A";
        }
        var session = scheduleChange.getSession();
        return String.format("%s to %s",
                session.getStartTime() != null ? session.getStartTime() : "N/A",
                session.getEndTime() != null ? session.getEndTime() : "N/A");

    }

    default String getTutorId(ScheduleChange scheduleChange) {
        try {
            if (scheduleChange != null &&
                    scheduleChange.getSession() != null &&
                    scheduleChange.getSession().getTutorClass() != null &&
                    scheduleChange.getSession().getTutorClass().getTutor() != null &&
                    scheduleChange.getSession().getTutorClass().getTutor().getUserId() != null) {
                return scheduleChange.getSession().getTutorClass()
                        .getTutor().getUserId();
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }

    default String getClassName(ScheduleChange scheduleChange) {
        try {
            if (scheduleChange != null &&
                    scheduleChange.getSession() != null &&
                    scheduleChange.getSession().getTutorClass() != null) {
                return scheduleChange.getSession().getTutorClass().getTitle();
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }

    default String getTutorName(ScheduleChange scheduleChange) {
        try {
            if (scheduleChange != null &&
                    scheduleChange.getSession() != null &&
                    scheduleChange.getSession().getTutorClass() != null &&
                    scheduleChange.getSession().getTutorClass().getTutor() != null) {
                var user = scheduleChange.getSession().getTutorClass().getTutor();
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                String fullName = (firstName + " " + lastName).trim();
                return fullName.isEmpty() ? "Unknown" : fullName;
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }

    default String getTutorEmail(ScheduleChange scheduleChange) {
        try {
            if (scheduleChange != null &&
                    scheduleChange.getSession() != null &&
                    scheduleChange.getSession().getTutorClass() != null &&
                    scheduleChange.getSession().getTutorClass().getTutor() != null) {
                String email = scheduleChange.getSession().getTutorClass().getTutor().getEmail();
                return email != null ? email : "Unknown";
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }
}
