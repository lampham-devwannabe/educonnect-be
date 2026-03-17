package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.tutor.TutorResponse;
import com.sep.educonnect.entity.TutorProfile;
import com.sep.educonnect.helper.LocalizationHelper;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface TutorProfileMapper {
    @Mapping(target = "tutorName", source = "user.firstName")
    @Mapping(target = "hourlyRate", source = "hourlyRate")
    @Mapping(target = "currencyCode", expression = "java(tutor.getCurrencyCode() != null ? tutor.getCurrencyCode().name() : null)")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "studentCount", source = "studentCount")
    @Mapping(target = "reviewCount", source = "reviewCount")
    @Mapping(target = "desc", expression = "java(localizationHelper.getLocalizedField(tutor, \"desc\"))")
    @Mapping(target = "bio", expression = "java(localizationHelper.getLocalizedField(tutor, \"bio\"))")
    TutorResponse toResponse(TutorProfile tutor, @Context LocalizationHelper localizationHelper);

    List<TutorResponse> toResponseList(List<TutorProfile> tutors, @Context LocalizationHelper localizationHelper);
}
