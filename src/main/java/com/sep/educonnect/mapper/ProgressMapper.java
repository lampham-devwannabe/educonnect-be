package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.progress.CourseProgressResponse;
import com.sep.educonnect.dto.progress.LessonProgressResponse;
import com.sep.educonnect.entity.CourseProgress;
import com.sep.educonnect.entity.LessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "courseProgressId", source = "id")
    @Mapping(target = "enrollmentId", source = "enrollment.id")
    @Mapping(target = "lessons", source = "lessonProgresses")
    CourseProgressResponse toCourseProgressResponse(CourseProgress progress);

    @Mapping(target = "lessonId", source = "lesson.lessonId")
    @Mapping(target = "lessonTitle", source = "lesson.title")
    LessonProgressResponse toLessonProgressResponse(LessonProgress progress);

    List<LessonProgressResponse> toLessonProgressResponses(List<LessonProgress> progressList);
}

