package com.sep.educonnect.service;

import com.sep.educonnect.dto.tag.TagImageResponse;
import com.sep.educonnect.dto.tag.TagRequest;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.TagMapper;
import com.sep.educonnect.repository.TagRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class TagService {
    TagRepository tagRepository;
    TagMapper tagMapper;

    static HashMap<String, String> imageMap= new HashMap<>();

    public static HashMap<String, String> getImageMap() {
        imageMap.put("Interactive", "https://i.pinimg.com/400x/68/c5/08/68c508d9026bf3036bab16f7c6239b19.jpg");
        imageMap.put("Conversational", "https://i.pinimg.com/400x/a7/70/a7/a770a7f3df10dd93b5158ac0481b810e.jpg");
        imageMap.put("Structured Lessons", "https://i.pinimg.com/400x/2f/e4/bb/2fe4bb5b05d01bb60d64a9ea6bef27bf.jpg");
        imageMap.put("Practice-Oriented", "https://i.pinimg.com/400x/94/97/46/94974623b9b891be217118cd50205e02.jpg");
        imageMap.put("Exam-Oriented", "https://i.pinimg.com/400x/4f/fe/ad/4ffead7c90769aec1558c427f7d66862.jpg");
        imageMap.put("Discussion-Based", "https://i.pinimg.com/400x/8e/d4/d6/8ed4d651a45540789cbe5da9e373911c.jpg");
        imageMap.put("Visual & Multimedia Aids", "https://i.pinimg.com/400x/6e/00/89/6e0089edf64ea1e799cd3da6ec417596.jpg");
        imageMap.put("Step-by-Step Explanation", "https://i.pinimg.com/400x/1d/c6/85/1dc68505e2534c0463eb50997776c450.jpg");
        imageMap.put("Project-Based Learning", "https://i.pinimg.com/400x/e9/9c/f5/e99cf5f1299f0b059990bc8cfed1919a.jpg");
        imageMap.put("Gamified Learning", "https://i.pinimg.com/400x/3b/b2/a6/3bb2a6b536138f60b797cc5e08523880.jpg");
        imageMap.put("Student-Centered", "https://i.pinimg.com/400x/0a/f3/29/0af329768bfdb55522a37a064af27ff9.jpg");
        imageMap.put("Intensive Coaching", "https://i.pinimg.com/400x/d1/cc/a7/d1cca71b9f2187e95f2fb4bb786a2e92.jpg");
        imageMap.put("Flexible Pace", "https://i.pinimg.com/400x/ae/02/fe/ae02fe564682ba49f9a9ea2c744339b7.jpg");
        return imageMap;
    }

    public TagResponse createTag(TagRequest request) {
        var tag = tagMapper.toEntity(request);
        tag = tagRepository.save(tag);
        return tagMapper.toResponse(tag);
    }

    @Transactional(readOnly = true)
    public Page<TagResponse> getAllTags(
            int page, int size, String sortBy, String direction, String name) {
        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Tag> tagPage = tagRepository.searchTags(normalize(name), pageable);
        return tagPage.map(tagMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(Long tagId) {
        Tag tag =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        return tagMapper.toResponse(tag);
    }

    public TagResponse updateTag(Long tagId, TagRequest request) {
        Tag tag =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        tagMapper.updateEntity(request, tag);
        tag = tagRepository.save(tag);
        return tagMapper.toResponse(tag);
    }

    public void deleteTag(Long tagId) {
        Tag tag =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        tag.setIsDeleted(true);
        tagRepository.save(tag);
    }

    public List<TagImageResponse> getAllTagWithImages() {
        Map<String, String> imageMap = getImageMap();

        return tagRepository.findAll()
                .stream()
                .filter(tag-> !tag.getIsDeleted())
                .map(tag -> {
                    String imageUrl = null;

                    if (tag.getNameEn() != null) {
                        imageUrl = imageMap.get(tag.getNameEn());
                    }

                    return TagImageResponse.builder()
                            .id(tag.getId())
                            .nameEn(tag.getNameEn())
                            .nameVi(tag.getNameVi())
                            .imageUrl(imageUrl)
                            .build();
                })
                .toList();
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
