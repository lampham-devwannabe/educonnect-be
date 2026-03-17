package com.sep.educonnect.dto.student;

import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.entity.ScheduleChange;
import com.sep.educonnect.entity.TutorClass;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class StudentScheduleDTO {
    Long sessionId;
    Long classId;
    String className;
    String courseName;
    String tutorName;
    LocalDate sessionDate;
    Integer slotNumber;
    Integer sessionNumber;
    String topic;
    String meetingJoinUrl;
    String meetingPassword;
    String notes;
    Boolean hasScheduleChange;
    LocalDate originalDate;
    Integer originalSlot;
    LocalDate newDate;
    Integer newSlot;

    public static StudentScheduleDTO fromSession(ClassSession session, String studentId, ScheduleChange scheduleChange) {
        TutorClass tc = session.getTutorClass();

        StudentScheduleDTOBuilder builder = StudentScheduleDTO.builder()
                .sessionId(session.getId())
                .classId(tc.getId())
                .className(tc.getTitle())
                .courseName(tc.getCourse().getName())
                .tutorName(tc.getTutor().getFirstName() + tc.getTutor().getLastName())
                .sessionNumber(session.getSessionNumber())
                .topic(session.getTopic())
                .meetingJoinUrl(session.getMeetingJoinUrl())
                .meetingPassword(session.getMeetingPassword())
                .notes(session.getNotes());

        // Nếu có thay đổi lịch đã được duyệt, dùng thông tin mới
        if (scheduleChange != null) {
            builder.hasScheduleChange(true)
                    .originalDate(session.getSessionDate())
                    .originalSlot(session.getSlotNumber())
                    .newDate(scheduleChange.getNewDate())
                    .newSlot(scheduleChange.getNewSLot())
                    // Hiển thị thông tin mới cho user
                    .sessionDate(scheduleChange.getNewDate())
                    .slotNumber(scheduleChange.getNewSLot());
        } else {
            builder.hasScheduleChange(false)
                    .sessionDate(session.getSessionDate())
                    .slotNumber(session.getSlotNumber());
        }

        return builder.build();
    }
}
