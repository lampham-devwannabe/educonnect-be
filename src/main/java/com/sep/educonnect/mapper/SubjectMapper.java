package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.subject.request.SubjectCreationRequest;
import com.sep.educonnect.dto.subject.request.SubjectUpdateRequest;
import com.sep.educonnect.dto.subject.response.SubjectResponse;
import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.helper.LocalizationHelper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface SubjectMapper {

    @Mapping(target = "subjectId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Subject toSubject(SubjectCreationRequest request);

    @Mapping(target = "name", expression = "java(localizationHelper.getLocalizedField(subject, \"name\"))")
    SubjectResponse toSubjectResponse(Subject subject, @Context LocalizationHelper localizationHelper);

    @Mapping(target = "subjectId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateSubject(@MappingTarget Subject subject, SubjectUpdateRequest request);
}
