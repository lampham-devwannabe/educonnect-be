package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.lesson.LessonRequest;
import com.sep.educonnect.dto.lesson.LessonResponse;
import com.sep.educonnect.entity.Lesson;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "module", ignore = true)
    @Mapping(target = "exams", ignore = true)
    Lesson toEntity(LessonRequest request);

    LessonResponse toResponse(Lesson lesson);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "orderNumber", source = "request.orderNumber")
    @Mapping(target = "durationMinutes", source = "request.durationMinutes")
    @Mapping(target = "objectives", source = "request.objectives")
    @Mapping(target = "status", source = "request.status")
    void updateEntity(@MappingTarget Lesson target, LessonRequest request);

    @Mapping(target = "lessonId", source = "lessonId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "orderNumber", source = "orderNumber")
    CourseInfoResponse.LessonInfo toLessonInfo(Lesson lesson);
}
