package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.classsession.ClassSessionResponse;
import com.sep.educonnect.dto.course.response.CourseGeneralResponse;
import com.sep.educonnect.dto.tutor.response.TutorClassResponse;
import com.sep.educonnect.dto.user.response.UserResponse;
import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.entity.User;
import com.sep.educonnect.helper.LocalizationHelper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface TutorClassMapper {

    @Mapping(target = "id", source = "tutorClass.id")
    @Mapping(target = "tutor", source = "tutor")
    @Mapping(target = "course", source = "course")
    @Mapping(target = "startDate", source = "tutorClass.startDate")
    @Mapping(target = "endDate", source = "tutorClass.endDate")
    @Mapping(target = "maxStudents", source = "tutorClass.maxStudents")
    @Mapping(target = "currentStudents", source = "tutorClass.currentStudents")
    @Mapping(target = "title", source = "tutorClass.title")
    @Mapping(target = "description", source = "tutorClass.description")
    @Mapping(target = "sessions", source = "sessions")
    TutorClassResponse toResponse(TutorClass tutorClass, @Context LocalizationHelper localizationHelper);

    List<TutorClassResponse> toResponseList(List<TutorClass> tutorClasses, @Context LocalizationHelper localizationHelper);

    UserResponse toTutorBasicDTO(User user);
    CourseGeneralResponse toCourseDTO(Course course);


    @Mapping(target = "meetingJoinUrl", source = "meetingJoinUrl")
    @Mapping(target = "meetingStartUrl", source = "meetingStartUrl")
    @Mapping(target = "meetingPassword", source = "meetingPassword")
    @Mapping(target = "meetingId", source = "meetingId")
    ClassSessionResponse toClassSessionDTO(ClassSession session);

}
