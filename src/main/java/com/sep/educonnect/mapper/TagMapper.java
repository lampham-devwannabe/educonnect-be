package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.tag.TagRequest;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.helper.LocalizationHelper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
        componentModel = "spring",
        uses = {LocalizationHelper.class})
public interface TagMapper {
    @Mapping(target = "id", ignore = true)
    Tag toEntity(TagRequest request);

    TagResponse toResponse(Tag tag);

    void updateEntity(TagRequest request, @MappingTarget Tag tag);
}
