package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.subject.response.SubjectSyllabusResponse;
import com.sep.educonnect.dto.syllabus.request.SyllabusCreationRequest;
import com.sep.educonnect.dto.syllabus.request.SyllabusUpdateRequest;
import com.sep.educonnect.dto.syllabus.response.SyllabusResponse;
import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.entity.Syllabus;
import com.sep.educonnect.helper.LocalizationHelper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface SyllabusMapper {

    @Mapping(target = "syllabusId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Syllabus toSyllabus(SyllabusCreationRequest request);

    @Mapping(target = "level", expression = "java(localizationHelper.getLocalizedField(syllabus, \"level\"))")
    @Mapping(target = "target", expression = "java(localizationHelper.getLocalizedField(syllabus, \"target\"))")
    @Mapping(target = "name", source = "syllabus.name")
    @Mapping(target = "description", expression = "java(localizationHelper.getLocalizedField(syllabus, \"description\"))")
    SyllabusResponse toSyllabusResponse(Syllabus syllabus, @Context LocalizationHelper localizationHelper);

    @Mapping(target = "syllabusId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateSyllabus(@MappingTarget Syllabus syllabus, SyllabusUpdateRequest request);

    @Mapping(target = "name", expression = "java(localizationHelper.getLocalizedField(subject, \"name\"))")
    @Mapping(target = "syllabuses", source = "syllabuses")
    SubjectSyllabusResponse toSubjectSyllabusResponse(
            Subject subject,
            List<Syllabus> syllabuses,
            @Context LocalizationHelper localizationHelper
    );

    @Mapping(target = "syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "level", expression = "java(localizationHelper.getLocalizedField(syllabus, \"level\"))")
    SubjectSyllabusResponse.SyllabusDto toSyllabusDto(
            Syllabus syllabus,
            @Context LocalizationHelper localizationHelper
    );

    List<SubjectSyllabusResponse.SyllabusDto> toSyllabusDtoList(
            List<Syllabus> syllabuses,
            @Context LocalizationHelper localizationHelper
    );
}
