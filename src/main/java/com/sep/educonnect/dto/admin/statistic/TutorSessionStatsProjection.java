package com.sep.educonnect.dto.admin.statistic;

public interface TutorSessionStatsProjection {
    Integer getId();
    String getTutorId();
    String getTutorName();
    Double getTeachingHours();
    Integer getSessionsCompleted();
}
