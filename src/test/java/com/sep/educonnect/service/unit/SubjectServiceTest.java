package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.constant.Language;
import com.sep.educonnect.dto.subject.request.SubjectCreationRequest;
import com.sep.educonnect.dto.subject.request.SubjectUpdateRequest;
import com.sep.educonnect.dto.subject.response.SubjectResponse;
import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.helper.LocalizationHelper;
import com.sep.educonnect.mapper.SubjectMapper;
import com.sep.educonnect.repository.SubjectRepository;
import com.sep.educonnect.service.SubjectService;
import com.sep.educonnect.service.TranslationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService Unit Tests")
class SubjectServiceTest {

    @Mock private SubjectRepository subjectRepository;

    @Mock private SubjectMapper subjectMapper;

    @Mock private LocalizationHelper localizationHelper;

    @Mock private TranslationService translationService;

    @InjectMocks private SubjectService subjectService;

    @Test
    @DisplayName("Should create subject in Vietnamese and trigger translation")
    void should_createSubjectWithVietnameseTranslation() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Toán").build();

        Subject subject = Subject.builder().subjectId(1L).nameVi("Toán").nameEn("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Toán").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Math");

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.VIETNAMESE);

        // Then
        assertEquals(expectedResponse, response);

        ArgumentCaptor<Subject> subjectCaptor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository, atLeast(1)).save(subjectCaptor.capture());
        List<Subject> savedSubjects = subjectCaptor.getAllValues();
        Subject finalSavedSubject = savedSubjects.get(savedSubjects.size() - 1);
        assertEquals("Math", finalSavedSubject.getNameEn());
        verify(translationService).translateProfileFields(anyMap(), eq("English"));
    }

    @Test
    @DisplayName("Should throw SUBJECT_EXISTED when duplicate subject on create")
    void should_throwSubjectExisted_when_createDuplicate() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> subjectService.createSubject(request, Language.ENGLISH));
        assertEquals(ErrorCode.SUBJECT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should update subject and translate English to Vietnamese")
    void should_updateSubjectWithTranslation() {
        // Given
        Long subjectId = 5L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Physics").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old Name").nameVi("Tên cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Vật lý");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Physics").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Physics")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        assertEquals("Physics", existingSubject.getNameEn());
        verify(subjectRepository, atLeast(1)).save(existingSubject);
        assertEquals("Vật lý", existingSubject.getNameVi());
    }

    @Test
    @DisplayName("Should throw SUBJECT_EXISTED when updating with duplicate name")
    void should_throwSubjectExisted_when_updateDuplicateName() {
        // Given
        Long subjectId = 10L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Chemistry").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Chemistry")).thenReturn(true);

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> subjectService.updateSubject(subjectId, request, Language.ENGLISH));
        assertEquals(ErrorCode.SUBJECT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should soft delete subject")
    void should_softDeleteSubject() {
        // Given
        Long subjectId = 8L;
        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("History").nameVi("Lịch sử").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);

        // When
        subjectService.deleteSubject(subjectId);

        // Then
        assertTrue(existingSubject.getIsDeleted());
        verify(subjectRepository).save(existingSubject);
    }

    @Test
    @DisplayName("Should get all active subjects with pagination")
    void should_getAllActiveSubjects_withPagination() {
        // Given
        Subject subject1 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();
        Subject subject2 =
                Subject.builder().subjectId(2L).nameEn("Physics").nameVi("Vật lý").build();

        Page<Subject> subjectPage = new PageImpl<>(List.of(subject1, subject2));
        when(subjectRepository.searchActiveSubjects(any(), any(Pageable.class))).thenReturn(subjectPage);

        SubjectResponse response1 = SubjectResponse.builder().subjectId(1L).name("Math").build();
        SubjectResponse response2 = SubjectResponse.builder().subjectId(2L).name("Physics").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        Page<SubjectResponse> result =
                subjectService.getAllActiveSubjects(0, 10, "subjectId", "asc", null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(subjectRepository).searchActiveSubjects(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no active subjects")
    void should_returnEmptyPage_when_noActiveSubjects() {
        // Given
        Page<Subject> emptyPage = new PageImpl<>(List.of());
        when(subjectRepository.searchActiveSubjects(any(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<SubjectResponse> result =
                subjectService.getAllActiveSubjects(0, 10, "subjectId", "asc", null);

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(subjectRepository).searchActiveSubjects(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get subjects with descending sort")
    void should_getSubjects_withDescendingSort() {
        // Given
        Subject subject1 =
                Subject.builder().subjectId(2L).nameEn("Physics").nameVi("Vật lý").build();
        Subject subject2 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();

        Page<Subject> subjectPage = new PageImpl<>(List.of(subject1, subject2));
        when(subjectRepository.searchActiveSubjects(any(), any(Pageable.class))).thenReturn(subjectPage);

        SubjectResponse response1 = SubjectResponse.builder().subjectId(2L).name("Physics").build();
        SubjectResponse response2 = SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        Page<SubjectResponse> result =
                subjectService.getAllActiveSubjects(0, 10, "subjectId", "desc", null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getSubjectId());
        assertEquals(1L, result.getContent().get(1).getSubjectId());
        verify(subjectRepository).searchActiveSubjects(any(), any(Pageable.class));
    }


    @Test
    @DisplayName("Should get subject by ID")
    void should_getSubjectById() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder().subjectId(subjectId).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Math").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectById(subjectId);

        // Then
        assertEquals(expectedResponse, response);
        verify(subjectRepository).findByIdAndNotDeleted(subjectId);
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when subject not found by ID")
    void should_throwSubjectNotExisted_when_getByIdNotFound() {
        // Given
        Long subjectId = 999L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.getSubjectById(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get subject with ID 1")
    void should_getSubject_withId1() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder().subjectId(subjectId).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Math").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectById(subjectId);

        // Then
        assertEquals(1L, response.getSubjectId());
        assertEquals("Math", response.getName());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for ID zero")
    void should_throwSubjectNotExisted_forIdZero() {
        // Given
        Long subjectId = 0L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.getSubjectById(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for negative ID")
    void should_throwSubjectNotExisted_forNegativeId() {
        // Given
        Long subjectId = -1L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.getSubjectById(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for null ID")
    void should_throwSubjectNotExisted_forNullId() {
        // Given
        when(subjectRepository.findByIdAndNotDeleted(null)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.getSubjectById(null));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should verify repository is called once")
    void should_verifyRepository_calledOnce() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder().subjectId(subjectId).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Math").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        subjectService.getSubjectById(subjectId);

        // Then
        verify(subjectRepository, times(1)).findByIdAndNotDeleted(subjectId);
        verify(subjectMapper, times(1)).toSubjectResponse(subject, localizationHelper);
    }

    @Test
    @DisplayName("Should verify mapper is called with correct parameters")
    void should_verifyMapper_calledWithCorrectParameters() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder().subjectId(subjectId).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Math").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        subjectService.getSubjectById(subjectId);

        // Then
        ArgumentCaptor<Subject> subjectCaptor = ArgumentCaptor.forClass(Subject.class);
        ArgumentCaptor<LocalizationHelper> helperCaptor =
                ArgumentCaptor.forClass(LocalizationHelper.class);
        verify(subjectMapper).toSubjectResponse(subjectCaptor.capture(), helperCaptor.capture());

        assertEquals(subject, subjectCaptor.getValue());
        assertEquals(localizationHelper, helperCaptor.getValue());
    }

    @Test
    @DisplayName("Should get different subjects by different IDs")
    void should_getDifferentSubjects_byDifferentIds() {
        // Given
        Long subjectId1 = 1L;
        Long subjectId2 = 2L;

        Subject subject1 =
                Subject.builder().subjectId(subjectId1).nameEn("Math").nameVi("Toán").build();

        Subject subject2 =
                Subject.builder().subjectId(subjectId2).nameEn("Physics").nameVi("Vật lý").build();

        SubjectResponse response1 =
                SubjectResponse.builder().subjectId(subjectId1).name("Math").build();

        SubjectResponse response2 =
                SubjectResponse.builder().subjectId(subjectId2).name("Physics").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId1)).thenReturn(Optional.of(subject1));
        when(subjectRepository.findByIdAndNotDeleted(subjectId2)).thenReturn(Optional.of(subject2));
        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        SubjectResponse result1 = subjectService.getSubjectById(subjectId1);
        SubjectResponse result2 = subjectService.getSubjectById(subjectId2);

        // Then
        assertEquals("Math", result1.getName());
        assertEquals("Physics", result2.getName());
    }

    @Test
    @DisplayName("Should get subject by name")
    void should_getSubjectByName() {
        // Given
        String subjectName = "Math";
        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectByName(subjectName);

        // Then
        assertEquals(expectedResponse, response);
        verify(subjectRepository).findBySubjectNameAndNotDeleted(subjectName);
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when subject not found by name")
    void should_throwSubjectNotExisted_when_getByNameNotFound() {
        // Given
        String subjectName = "NonExistent";
        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> subjectService.getSubjectByName(subjectName));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get subject by Vietnamese name")
    void should_getSubjectBy_vietnameseName() {
        // Given
        String subjectName = "Toán";
        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Toán").build();

        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectByName(subjectName);

        // Then
        assertEquals("Toán", response.getName());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for null name")
    void should_throwSubjectNotExisted_forNullName() {
        // Given
        when(subjectRepository.findBySubjectNameAndNotDeleted(null)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.getSubjectByName(null));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for name with only whitespace")
    void should_throwSubjectNotExisted_forNameWithOnlyWhitespace() {
        // Given
        String subjectName = "   ";
        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class, () -> subjectService.getSubjectByName(subjectName));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get subject with name containing numbers")
    void should_getSubject_withNameContainingNumbers() {
        // Given
        String subjectName = "Math 101";
        Subject subject =
                Subject.builder().subjectId(1L).nameEn("Math 101").nameVi("Toán 101").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math 101").build();

        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectByName(subjectName);

        // Then
        assertEquals("Math 101", response.getName());
    }

    @Test
    @DisplayName("Should get subject with name containing hyphen")
    void should_getSubject_withNameContainingHyphen() {
        // Given
        String subjectName = "Pre-Calculus";
        Subject subject =
                Subject.builder()
                        .subjectId(1L)
                        .nameEn("Pre-Calculus")
                        .nameVi("Tiền Giải Tích")
                        .build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Pre-Calculus").build();

        when(subjectRepository.findBySubjectNameAndNotDeleted(subjectName))
                .thenReturn(Optional.of(subject));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.getSubjectByName(subjectName);

        // Then
        assertEquals("Pre-Calculus", response.getName());
    }

    @Test
    @DisplayName("Should search subjects by name")
    void should_searchSubjectsByName() {
        // Given
        String searchTerm = "Math";
        Subject subject1 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();
        Subject subject2 =
                Subject.builder()
                        .subjectId(2L)
                        .nameEn("Advanced Math")
                        .nameVi("Toán nâng cao")
                        .build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject1, subject2));

        SubjectResponse response1 = SubjectResponse.builder().subjectId(1L).name("Math").build();
        SubjectResponse response2 =
                SubjectResponse.builder().subjectId(2L).name("Advanced Math").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(2, result.size());
        verify(subjectRepository).findBySubjectNameContainingAndNotDeleted(searchTerm);
    }

    @Test
    @DisplayName("Should return empty list when no subjects found by search")
    void should_returnEmptyList_when_searchNoResults() {
        // Given
        String searchTerm = "NonExistent";
        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of());

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should search subjects with partial name match")
    void should_searchSubjects_withPartialNameMatch() {
        // Given
        String searchTerm = "Mat";
        Subject subject1 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();
        Subject subject2 =
                Subject.builder().subjectId(2L).nameEn("Mathematics").nameVi("Toán học").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject1, subject2));

        SubjectResponse response1 = SubjectResponse.builder().subjectId(1L).name("Math").build();
        SubjectResponse response2 =
                SubjectResponse.builder().subjectId(2L).name("Mathematics").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Math")));
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Mathematics")));
    }

    @Test
    @DisplayName("Should search subjects with empty string")
    void should_searchSubjects_withEmptyString() {
        // Given
        String searchTerm = "";
        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response = SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should search subjects with null string")
    void should_searchSubjects_withNullString() {
        // Given
        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(null))
                .thenReturn(List.of());

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should search subjects case sensitive")
    void should_searchSubjects_caseSensitive() {
        // Given
        String searchTerm = "math";
        Subject subject =
                Subject.builder().subjectId(1L).nameEn("Mathematics").nameVi("Toán học").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response =
                SubjectResponse.builder().subjectId(1L).name("Mathematics").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
        assertEquals("Mathematics", result.get(0).getName());
    }

    @Test
    @DisplayName("Should search subjects with Vietnamese search term")
    void should_searchSubjects_withVietnameseSearchTerm() {
        // Given
        String searchTerm = "Toán";
        Subject subject1 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();
        Subject subject2 =
                Subject.builder().subjectId(2L).nameEn("Mathematics").nameVi("Toán học").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject1, subject2));

        SubjectResponse response1 = SubjectResponse.builder().subjectId(1L).name("Toán").build();
        SubjectResponse response2 =
                SubjectResponse.builder().subjectId(2L).name("Toán học").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should search subjects with whitespace")
    void should_searchSubjects_withWhitespace() {
        // Given
        String searchTerm = "  Math  ";
        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response = SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should search subjects with special characters")
    void should_searchSubjects_withSpecialCharacters() {
        // Given
        String searchTerm = "Math &";
        Subject subject =
                Subject.builder()
                        .subjectId(1L)
                        .nameEn("Math & Physics")
                        .nameVi("Toán & Vật lý")
                        .build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response =
                SubjectResponse.builder().subjectId(1L).name("Math & Physics").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
        assertEquals("Math & Physics", result.get(0).getName());
    }

    @Test
    @DisplayName("Should search subjects with unicode characters")
    void should_searchSubjects_withUnicodeCharacters() {
        // Given
        String searchTerm = "数";
        Subject subject =
                Subject.builder().subjectId(1L).nameEn("Mathematics").nameVi("数学").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response = SubjectResponse.builder().subjectId(1L).name("数学").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should search subjects with single character")
    void should_searchSubjects_withSingleCharacter() {
        // Given
        String searchTerm = "M";
        Subject subject1 = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Toán").build();
        Subject subject2 =
                Subject.builder().subjectId(2L).nameEn("Music").nameVi("Âm nhạc").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject1, subject2));

        SubjectResponse response1 = SubjectResponse.builder().subjectId(1L).name("Math").build();
        SubjectResponse response2 = SubjectResponse.builder().subjectId(2L).name("Music").build();

        when(subjectMapper.toSubjectResponse(subject1, localizationHelper)).thenReturn(response1);
        when(subjectMapper.toSubjectResponse(subject2, localizationHelper)).thenReturn(response2);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should search subjects with numeric search term")
    void should_searchSubjects_withNumericSearchTerm() {
        // Given
        String searchTerm = "101";
        Subject subject =
                Subject.builder().subjectId(1L).nameEn("Math 101").nameVi("Toán 101").build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response = SubjectResponse.builder().subjectId(1L).name("Math 101").build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
        assertEquals("Math 101", result.get(0).getName());
    }

    @Test
    @DisplayName("Should verify repository called once")
    void should_verifyRepository_calledOnceForSearch() {
        // Given
        String searchTerm = "Math";
        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of());

        // When
        subjectService.searchSubjectsByName(searchTerm);

        // Then
        verify(subjectRepository, times(1)).findBySubjectNameContainingAndNotDeleted(searchTerm);
    }


    @Test
    @DisplayName("Should search subjects with long search term")
    void should_searchSubjects_withLongSearchTerm() {
        // Given
        String searchTerm = "Advanced Computer Science and Information Technology";
        Subject subject =
                Subject.builder()
                        .subjectId(1L)
                        .nameEn("Advanced Computer Science and Information Technology")
                        .nameVi("Khoa học máy tính và công nghệ thông tin nâng cao")
                        .build();

        when(subjectRepository.findBySubjectNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(subject));

        SubjectResponse response =
                SubjectResponse.builder()
                        .subjectId(1L)
                        .name("Advanced Computer Science and Information Technology")
                        .build();

        when(subjectMapper.toSubjectResponse(subject, localizationHelper)).thenReturn(response);

        // When
        List<SubjectResponse> result = subjectService.searchSubjectsByName(searchTerm);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should return false when subject does not exist by name")
    void should_returnFalse_when_subjectNotExists() {
        // Given
        String subjectName = "NonExistent";
        when(subjectRepository.existsBySubjectNameAndNotDeleted(subjectName)).thenReturn(false);

        // When
        boolean exists = subjectService.existsByName(subjectName);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should create subject in English and trigger translation")
    void should_createSubjectWithEnglishTranslation() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Toán");

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        verify(translationService).translateProfileFields(anyMap(), eq("Vietnamese"));
    }

    @Test
    @DisplayName("Should handle translation failure gracefully")
    void should_handleTranslationFailure_gracefully() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(false);

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.VIETNAMESE);

        // Then
        assertEquals(expectedResponse, response);
        // Subject should still be created even if translation fails
        verify(subjectRepository).save(subject);
    }

    @Test
    @DisplayName("Should update subject with null subjectName")
    void should_updateSubject_withNullSubjectName() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName(null).build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old Name").nameVi("Tên cũ").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Old Name").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted(null)).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        // No translation should be triggered when subjectName is null
        verify(translationService, never()).translateProfileFields(anyMap(), anyString());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when updating non-existent subject")
    void should_throwSubjectNotExisted_when_updateNonExistent() {
        // Given
        Long subjectId = 999L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("NewName").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> subjectService.updateSubject(subjectId, request, Language.ENGLISH));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update subject with Vietnamese name")
    void should_updateSubject_withVietnameseName() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Toán học").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Mathematics");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Toán học").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Toán học")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.VIETNAMESE);

        // Then
        assertEquals(expectedResponse, response);
        assertEquals("Toán học", existingSubject.getNameVi());
        verify(translationService).translateProfileFields(anyMap(), eq("English"));
    }

    @Test
    @DisplayName("Should update subject with empty string name")
    void should_updateSubject_withEmptyStringName() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName("").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Old").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        // No translation when name is empty
        verify(translationService, never()).translateProfileFields(anyMap(), anyString());
    }

    @Test
    @DisplayName("Should update subject with special characters in name")
    void should_updateSubject_withSpecialCharactersInName() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Math & Physics").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Toán & Vật lý");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Math & Physics").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Math & Physics"))
                .thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        assertEquals("Math & Physics", existingSubject.getNameEn());
    }

    @Test
    @DisplayName("Should update subject with very long name")
    void should_updateSubject_withVeryLongName() {
        // Given
        Long subjectId = 1L;
        String longName = "A".repeat(255);
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName(longName).build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", longName);

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name(longName).build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted(longName)).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        assertEquals(255, existingSubject.getNameEn().length());
    }

    @Test
    @DisplayName("Should handle translation failure gracefully in update")
    void should_handleTranslationFailure_inUpdate() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Physics").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(false);

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Physics").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Physics")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        // Subject should still be updated even if translation fails
        verify(subjectRepository, atLeast(1)).save(existingSubject);
    }

    @Test
    @DisplayName("Should handle translation exception in update")
    void should_handleTranslationException_inUpdate() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Chemistry").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        CompletableFuture<TranslationService.TranslationResult> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Translation error"));

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Chemistry").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Chemistry")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(failedFuture);
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Should handle translation returning null translated name in update")
    void should_handleTranslationReturningNull_inUpdate() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("Biology").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", null);

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Biology").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Biology")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Should update subject with null language defaults to English handling")
    void should_updateSubject_withNullLanguage() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request =
                SubjectUpdateRequest.builder().subjectName("History").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Lịch sử");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("History").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("History")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.updateSubject(subjectId, request, null);

        // Then
        assertEquals(expectedResponse, response);
        // Should default to English behavior (not VIETNAMESE)
        assertEquals("History", existingSubject.getNameEn());
    }

    @Test
    @DisplayName("Should verify mapper updateSubject is called")
    void should_verifyMapper_updateSubjectCalled() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName("Art").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Art").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Art")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new TranslationService.TranslationResult()));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        verify(subjectMapper, times(1)).updateSubject(existingSubject, request);
        verify(subjectMapper, times(1)).toSubjectResponse(existingSubject, localizationHelper);
    }

    @Test
    @DisplayName("Should update subject and save at least once")
    void should_updateSubject_andSaveAtLeastOnce() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName("Music").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Âm nhạc");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("Music").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("Music")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        verify(subjectRepository, atLeast(1)).save(existingSubject);
    }

    @Test
    @DisplayName("Should update subject with unicode characters")
    void should_updateSubject_withUnicodeCharacters() {
        // Given
        Long subjectId = 1L;
        SubjectUpdateRequest request = SubjectUpdateRequest.builder().subjectName("数学").build();

        Subject existingSubject =
                Subject.builder().subjectId(subjectId).nameEn("Old").nameVi("Cũ").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Mathematics");

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(subjectId).name("数学").build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId))
                .thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectNameAndNotDeleted("数学")).thenReturn(false);
        doAnswer(invocation -> null)
                .when(subjectMapper)
                .updateSubject(eq(existingSubject), eq(request));
        when(subjectRepository.save(existingSubject)).thenReturn(existingSubject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(existingSubject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request, Language.VIETNAMESE);

        // Then
        assertEquals(expectedResponse, response);
        assertEquals("数学", existingSubject.getNameVi());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when deleting non-existent subject")
    void should_throwSubjectNotExisted_when_deleteNotFound() {
        // Given
        Long subjectId = 999L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.deleteSubject(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    // ========== Additional comprehensive test cases for deleteSubject ==========

    @Test
    @DisplayName("Should delete subject with ID 1")
    void should_deleteSubject_withId1() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder()
                        .subjectId(subjectId)
                        .nameEn("Math")
                        .nameVi("Toán")
                        .isDeleted(false)
                        .build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);

        // When
        subjectService.deleteSubject(subjectId);

        // Then
        assertTrue(subject.getIsDeleted());
        verify(subjectRepository).findByIdAndNotDeleted(subjectId);
        verify(subjectRepository).save(subject);
    }

    @Test
    @DisplayName("Should delete subject with very large ID")
    void should_deleteSubject_withVeryLargeId() {
        // Given
        Long subjectId = Long.MAX_VALUE;
        Subject subject =
                Subject.builder()
                        .subjectId(subjectId)
                        .nameEn("Math")
                        .nameVi("Toán")
                        .isDeleted(false)
                        .build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);

        // When
        subjectService.deleteSubject(subjectId);

        // Then
        assertTrue(subject.getIsDeleted());
        verify(subjectRepository).save(subject);
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for ID zero")
    void should_throwSubjectNotExisted_forIdZeroOnDelete() {
        // Given
        Long subjectId = 0L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.deleteSubject(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for negative ID on delete")
    void should_throwSubjectNotExisted_forNegativeIdOnDelete() {
        // Given
        Long subjectId = -1L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.deleteSubject(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED for null ID on delete")
    void should_throwSubjectNotExisted_forNullIdOnDelete() {
        // Given
        when(subjectRepository.findByIdAndNotDeleted(null)).thenReturn(Optional.empty());

        // When & Then
        AppException exception =
                assertThrows(AppException.class, () -> subjectService.deleteSubject(null));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete subject regardless of current isDeleted state")
    void should_deleteSubject_regardlessOfCurrentState() {
        // Given
        Long subjectId = 1L;
        Subject subject =
                Subject.builder()
                        .subjectId(subjectId)
                        .nameEn("Math")
                        .nameVi("Toán")
                        .isDeleted(false)
                        .build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(subject)).thenReturn(subject);

        // When
        subjectService.deleteSubject(subjectId);

        // Then
        assertTrue(subject.getIsDeleted());
    }

    @Test
    @DisplayName("Should create subject with special characters in name")
    void should_createSubject_withSpecialCharacters() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math & Physics").build();

        Subject subject =
                Subject.builder()
                        .subjectId(1L)
                        .nameEn("Math & Physics")
                        .nameVi("Math & Physics")
                        .build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math & Physics").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Toán & Vật lý");

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Should create subject with very long name")
    void should_createSubject_withVeryLongName() {
        // Given
        String longName = "A".repeat(255);
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName(longName).build();

        Subject subject = Subject.builder().subjectId(1L).nameVi(longName).nameEn(longName).build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name(longName).build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", longName);

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.VIETNAMESE);

        // Then
        assertNotNull(response);
        assertEquals(longName, response.getName());
    }

    @Test
    @DisplayName("Should create subject with unicode characters")
    void should_createSubject_withUnicodeCharacters() {
        // Given
        SubjectCreationRequest request = SubjectCreationRequest.builder().subjectName("数学").build();

        Subject subject = Subject.builder().subjectId(1L).nameVi("数学").nameEn("数学").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("数学").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "Mathematics");

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.VIETNAMESE);

        // Then
        assertNotNull(response);
        assertEquals("数学", response.getName());
    }

    @Test
    @DisplayName("Should handle translation returning null translated name")
    void should_handleTranslation_withNullTranslatedName() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", null);

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        // Translation should not update the subject when translated name is null
    }

    @Test
    @DisplayName("Should handle translation returning blank translated name")
    void should_handleTranslation_withBlankTranslatedName() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        TranslationService.TranslationResult translationResult =
                new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("name", "   ");

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Should handle translation exception")
    void should_handleTranslation_withException() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        CompletableFuture<TranslationService.TranslationResult> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Translation service error"));

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(failedFuture);
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        // Subject should still be created even if translation fails
    }

    @Test
    @DisplayName("Should create subject with mixed case in language parameter")
    void should_createSubject_withMixedCaseLanguage() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new TranslationService.TranslationResult()));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When - Using lowercase language
        SubjectResponse response = subjectService.createSubject(request, "english");

        // Then
        assertNotNull(response);
        // Should default to setting nameEn since it doesn't match Language.VIETNAMESE
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void should_verifyMapperCalled_correctly() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new TranslationService.TranslationResult()));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        subjectService.createSubject(request, Language.ENGLISH);

        // Then
        verify(subjectMapper).toSubject(request);
        verify(subjectMapper).toSubjectResponse(subject, localizationHelper);
    }

    @Test
    @DisplayName("Should save subject before translation")
    void should_saveSubject_beforeTranslation() {
        // Given
        SubjectCreationRequest request =
                SubjectCreationRequest.builder().subjectName("Math").build();

        Subject subject = Subject.builder().subjectId(1L).nameEn("Math").nameVi("Math").build();

        SubjectResponse expectedResponse =
                SubjectResponse.builder().subjectId(1L).name("Math").build();

        when(subjectMapper.toSubject(request)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                new TranslationService.TranslationResult()));
        when(subjectMapper.toSubjectResponse(subject, localizationHelper))
                .thenReturn(expectedResponse);

        // When
        SubjectResponse response = subjectService.createSubject(request, Language.ENGLISH);

        // Then
        assertNotNull(response);
        // Verify repository save is called before translation service
        verify(subjectRepository).save(subject);
        verify(translationService).translateProfileFields(anyMap(), eq("Vietnamese"));
    }
}
