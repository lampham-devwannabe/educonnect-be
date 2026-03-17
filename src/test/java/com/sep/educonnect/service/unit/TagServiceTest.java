package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sep.educonnect.dto.tag.TagRequest;
import com.sep.educonnect.dto.tag.TagResponse;
import com.sep.educonnect.entity.Tag;
import com.sep.educonnect.mapper.TagMapper;
import com.sep.educonnect.repository.TagRepository;
import com.sep.educonnect.service.TagService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Objects;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Unit Tests")
class TagServiceTest {

    @Mock private TagRepository tagRepository;

    @Mock private TagMapper tagMapper;

    @InjectMocks private TagService tagService;

    private TagRequest validRequest;
    private Tag tag;
    private TagResponse tagResponse;

    @BeforeEach
    void setUp() {
        validRequest = TagRequest.builder().nameVi("Toán học").nameEn("Mathematics").build();

        tag = Tag.builder().id(1L).nameVi("Toán học").nameEn("Mathematics").build();

        tagResponse = TagResponse.builder().id(1L).nameVi("Toán học").nameEn("Mathematics").build();
    }

    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }

    @Test
    @DisplayName("Should create tag successfully")
    void should_createTag_successfully() {
        // Given
        when(tagMapper.toEntity(validRequest)).thenReturn(tag);
        when(tagRepository.save(tag)).thenReturn(tag);
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        // When
        TagResponse result = tagService.createTag(validRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Toán học", result.getNameVi());
        assertEquals("Mathematics", result.getNameEn());

        verify(tagMapper).toEntity(validRequest);
        verify(tagRepository).save(tag);
        verify(tagMapper).toResponse(tag);
    }

    @Test
    @DisplayName("Should create tag with Vietnamese name only")
    void should_createTag_withVietnameseNameOnly() {
        // Given
        TagRequest request = TagRequest.builder().nameVi("Khoa học").nameEn(null).build();

        Tag tagWithViOnly = Tag.builder().id(2L).nameVi("Khoa học").nameEn(null).build();

        TagResponse response = TagResponse.builder().id(2L).nameVi("Khoa học").nameEn(null).build();

        when(tagMapper.toEntity(request)).thenReturn(tagWithViOnly);
        when(tagRepository.save(tagWithViOnly)).thenReturn(tagWithViOnly);
        when(tagMapper.toResponse(tagWithViOnly)).thenReturn(response);

        // When
        TagResponse result = tagService.createTag(request);

        // Then
        assertNotNull(result);
        assertEquals("Khoa học", result.getNameVi());
        assertNull(result.getNameEn());
        verify(tagRepository).save(tagWithViOnly);
    }

    @Test
    @DisplayName("Should create tag with English name only")
    void should_createTag_withEnglishNameOnly() {
        // Given
        TagRequest request = TagRequest.builder().nameVi(null).nameEn("Science").build();

        Tag tagWithEnOnly = Tag.builder().id(3L).nameVi(null).nameEn("Science").build();

        TagResponse response = TagResponse.builder().id(3L).nameVi(null).nameEn("Science").build();

        when(tagMapper.toEntity(request)).thenReturn(tagWithEnOnly);
        when(tagRepository.save(tagWithEnOnly)).thenReturn(tagWithEnOnly);
        when(tagMapper.toResponse(tagWithEnOnly)).thenReturn(response);

        // When
        TagResponse result = tagService.createTag(request);

        // Then
        assertNotNull(result);
        assertNull(result.getNameVi());
        assertEquals("Science", result.getNameEn());
        verify(tagRepository).save(tagWithEnOnly);
    }

    @Test
    @DisplayName("Should create tag with special characters")
    void should_createTag_withSpecialCharacters() {
        // Given
        TagRequest request =
                TagRequest.builder().nameVi("Toán học & Khoa học").nameEn("Math & Science").build();

        Tag tagWithSpecialChars =
                Tag.builder().id(5L).nameVi("Toán học & Khoa học").nameEn("Math & Science").build();

        TagResponse response =
                TagResponse.builder()
                        .id(5L)
                        .nameVi("Toán học & Khoa học")
                        .nameEn("Math & Science")
                        .build();

        when(tagMapper.toEntity(request)).thenReturn(tagWithSpecialChars);
        when(tagRepository.save(tagWithSpecialChars)).thenReturn(tagWithSpecialChars);
        when(tagMapper.toResponse(tagWithSpecialChars)).thenReturn(response);

        // When
        TagResponse result = tagService.createTag(request);

        // Then
        assertNotNull(result);
        assertEquals("Toán học & Khoa học", result.getNameVi());
        assertEquals("Math & Science", result.getNameEn());
        verify(tagRepository).save(tagWithSpecialChars);
    }

    @Test
    @DisplayName("Should verify mapper and repository interaction order")
    void should_verifyInteractionOrder() {
        // Given
        when(tagMapper.toEntity(validRequest)).thenReturn(tag);
        when(tagRepository.save(tag)).thenReturn(tag);
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        // When
        tagService.createTag(validRequest);

        // Then - Verify order of interactions
        var inOrder = inOrder(tagMapper, tagRepository);
        inOrder.verify(tagMapper).toEntity(validRequest);
        inOrder.verify(tagRepository).save(tag);
        inOrder.verify(tagMapper).toResponse(tag);
    }

    @Test
    @DisplayName("Should handle mapper returning tag without ID")
    void should_handleTag_withoutInitialId() {
        // Given
        Tag tagWithoutId = Tag.builder().nameVi("Toán học").nameEn("Mathematics").build();

        Tag savedTagWithId = Tag.builder().id(1L).nameVi("Toán học").nameEn("Mathematics").build();

        when(tagMapper.toEntity(validRequest)).thenReturn(tagWithoutId);
        when(tagRepository.save(tagWithoutId)).thenReturn(savedTagWithId);
        when(tagMapper.toResponse(savedTagWithId)).thenReturn(tagResponse);

        // When
        TagResponse result = tagService.createTag(validRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(tagRepository).save(tagWithoutId);
    }

    @Test
    @DisplayName("Should create multiple tags with different IDs")
    void should_createMultipleTags_withDifferentIds() {
        // Given
        TagRequest request1 = TagRequest.builder().nameVi("Tag 1").nameEn("Tag 1").build();

        TagRequest request2 = TagRequest.builder().nameVi("Tag 2").nameEn("Tag 2").build();

        Tag tag1 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();

        when(tagMapper.toEntity(request1)).thenReturn(tag1);
        when(tagMapper.toEntity(request2)).thenReturn(tag2);
        when(tagRepository.save(tag1)).thenReturn(tag1);
        when(tagRepository.save(tag2)).thenReturn(tag2);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);

        // When
        TagResponse result1 = tagService.createTag(request1);
        TagResponse result2 = tagService.createTag(request2);

        // Then
        assertEquals(1L, result1.getId());
        assertEquals(2L, result2.getId());
        verify(tagRepository, times(2)).save(any(Tag.class));
    }


    @Test
    @DisplayName("Should get all tags with default pagination and ascending sort")
    void should_getAllTags_withDefaultPaginationAscending() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String direction = "asc";

        Tag tag1 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();
        Tag tag3 = Tag.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1, tag2, tag3),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        3);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();
        TagResponse response3 =
                TagResponse.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);
        when(tagMapper.toResponse(tag3)).thenReturn(response3);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(3, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        assertEquals(3L, result.getContent().get(2).getId());

        verify(tagRepository).searchTags(any(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should get all tags with descending sort")
    void should_getAllTags_withDescendingSort() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String direction = "desc";

        Tag tag1 = Tag.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();
        Tag tag3 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1, tag2, tag3),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC,
                                        sortBy)),
                        3);

        TagResponse response1 =
                TagResponse.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();
        TagResponse response3 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);
        when(tagMapper.toResponse(tag3)).thenReturn(response3);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        assertEquals(1L, result.getContent().get(2).getId());
    }

    @Test
    @DisplayName("Should get all tags with empty result")
    void should_getAllTags_withEmptyResult() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String direction = "asc";

        var emptyPage =
                new org.springframework.data.domain.PageImpl<Tag>(
                        java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        0);

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
        verify(tagRepository).searchTags(any(), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should get all tags with pagination - page 1")
    void should_getAllTags_withPaginationPage1() {
        // Given
        int page = 1;
        int size = 2;
        String sortBy = "id";
        String direction = "asc";

        Tag tag3 = Tag.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();
        Tag tag4 = Tag.builder().id(4L).nameVi("Tag 4").nameEn("Tag 4").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag3, tag4),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        5);

        TagResponse response3 =
                TagResponse.builder().id(3L).nameVi("Tag 3").nameEn("Tag 3").build();
        TagResponse response4 =
                TagResponse.builder().id(4L).nameVi("Tag 4").nameEn("Tag 4").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag3)).thenReturn(response3);
        when(tagMapper.toResponse(tag4)).thenReturn(response4);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getNumber());
    }

    @Test
    @DisplayName("Should get all tags sorted by nameVi")
    void should_getAllTags_sortedByNameVi() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "nameVi";
        String direction = "asc";

        Tag tag1 = Tag.builder().id(1L).nameVi("Anh văn").nameEn("English").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Hóa học").nameEn("Chemistry").build();
        Tag tag3 = Tag.builder().id(3L).nameVi("Toán học").nameEn("Mathematics").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1, tag2, tag3),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        3);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Anh văn").nameEn("English").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Hóa học").nameEn("Chemistry").build();
        TagResponse response3 =
                TagResponse.builder().id(3L).nameVi("Toán học").nameEn("Mathematics").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);
        when(tagMapper.toResponse(tag3)).thenReturn(response3);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Anh văn", result.getContent().get(0).getNameVi());
        assertEquals("Hóa học", result.getContent().get(1).getNameVi());
        assertEquals("Toán học", result.getContent().get(2).getNameVi());
    }

    @Test
    @DisplayName("Should get all tags sorted by nameEn")
    void should_getAllTags_sortedByNameEn() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "nameEn";
        String direction = "asc";

        Tag tag1 = Tag.builder().id(1L).nameVi("Hóa học").nameEn("Chemistry").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Anh văn").nameEn("English").build();
        Tag tag3 = Tag.builder().id(3L).nameVi("Toán học").nameEn("Mathematics").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1, tag2, tag3),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        3);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Hóa học").nameEn("Chemistry").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Anh văn").nameEn("English").build();
        TagResponse response3 =
                TagResponse.builder().id(3L).nameVi("Toán học").nameEn("Mathematics").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);
        when(tagMapper.toResponse(tag3)).thenReturn(response3);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Chemistry", result.getContent().get(0).getNameEn());
        assertEquals("English", result.getContent().get(1).getNameEn());
        assertEquals("Mathematics", result.getContent().get(2).getNameEn());
    }

    @Test
    @DisplayName("Should get all tags with invalid direction defaults to ascending")
    void should_getAllTags_withInvalidDirectionDefaultsToAsc() {
        // Given
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String direction = "invalid";

        Tag tag1 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        1);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should get all tags with large page size")
    void should_getAllTags_withLargePageSize() {
        // Given
        int page = 0;
        int size = 100;
        String sortBy = "id";
        String direction = "asc";

        Tag tag1 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        Tag tag2 = Tag.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1, tag2),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        2);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();
        TagResponse response2 =
                TagResponse.builder().id(2L).nameVi("Tag 2").nameEn("Tag 2").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    @DisplayName("Should get all tags with single item per page")
    void should_getAllTags_withSingleItemPerPage() {
        // Given
        int page = 0;
        int size = 1;
        String sortBy = "id";
        String direction = "asc";

        Tag tag1 = Tag.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        var tagPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(tag1),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        sortBy)),
                        5);

        TagResponse response1 =
                TagResponse.builder().id(1L).nameVi("Tag 1").nameEn("Tag 1").build();

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tagPage);
        when(tagMapper.toResponse(tag1)).thenReturn(response1);

        // When
        var result = tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(5, result.getTotalPages());
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should verify repository is called with correct pageable")
    void should_verifyRepositoryCalledWithCorrectPageable() {
        // Given
        int page = 2;
        int size = 5;
        String sortBy = "nameVi";
        String direction = "desc";

        var emptyPage =
                new org.springframework.data.domain.PageImpl<Tag>(
                        java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(
                                page,
                                size,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC,
                                        sortBy)),
                        0);

        when(tagRepository.searchTags(any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // When
        tagService.getAllTags(page, size, sortBy, direction, null);

        // Then
        verify(tagRepository)
                .searchTags(
                        any(),
                        argThat(
                                (Pageable pageable) ->
                                        pageable.getPageNumber() == page
                                                && pageable.getPageSize() == size
                                                && pageable.getSort().getOrderFor(sortBy) != null
                                                && Objects.requireNonNull(
                                                                        pageable.getSort()
                                                                                .getOrderFor(
                                                                                        sortBy))
                                                                .getDirection()
                                                        == org.springframework.data.domain.Sort
                                                                .Direction.DESC));
    }

    @Test
    @DisplayName("Should get tag by id successfully")
    void should_getTagById_successfully() {
        // Given
        Long tagId = 1L;
        Tag tag = Tag.builder().id(tagId).nameVi("Toán học").nameEn("Mathematics").build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Toán học").nameEn("Mathematics").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals("Toán học", result.getNameVi());
        assertEquals("Mathematics", result.getNameEn());
        verify(tagRepository).findById(tagId);
        verify(tagMapper).toResponse(tag);
    }

    @Test
    @DisplayName("Should throw exception when tag not found by id")
    void should_throwException_when_tagNotFoundById() {
        // Given
        Long tagId = 999L;
        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.empty());

        // When & Then
        var exception =
                assertThrows(
                        com.sep.educonnect.exception.AppException.class,
                        () -> tagService.getTagById(tagId));
        assertEquals(
                com.sep.educonnect.exception.ErrorCode.TAG_NOT_FOUND, exception.getErrorCode());
        verify(tagRepository).findById(tagId);
        verify(tagMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should get tag by id with null Vietnamese name")
    void should_getTagById_withNullVietnameseName() {
        // Given
        Long tagId = 2L;
        Tag tag = Tag.builder().id(tagId).nameVi(null).nameEn("Science").build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi(null).nameEn("Science").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertNull(result.getNameVi());
        assertEquals("Science", result.getNameEn());
    }

    @Test
    @DisplayName("Should get tag by id with null English name")
    void should_getTagById_withNullEnglishName() {
        // Given
        Long tagId = 3L;
        Tag tag = Tag.builder().id(tagId).nameVi("Khoa học").nameEn(null).build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Khoa học").nameEn(null).build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals("Khoa học", result.getNameVi());
        assertNull(result.getNameEn());
    }

    @Test
    @DisplayName("Should get tag by id with large id value")
    void should_getTagById_withLargeIdValue() {
        // Given
        Long tagId = 999999999L;
        Tag tag = Tag.builder().id(tagId).nameVi("Tag").nameEn("Tag").build();
        TagResponse response = TagResponse.builder().id(tagId).nameVi("Tag").nameEn("Tag").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        verify(tagRepository).findById(tagId);
    }

    @Test
    @DisplayName("Should get tag by id with special characters in names")
    void should_getTagById_withSpecialCharactersInNames() {
        // Given
        Long tagId = 5L;
        Tag tag =
                Tag.builder()
                        .id(tagId)
                        .nameVi("Toán & Lý & Hóa")
                        .nameEn("Math & Physics & Chemistry")
                        .build();
        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("Toán & Lý & Hóa")
                        .nameEn("Math & Physics & Chemistry")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals("Toán & Lý & Hóa", result.getNameVi());
        assertEquals("Math & Physics & Chemistry", result.getNameEn());
    }

    @Test
    @DisplayName("Should get tag by id with Unicode characters")
    void should_getTagById_withUnicodeCharacters() {
        // Given
        Long tagId = 6L;
        Tag tag =
                Tag.builder()
                        .id(tagId)
                        .nameVi("Tiếng Việt có dấu")
                        .nameEn("日本語, 한국어, Español")
                        .build();
        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("Tiếng Việt có dấu")
                        .nameEn("日本語, 한국어, Español")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals("Tiếng Việt có dấu", result.getNameVi());
        assertEquals("日本語, 한국어, Español", result.getNameEn());
    }

    @Test
    @DisplayName("Should throw exception when finding multiple non-existent tags")
    void should_throwException_whenFindingMultipleNonExistentTags() {
        // Given
        Long tagId1 = 100L;
        Long tagId2 = 200L;

        when(tagRepository.findById(tagId1)).thenReturn(java.util.Optional.empty());
        when(tagRepository.findById(tagId2)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tagService.getTagById(tagId1));
        assertThrows(
                com.sep.educonnect.exception.AppException.class,
                () -> tagService.getTagById(tagId2));

        verify(tagRepository).findById(tagId1);
        verify(tagRepository).findById(tagId2);
    }

    @Test
    @DisplayName("Should get multiple different tags by id successfully")
    void should_getMultipleDifferentTags_byId() {
        // Given
        Long tagId1 = 1L;
        Long tagId2 = 2L;

        Tag tag1 = Tag.builder().id(tagId1).nameVi("Tag 1").nameEn("Tag 1").build();
        Tag tag2 = Tag.builder().id(tagId2).nameVi("Tag 2").nameEn("Tag 2").build();

        TagResponse response1 =
                TagResponse.builder().id(tagId1).nameVi("Tag 1").nameEn("Tag 1").build();
        TagResponse response2 =
                TagResponse.builder().id(tagId2).nameVi("Tag 2").nameEn("Tag 2").build();

        when(tagRepository.findById(tagId1)).thenReturn(java.util.Optional.of(tag1));
        when(tagRepository.findById(tagId2)).thenReturn(java.util.Optional.of(tag2));
        when(tagMapper.toResponse(tag1)).thenReturn(response1);
        when(tagMapper.toResponse(tag2)).thenReturn(response2);

        // When
        TagResponse result1 = tagService.getTagById(tagId1);
        TagResponse result2 = tagService.getTagById(tagId2);

        // Then
        assertEquals(tagId1, result1.getId());
        assertEquals(tagId2, result2.getId());
        assertEquals("Tag 1", result1.getNameVi());
        assertEquals("Tag 2", result2.getNameVi());
    }

    @Test
    @DisplayName("Should verify repository and mapper interaction for getTagById")
    void should_verifyInteraction_forGetTagById() {
        // Given
        Long tagId = 1L;
        Tag tag = Tag.builder().id(tagId).nameVi("Test").nameEn("Test").build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Test").nameEn("Test").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        tagService.getTagById(tagId);

        // Then
        var inOrder = inOrder(tagRepository, tagMapper);
        inOrder.verify(tagRepository).findById(tagId);
        inOrder.verify(tagMapper).toResponse(tag);
    }

    @Test
    @DisplayName("Should get tag by id with empty string names")
    void should_getTagById_withEmptyStringNames() {
        // Given
        Long tagId = 7L;
        Tag tag = Tag.builder().id(tagId).nameVi("").nameEn("").build();
        TagResponse response = TagResponse.builder().id(tagId).nameVi("").nameEn("").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(response);

        // When
        TagResponse result = tagService.getTagById(tagId);

        // Then
        assertNotNull(result);
        assertEquals("", result.getNameVi());
        assertEquals("", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag successfully")
    void should_updateTag_successfully() {
        // Given
        Long tagId = 1L;
        TagRequest request =
                TagRequest.builder().nameVi("Toán học cập nhật").nameEn("Updated Math").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Toán học").nameEn("Math").build();

        Tag updatedTag =
                Tag.builder().id(tagId).nameVi("Toán học cập nhật").nameEn("Updated Math").build();

        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("Toán học cập nhật")
                        .nameEn("Updated Math")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals("Toán học cập nhật", result.getNameVi());
        assertEquals("Updated Math", result.getNameEn());

        verify(tagRepository).findById(tagId);
        verify(tagMapper).updateEntity(request, existingTag);
        verify(tagRepository).save(existingTag);
        verify(tagMapper).toResponse(updatedTag);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent tag")
    void should_throwException_when_updatingNonExistentTag() {
        // Given
        Long tagId = 999L;
        TagRequest request = TagRequest.builder().nameVi("New Name").nameEn("New Name").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.empty());

        // When & Then
        var exception =
                assertThrows(
                        com.sep.educonnect.exception.AppException.class,
                        () -> tagService.updateTag(tagId, request));
        assertEquals(
                com.sep.educonnect.exception.ErrorCode.TAG_NOT_FOUND, exception.getErrorCode());

        verify(tagRepository).findById(tagId);
        verify(tagMapper, never()).updateEntity(any(), any());
        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update tag with null Vietnamese name")
    void should_updateTag_withNullVietnameseName() {
        // Given
        Long tagId = 2L;
        TagRequest request = TagRequest.builder().nameVi(null).nameEn("Science").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Khoa học").nameEn("Science").build();

        Tag updatedTag = Tag.builder().id(tagId).nameVi(null).nameEn("Science").build();

        TagResponse response =
                TagResponse.builder().id(tagId).nameVi(null).nameEn("Science").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertNull(result.getNameVi());
        assertEquals("Science", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with null English name")
    void should_updateTag_withNullEnglishName() {
        // Given
        Long tagId = 3L;
        TagRequest request = TagRequest.builder().nameVi("Khoa học").nameEn(null).build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Khoa học").nameEn("Science").build();

        Tag updatedTag = Tag.builder().id(tagId).nameVi("Khoa học").nameEn(null).build();

        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Khoa học").nameEn(null).build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("Khoa học", result.getNameVi());
        assertNull(result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with empty strings")
    void should_updateTag_withEmptyStrings() {
        // Given
        Long tagId = 4L;
        TagRequest request = TagRequest.builder().nameVi("").nameEn("").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Old Name").nameEn("Old Name").build();

        Tag updatedTag = Tag.builder().id(tagId).nameVi("").nameEn("").build();

        TagResponse response = TagResponse.builder().id(tagId).nameVi("").nameEn("").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("", result.getNameVi());
        assertEquals("", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with special characters")
    void should_updateTag_withSpecialCharacters() {
        // Given
        Long tagId = 5L;
        TagRequest request =
                TagRequest.builder().nameVi("Toán & Lý").nameEn("Math & Physics").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Toán").nameEn("Math").build();

        Tag updatedTag =
                Tag.builder().id(tagId).nameVi("Toán & Lý").nameEn("Math & Physics").build();

        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("Toán & Lý")
                        .nameEn("Math & Physics")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("Toán & Lý", result.getNameVi());
        assertEquals("Math & Physics", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with Unicode characters")
    void should_updateTag_withUnicodeCharacters() {
        // Given
        Long tagId = 6L;
        TagRequest request =
                TagRequest.builder().nameVi("Tiếng Việt có dấu").nameEn("日本語, 한국어").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Old").nameEn("Old").build();

        Tag updatedTag =
                Tag.builder().id(tagId).nameVi("Tiếng Việt có dấu").nameEn("日本語, 한국어").build();

        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("Tiếng Việt có dấu")
                        .nameEn("日本語, 한국어")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("Tiếng Việt có dấu", result.getNameVi());
        assertEquals("日本語, 한국어", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with long names")
    void should_updateTag_withLongNames() {
        // Given
        Long tagId = 7L;
        String longViName = "Toán học đại số tuyến tính".repeat(10);
        String longEnName = "Advanced Linear Algebra".repeat(10);

        TagRequest request = TagRequest.builder().nameVi(longViName).nameEn(longEnName).build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Short").nameEn("Short").build();

        Tag updatedTag = Tag.builder().id(tagId).nameVi(longViName).nameEn(longEnName).build();

        TagResponse response =
                TagResponse.builder().id(tagId).nameVi(longViName).nameEn(longEnName).build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals(longViName, result.getNameVi());
        assertEquals(longEnName, result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag multiple times successfully")
    void should_updateTag_multipleTimes() {
        // Given
        Long tagId = 8L;

        TagRequest request1 = TagRequest.builder().nameVi("Update 1").nameEn("Update 1").build();
        TagRequest request2 = TagRequest.builder().nameVi("Update 2").nameEn("Update 2").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Original").nameEn("Original").build();

        Tag updatedTag1 = Tag.builder().id(tagId).nameVi("Update 1").nameEn("Update 1").build();
        Tag updatedTag2 = Tag.builder().id(tagId).nameVi("Update 2").nameEn("Update 2").build();

        TagResponse response1 =
                TagResponse.builder().id(tagId).nameVi("Update 1").nameEn("Update 1").build();
        TagResponse response2 =
                TagResponse.builder().id(tagId).nameVi("Update 2").nameEn("Update 2").build();

        when(tagRepository.findById(tagId))
                .thenReturn(java.util.Optional.of(existingTag))
                .thenReturn(java.util.Optional.of(updatedTag1));
        doNothing().when(tagMapper).updateEntity(any(TagRequest.class), any(Tag.class));
        when(tagRepository.save(any(Tag.class))).thenReturn(updatedTag1).thenReturn(updatedTag2);
        when(tagMapper.toResponse(updatedTag1)).thenReturn(response1);
        when(tagMapper.toResponse(updatedTag2)).thenReturn(response2);

        // When
        TagResponse result1 = tagService.updateTag(tagId, request1);
        TagResponse result2 = tagService.updateTag(tagId, request2);

        // Then
        assertEquals("Update 1", result1.getNameVi());
        assertEquals("Update 2", result2.getNameVi());
        verify(tagRepository, times(2)).findById(tagId);
        verify(tagRepository, times(2)).save(any(Tag.class));
    }

    @Test
    @DisplayName("Should verify update interaction order")
    void should_verifyUpdateInteractionOrder() {
        // Given
        Long tagId = 9L;
        TagRequest request = TagRequest.builder().nameVi("Updated").nameEn("Updated").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Original").nameEn("Original").build();
        Tag updatedTag = Tag.builder().id(tagId).nameVi("Updated").nameEn("Updated").build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Updated").nameEn("Updated").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        tagService.updateTag(tagId, request);

        // Then
        var inOrder = inOrder(tagRepository, tagMapper);
        inOrder.verify(tagRepository).findById(tagId);
        inOrder.verify(tagMapper).updateEntity(request, existingTag);
        inOrder.verify(tagRepository).save(existingTag);
        inOrder.verify(tagMapper).toResponse(updatedTag);
    }

    @Test
    @DisplayName("Should update tag with whitespace in names")
    void should_updateTag_withWhitespaceInNames() {
        // Given
        Long tagId = 10L;
        TagRequest request =
                TagRequest.builder().nameVi("  Toán học  ").nameEn("  Mathematics  ").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Old").nameEn("Old").build();

        Tag updatedTag =
                Tag.builder().id(tagId).nameVi("  Toán học  ").nameEn("  Mathematics  ").build();

        TagResponse response =
                TagResponse.builder()
                        .id(tagId)
                        .nameVi("  Toán học  ")
                        .nameEn("  Mathematics  ")
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("  Toán học  ", result.getNameVi());
        assertEquals("  Mathematics  ", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with numbers in names")
    void should_updateTag_withNumbersInNames() {
        // Given
        Long tagId = 11L;
        TagRequest request = TagRequest.builder().nameVi("Toán 12").nameEn("Math Grade 12").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Toán").nameEn("Math").build();

        Tag updatedTag = Tag.builder().id(tagId).nameVi("Toán 12").nameEn("Math Grade 12").build();

        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Toán 12").nameEn("Math Grade 12").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals("Toán 12", result.getNameVi());
        assertEquals("Math Grade 12", result.getNameEn());
    }

    @Test
    @DisplayName("Should update tag with large id value")
    void should_updateTag_withLargeIdValue() {
        // Given
        Long tagId = 999999999L;
        TagRequest request = TagRequest.builder().nameVi("Updated").nameEn("Updated").build();

        Tag existingTag = Tag.builder().id(tagId).nameVi("Old").nameEn("Old").build();
        Tag updatedTag = Tag.builder().id(tagId).nameVi("Updated").nameEn("Updated").build();
        TagResponse response =
                TagResponse.builder().id(tagId).nameVi("Updated").nameEn("Updated").build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(existingTag));
        doNothing().when(tagMapper).updateEntity(request, existingTag);
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(response);

        // When
        TagResponse result = tagService.updateTag(tagId, request);

        // Then
        assertNotNull(result);
        assertEquals(tagId, result.getId());
    }

    @Test
    @DisplayName("Should delete tag successfully (soft delete)")
    void should_deleteTag_successfully() {
        // Given
        Long tagId = 1L;
        Tag tag =
                Tag.builder()
                        .id(tagId)
                        .nameVi("Toán học")
                        .nameEn("Mathematics")
                        .isDeleted(false)
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        // When
        tagService.deleteTag(tagId);

        // Then
        verify(tagRepository).findById(tagId);
        verify(tagRepository)
                .save(
                        argThat(
                                savedTag ->
                                        savedTag.getId().equals(tagId)
                                                && savedTag.getIsDeleted().equals(true)));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent tag")
    void should_throwException_when_deletingNonExistentTag() {
        // Given
        Long tagId = 999L;
        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.empty());

        // When & Then
        var exception =
                assertThrows(
                        com.sep.educonnect.exception.AppException.class,
                        () -> tagService.deleteTag(tagId));
        assertEquals(
                com.sep.educonnect.exception.ErrorCode.TAG_NOT_FOUND, exception.getErrorCode());

        verify(tagRepository).findById(tagId);
        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should soft delete tag without affecting tag data")
    void should_softDeleteTag_withoutAffectingData() {
        // Given
        Long tagId = 2L;
        Tag tag =
                Tag.builder()
                        .id(tagId)
                        .nameVi("Khoa học")
                        .nameEn("Science")
                        .isDeleted(false)
                        .build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        // When
        tagService.deleteTag(tagId);

        // Then
        verify(tagRepository)
                .save(
                        argThat(
                                savedTag ->
                                        savedTag.getId().equals(tagId)
                                                && savedTag.getNameVi().equals("Khoa học")
                                                && savedTag.getNameEn().equals("Science")
                                                && savedTag.getIsDeleted().equals(true)));
    }

    @Test
    @DisplayName("Should delete multiple different tags successfully")
    void should_deleteMultipleTags_successfully() {
        // Given
        Long tagId1 = 1L;
        Long tagId2 = 2L;

        Tag tag1 =
                Tag.builder().id(tagId1).nameVi("Tag 1").nameEn("Tag 1").isDeleted(false).build();
        Tag tag2 =
                Tag.builder().id(tagId2).nameVi("Tag 2").nameEn("Tag 2").isDeleted(false).build();

        when(tagRepository.findById(tagId1)).thenReturn(java.util.Optional.of(tag1));
        when(tagRepository.findById(tagId2)).thenReturn(java.util.Optional.of(tag2));
        when(tagRepository.save(any(Tag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tagService.deleteTag(tagId1);
        tagService.deleteTag(tagId2);

        // Then
        verify(tagRepository, times(2)).findById(anyLong());
        verify(tagRepository, times(2)).save(any(Tag.class));
    }

    @Test
    @DisplayName("Should verify delete interaction order")
    void should_verifyDeleteInteractionOrder() {
        // Given
        Long tagId = 5L;
        Tag tag = Tag.builder().id(tagId).nameVi("Test").nameEn("Test").isDeleted(false).build();

        when(tagRepository.findById(tagId)).thenReturn(java.util.Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        // When
        tagService.deleteTag(tagId);

        // Then
        var inOrder = inOrder(tagRepository);
        inOrder.verify(tagRepository).findById(tagId);
        inOrder.verify(tagRepository).save(tag);
    }
}
