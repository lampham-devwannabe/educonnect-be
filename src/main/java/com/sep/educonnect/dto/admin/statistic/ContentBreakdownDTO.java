package com.sep.educonnect.dto.admin.statistic;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ContentBreakdownDTO {
    Long readyVideos;
    Long processingVideos;
    Long failedVideos;
    Long publishedLessons;
    Long draftLessons;
    Long activeExams;
}
