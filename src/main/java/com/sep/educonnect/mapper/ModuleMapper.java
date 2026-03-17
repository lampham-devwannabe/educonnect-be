package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.course.response.CourseInfoResponse;
import com.sep.educonnect.dto.module.ModuleRequest;
import com.sep.educonnect.dto.module.ModuleResponse;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.Module;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", uses = {LessonMapper.class})
public interface ModuleMapper {

    @Mapping(target = "moduleId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    Module toEntity(ModuleRequest request);

    ModuleResponse toResponse(Module module);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "status", source = "status")
    void updateEntity(@MappingTarget Module target, ModuleRequest request);

    default CourseInfoResponse.ModuleInfo toModuleInfo(Module module, @Context LessonMapper lessonMapper) {
        if (module == null) {
            return null;
        }
        
        List<CourseInfoResponse.LessonInfo> lessonInfos = module.getLessons() != null && !module.getLessons().isEmpty()
                ? module.getLessons().stream()
                    .sorted(Comparator.comparing(Lesson::getOrderNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(lessonMapper::toLessonInfo)
                    .toList()
                : List.of();

        return CourseInfoResponse.ModuleInfo.builder()
                .moduleId(module.getModuleId())
                .title(module.getTitle())
                .orderNumber(module.getOrderNumber())
                .lessons(lessonInfos)
                .build();
    }
}
