package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.video.VideoLessonRequest;
import com.sep.educonnect.dto.video.VideoLessonResponse;
import com.sep.educonnect.entity.VideoLesson;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VideoLessonMapper {

    @Mapping(target = "videoLessonId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "processingProgress", ignore = true)
    @Mapping(target = "processingErrorMessage", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    VideoLesson toEntity(VideoLessonRequest request);

    VideoLessonResponse toResponse(VideoLesson video);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "videoType", source = "request.videoType")
    @Mapping(target = "accessType", source = "request.accessType")
    @Mapping(target = "isPreview", source = "request.isPreview")
    void updateEntity(@MappingTarget VideoLesson target, VideoLessonRequest request);
}
