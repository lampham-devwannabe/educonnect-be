package com.sep.educonnect.dto.booking;

import com.sep.educonnect.entity.TutorClass;

import java.util.List;

public record BookingStateResponse(
        boolean alreadyJoined,    // join state
        Long joinedClassId,       // null if never enrolled
        List<ClassSummary> joinableClasses
) {
    public record ClassSummary(Long id, String title, int currentStudents, int maxStudents, int sessionsLeft) {
        public static ClassSummary fromEntity(TutorClass c, int sessionLeft) {
            return new ClassSummary(
                    c.getId(),
                    c.getTitle(),
                    c.getCurrentStudents(),
                    c.getMaxStudents(),
                    sessionLeft
            );
        }
    }


}