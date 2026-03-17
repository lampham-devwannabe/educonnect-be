package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.course.request.CourseCreationRequest;
import com.sep.educonnect.dto.course.request.CourseUpdateRequest;
import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.course.response.MyCourseResponse;
import com.sep.educonnect.dto.syllabus.response.SyllabusResponse;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.CourseProgress;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.entity.User;

import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", uses = { SyllabusMapper.class, ModuleMapper.class, LessonMapper.class })
public interface CourseMapper {

        @Mapping(target = "tutor", source = "tutor")
        @Mapping(target = "syllabus", ignore = true)
        @Mapping(target = "totalEnrolled", ignore = true)
        @Mapping(target = "isDeleted", source = "isDeleted")
        CourseInfoResponse toCourseInfoResponse(Course course);

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "syllabus", ignore = true)
        @Mapping(target = "tutor", ignore = true)
        Course toCourse(CourseCreationRequest request);

        default CourseInfoResponse.TutorInfo toTutorInfo(User tutor) {
                if (tutor == null) {
                        return null;
                }
                return CourseInfoResponse.TutorInfo.builder()
                                .userId(tutor.getUserId())
                                .username(tutor.getUsername())
                                .firstName(tutor.getFirstName())
                                .lastName(tutor.getLastName())
                                .avatar(tutor.getAvatar())
                                .tutorProfileId(null)
                                .build();
        }

        default CourseInfoResponse.SyllabusInfo toSyllabusInfo(
                        SyllabusResponse syllabusResponse,
                        Long syllabusId,
                        @Context ModuleMapper moduleMapper,
                        @Context LessonMapper lessonMapper,
                        List<Module> modules) {
                if (syllabusResponse == null) {
                        return null;
                }

                List<CourseInfoResponse.ModuleInfo> moduleInfos = toModuleInfos(modules, moduleMapper, lessonMapper);
                int totalModules = moduleInfos.size();
                int totalLessons = moduleInfos.stream()
                                .mapToInt(
                                                module -> module.getLessons() != null
                                                                ? module.getLessons().size()
                                                                : 0)
                                .sum();

                return CourseInfoResponse.SyllabusInfo.builder()
                                .syllabusId(syllabusId)
                                .name(syllabusResponse.getName())
                                .level(syllabusResponse.getLevel())
                                .target(syllabusResponse.getTarget())
                                .description(syllabusResponse.getDescription())
                                .status(syllabusResponse.getStatus())
                                .modules(moduleInfos)
                                .totalModules(totalModules)
                                .totalLessons(totalLessons)
                                .build();
        }

        default List<CourseInfoResponse.ModuleInfo> toModuleInfos(
                        List<Module> modules, ModuleMapper moduleMapper, LessonMapper lessonMapper) {
                if (modules == null || modules.isEmpty()) {
                        return List.of();
                }

                return modules.stream()
                                .sorted(
                                                Comparator.comparing(
                                                                Module::getOrderNumber,
                                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(module -> moduleMapper.toModuleInfo(module, lessonMapper))
                                .toList();
        }

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "syllabus", ignore = true)
        @Mapping(target = "tutor", ignore = true)
        @Mapping(target = "isDeleted", ignore = true)
        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
        void updateCourse(@MappingTarget Course course, CourseUpdateRequest request);

        default MyCourseResponse toMyCourseResponse(Course course, Long classId, CourseProgress courseProgress, Lesson lesson) {
                if (course == null) {
                        return null;
                }

                int totalLessons = courseProgress != null && courseProgress.getTotalLessons() != null
                                ? courseProgress.getTotalLessons()
                                : 0;
                int completedLessons = courseProgress != null && courseProgress.getCompletedLessons() != null
                                ? courseProgress.getCompletedLessons()
                                : 0;
                int progressPercentage = courseProgress != null && courseProgress.getProgressPercentage() != null
                                ? courseProgress.getProgressPercentage()
                                : (totalLessons > 0 ? (completedLessons * 100) / totalLessons : 0);

                MyCourseResponse.LatestLesson latestLesson = lesson != null
                                ? MyCourseResponse.LatestLesson.builder()
                                                .lessonId(lesson.getLessonId())
                                                .title(lesson.getTitle())
                                                .build()
                                : null;

                return MyCourseResponse.builder()
                                .id(course.getId())
                                    .classId(classId)
                                .name(course.getName())
                                .pictureUrl(course.getPictureUrl())
                                .progressPercentage(progressPercentage)
                                .completedLessons(completedLessons)
                                .totalLessons(totalLessons)
                                .latestLesson(latestLesson)
                                .build();
        }
}
