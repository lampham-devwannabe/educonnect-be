package com.sep.educonnect.service.unit;

import com.sep.educonnect.constant.Language;
import com.sep.educonnect.dto.subject.response.SubjectSyllabusResponse;
import com.sep.educonnect.dto.syllabus.request.SyllabusCreationRequest;
import com.sep.educonnect.dto.syllabus.request.SyllabusUpdateRequest;
import com.sep.educonnect.dto.syllabus.response.SyllabusResponse;
import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.entity.Syllabus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.helper.LocalizationHelper;
import com.sep.educonnect.mapper.SyllabusMapper;
import com.sep.educonnect.repository.SubjectRepository;
import com.sep.educonnect.repository.SyllabusRepository;
import com.sep.educonnect.service.SyllabusService;
import com.sep.educonnect.service.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyllabusService Unit Tests")
class SyllabusServiceTest {

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SyllabusMapper syllabusMapper;

    @Mock
    private LocalizationHelper localizationHelper;

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private SyllabusService syllabusService;

    @Test
    @DisplayName("Should create syllabus and translate fields")
    void should_createSyllabusWithTranslation() {
        // Given
        SyllabusCreationRequest request = SyllabusCreationRequest.builder()
                .subjectId(1L)
                .name("IELTS Foundation")
                .level("Intermediate")
                .target("Students aiming for IELTS 6.0")
                .description("Full course description")
                .status("ACTIVE")
                .build();

        Subject subject = Subject.builder()
                .subjectId(1L)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();

        Syllabus syllabus = Syllabus.builder()
                .syllabusId(5L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();

        SyllabusResponse expectedResponse = SyllabusResponse.builder()
                .syllabusId(5L)
                .name("IELTS Foundation")
                .build();

        TranslationService.TranslationResult translationResult = new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("target", "Sinh viên hướng tới IELTS 6.0");
        translationResult.getTranslations().put("level", "Trung cấp");
        translationResult.getTranslations().put("description", "Mô tả khóa học");

        when(subjectRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(subject));
        when(syllabusMapper.toSyllabus(request)).thenReturn(syllabus);
        when(syllabusRepository.save(syllabus)).thenReturn(syllabus);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(syllabusMapper.toSyllabusResponse(syllabus, localizationHelper)).thenReturn(expectedResponse);

        // When
        SyllabusResponse response = syllabusService.createSyllabus(request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);

        ArgumentCaptor<Syllabus> syllabusCaptor = ArgumentCaptor.forClass(Syllabus.class);
        verify(syllabusRepository, atLeast(1)).save(syllabusCaptor.capture());
        Syllabus finalSaved = syllabusCaptor.getValue();
        assertEquals("Sinh viên hướng tới IELTS 6.0", finalSaved.getTargetVi());
        assertEquals("Trung cấp", finalSaved.getLevelVi());
        assertEquals("Mô tả khóa học", finalSaved.getDescriptionVi());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when creating syllabus for missing subject")
    void should_throwWhenSubjectMissing_onCreate() {
        // Given
        SyllabusCreationRequest request = SyllabusCreationRequest.builder()
                .subjectId(99L)
                .name("Non existing")
                .build();

        when(subjectRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.createSyllabus(request, Language.VIETNAMESE));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw SUBJECT_EXISTED when update save violates constraint")
    void should_throwSyllabusExisted_when_updateDuplicate() {
        // Given
        Long syllabusId = 4L;
        SyllabusUpdateRequest request = SyllabusUpdateRequest.builder()
                .subjectId(1L)
                .name("Duplicate")
                .build();

        Syllabus existing = Syllabus.builder()
                .syllabusId(syllabusId)
                .subjectId(1L)
                .name("Old")
                .build();

        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null).when(syllabusMapper).updateSyllabus(eq(existing), eq(request));
        when(syllabusRepository.save(existing)).thenThrow(new DataIntegrityViolationException("duplicate"));

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.updateSyllabus(syllabusId, request, Language.ENGLISH));
        assertEquals(ErrorCode.SYLLABUS_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should soft delete syllabus")
    void should_softDeleteSyllabus() {
        // Given
        Long syllabusId = 9L;
        Syllabus existing = Syllabus.builder()
                .syllabusId(syllabusId)
                .subjectId(1L)
                .name("Remove me")
                .build();

        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.of(existing));
        when(syllabusRepository.save(existing)).thenReturn(existing);

        // When
        syllabusService.deleteSyllabus(syllabusId);

        // Then
        assertTrue(existing.getIsDeleted());
        verify(syllabusRepository).save(existing);
    }

    @Test
    @DisplayName("Should fetch paged subjects with syllabuses")
    void should_getAllSubjectsWithSyllabuses() {
        // Given
        Subject subject = Subject.builder()
                .subjectId(1L)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();
        Page<Subject> subjectPage = new PageImpl<>(List.of(subject));
        when(subjectRepository.findAllActiveSubjects(any(Pageable.class))).thenReturn(subjectPage);
        when(syllabusRepository.findBySubjectIdsAndNotDeleted(anyList())).thenReturn(List.of());
        SubjectSyllabusResponse mapped = SubjectSyllabusResponse.builder()
                .subjectId(1L)
                .name("English")
                .syllabuses(List.of())
                .build();
        when(syllabusMapper.toSubjectSyllabusResponse(eq(subject), anyList(), eq(localizationHelper))).thenReturn(mapped);

        // When
        Page<SubjectSyllabusResponse> result = syllabusService.getAllSubjectsWithSyllabuses(0, 10, "name", "asc");

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(mapped, result.getContent().get(0));
    }

    @Test
    @DisplayName("Should return empty page when fetching subjects with syllabuses with no results")
    void should_getAllSubjectsWithSyllabuses_returnEmptyPage() {
        // Given
        Page<Subject> subjectPage = new PageImpl<>(List.of());
        when(subjectRepository.findAllActiveSubjects(any(Pageable.class))).thenReturn(subjectPage);
        when(syllabusRepository.findBySubjectIdsAndNotDeleted(anyList())).thenReturn(List.of());

        // When
        Page<SubjectSyllabusResponse> result = syllabusService.getAllSubjectsWithSyllabuses(0, 10, "name", "asc");

        // Then
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isEmpty());
        assertEquals(0, result.getContent().size());
    }

    @Test
    @DisplayName("Should get all active syllabus with pagination")
    void should_getAllActiveSyllabus_withPagination() {
        // Given
        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();
        Syllabus syllabus2 = Syllabus.builder()
                .syllabusId(2L)
                .subjectId(1L)
                .name("IELTS Advanced")
                .build();

        Page<Syllabus> syllabusPage = new PageImpl<>(List.of(syllabus1, syllabus2));
        when(syllabusRepository.searchActiveSyllabus(any(), any(), any(), any(Pageable.class))).thenReturn(syllabusPage);

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();
        SyllabusResponse response2 = SyllabusResponse.builder()
                .syllabusId(2L)
                .name("IELTS Advanced")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);
        when(syllabusMapper.toSyllabusResponse(syllabus2, localizationHelper)).thenReturn(response2);

        // When
        Page<SyllabusResponse> result = syllabusService.getAllActiveSyllabus(0, 10, "syllabusId", "asc", null, null, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getSyllabusId());
        assertEquals(2L, result.getContent().get(1).getSyllabusId());
        verify(syllabusRepository).searchActiveSyllabus(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get all active syllabus with descending order")
    void should_getAllActiveSyllabus_withDescendingOrder() {
        // Given
        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(2L)
                .subjectId(1L)
                .name("IELTS Advanced")
                .build();
        Syllabus syllabus2 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();

        Page<Syllabus> syllabusPage = new PageImpl<>(List.of(syllabus1, syllabus2));
        when(syllabusRepository.searchActiveSyllabus(any(), any(), any(), any(Pageable.class))).thenReturn(syllabusPage);

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(2L)
                .name("IELTS Advanced")
                .build();
        SyllabusResponse response2 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);
        when(syllabusMapper.toSyllabusResponse(syllabus2, localizationHelper)).thenReturn(response2);

        // When
        Page<SyllabusResponse> result = syllabusService.getAllActiveSyllabus(0, 10, "syllabusId", "desc", null, null, null);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).getSyllabusId());
        assertEquals(1L, result.getContent().get(1).getSyllabusId());
        verify(syllabusRepository).searchActiveSyllabus(any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get syllabus by ID")
    void should_getSyllabusById() {
        // Given
        Long syllabusId = 1L;
        Syllabus syllabus = Syllabus.builder()
                .syllabusId(syllabusId)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();

        SyllabusResponse expectedResponse = SyllabusResponse.builder()
                .syllabusId(syllabusId)
                .name("IELTS Foundation")
                .build();

        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.of(syllabus));
        when(syllabusMapper.toSyllabusResponse(syllabus, localizationHelper)).thenReturn(expectedResponse);

        // When
        SyllabusResponse response = syllabusService.getSyllabusById(syllabusId);

        // Then
        assertEquals(expectedResponse, response);
        verify(syllabusRepository).findByIdAndNotDeleted(syllabusId);
    }

    @Test
    @DisplayName("Should throw SYLLABUS_NOT_EXISTED when syllabus not found by ID")
    void should_throwSyllabusNotExisted_when_getByIdNotFound() {
        // Given
        Long syllabusId = 999L;
        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.getSyllabusById(syllabusId));
        assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get syllabus by name")
    void should_getSyllabusByName() {
        // Given
        String name = "IELTS Foundation";
        Syllabus syllabus = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name(name)
                .build();

        SyllabusResponse expectedResponse = SyllabusResponse.builder()
                .syllabusId(1L)
                .name(name)
                .build();

        when(syllabusRepository.findByNameAndNotDeleted(name)).thenReturn(Optional.of(syllabus));
        when(syllabusMapper.toSyllabusResponse(syllabus, localizationHelper)).thenReturn(expectedResponse);

        // When
        SyllabusResponse response = syllabusService.getSyllabusByName(name);

        // Then
        assertEquals(expectedResponse, response);
        verify(syllabusRepository).findByNameAndNotDeleted(name);
    }

    @Test
    @DisplayName("Should throw SYLLABUS_NOT_EXISTED when syllabus not found by name")
    void should_throwSyllabusNotExisted_when_getByNameNotFound() {
        // Given
        String name = "NonExistent";
        when(syllabusRepository.findByNameAndNotDeleted(name)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.getSyllabusByName(name));
        assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should get syllabus by subject ID")
    void should_getSyllabusBySubjectId() {
        // Given
        Long subjectId = 1L;
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();

        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(subjectId)
                .name("IELTS Foundation")
                .build();
        Syllabus syllabus2 = Syllabus.builder()
                .syllabusId(2L)
                .subjectId(subjectId)
                .name("IELTS Advanced")
                .build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(syllabusRepository.findBySubjectIdAndNotDeleted(subjectId))
                .thenReturn(List.of(syllabus1, syllabus2));

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();
        SyllabusResponse response2 = SyllabusResponse.builder()
                .syllabusId(2L)
                .name("IELTS Advanced")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);
        when(syllabusMapper.toSyllabusResponse(syllabus2, localizationHelper)).thenReturn(response2);

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusBySubjectId(subjectId);

        // Then
        assertEquals(2, result.size());
        verify(subjectRepository).findByIdAndNotDeleted(subjectId);
        verify(syllabusRepository).findBySubjectIdAndNotDeleted(subjectId);
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when getting syllabus for non-existent subject")
    void should_throwSubjectNotExisted_when_getBySubjectIdNotFound() {
        // Given
        Long subjectId = 999L;
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.getSyllabusBySubjectId(subjectId));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should search syllabus by name")
    void should_searchSyllabusByName() {
        // Given
        String searchTerm = "IELTS";
        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();
        Syllabus syllabus2 = Syllabus.builder()
                .syllabusId(2L)
                .subjectId(1L)
                .name("IELTS Advanced")
                .build();

        when(syllabusRepository.findByNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of(syllabus1, syllabus2));

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();
        SyllabusResponse response2 = SyllabusResponse.builder()
                .syllabusId(2L)
                .name("IELTS Advanced")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);
        when(syllabusMapper.toSyllabusResponse(syllabus2, localizationHelper)).thenReturn(response2);

        // When
        List<SyllabusResponse> result = syllabusService.searchSyllabusByName(searchTerm);

        // Then
        assertEquals(2, result.size());
        verify(syllabusRepository).findByNameContainingAndNotDeleted(searchTerm);
    }

    @Test
    @DisplayName("Should return empty list when searching syllabus by name with no results")
    void should_searchSyllabusByName_returnEmptyList() {
        // Given
        String searchTerm = "NonExistent";
        when(syllabusRepository.findByNameContainingAndNotDeleted(searchTerm))
                .thenReturn(List.of());

        // When
        List<SyllabusResponse> result = syllabusService.searchSyllabusByName(searchTerm);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(syllabusRepository).findByNameContainingAndNotDeleted(searchTerm);
    }

    @Test
    @DisplayName("Should get syllabus by level")
    void should_getSyllabusByLevel() {
        // Given
        String level = "Intermediate";
        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .levelEn(level)
                .build();

        when(syllabusRepository.findByLevelAndNotDeleted(level)).thenReturn(List.of(syllabus1));

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusByLevel(level);

        // Then
        assertEquals(1, result.size());
        verify(syllabusRepository).findByLevelAndNotDeleted(level);
    }

    @Test
    @DisplayName("Should return empty list when getting syllabus by level with no results")
    void should_getSyllabusByLevel_returnEmptyList() {
        // Given
        String level = "Advanced Expert";
        when(syllabusRepository.findByLevelAndNotDeleted(level)).thenReturn(List.of());

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusByLevel(level);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(syllabusRepository).findByLevelAndNotDeleted(level);
    }

    @Test
    @DisplayName("Should get syllabus by status")
    void should_getSyllabusByStatus() {
        // Given
        String status = "ACTIVE";
        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .status(status)
                .build();

        when(syllabusRepository.findByStatusAndNotDeleted(status)).thenReturn(List.of(syllabus1));

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusByStatus(status);

        // Then
        assertEquals(1, result.size());
        verify(syllabusRepository).findByStatusAndNotDeleted(status);
    }

    @Test
    @DisplayName("Should return empty list when getting syllabus by status with no results")
    void should_getSyllabusByStatus_returnEmptyList() {
        // Given
        String status = "INACTIVE";
        when(syllabusRepository.findByStatusAndNotDeleted(status)).thenReturn(List.of());

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusByStatus(status);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(syllabusRepository).findByStatusAndNotDeleted(status);
    }

    @Test
    @DisplayName("Should get syllabus by subject and level")
    void should_getSyllabusBySubjectAndLevel() {
        // Given
        Long subjectId = 1L;
        String level = "Intermediate";
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();

        Syllabus syllabus1 = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(subjectId)
                .name("IELTS Foundation")
                .levelEn(level)
                .build();

        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.of(subject));
        when(syllabusRepository.findBySubjectIdAndLevelAndNotDeleted(subjectId, level))
                .thenReturn(List.of(syllabus1));

        SyllabusResponse response1 = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        when(syllabusMapper.toSyllabusResponse(syllabus1, localizationHelper)).thenReturn(response1);

        // When
        List<SyllabusResponse> result = syllabusService.getSyllabusBySubjectAndLevel(subjectId, level);

        // Then
        assertEquals(1, result.size());
        verify(subjectRepository).findByIdAndNotDeleted(subjectId);
        verify(syllabusRepository).findBySubjectIdAndLevelAndNotDeleted(subjectId, level);
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when getting syllabus by subject and level for non-existent subject")
    void should_throwSubjectNotExisted_when_getBySubjectAndLevel_subjectNotFound() {
        // Given
        Long subjectId = 999L;
        String level = "Intermediate";
        when(subjectRepository.findByIdAndNotDeleted(subjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.getSyllabusBySubjectAndLevel(subjectId, level));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should check if syllabus exists by name")
    void should_checkExistsByName() {
        // Given
        String name = "IELTS Foundation";
        when(syllabusRepository.existsByNameAndNotDeleted(name)).thenReturn(true);

        // When
        boolean exists = syllabusService.existsByName(name);

        // Then
        assertTrue(exists);
        verify(syllabusRepository).existsByNameAndNotDeleted(name);
    }

    @Test
    @DisplayName("Should count syllabus by subject")
    void should_countSyllabusBySubject() {
        // Given
        Long subjectId = 1L;
        when(syllabusRepository.countBySubjectIdAndNotDeleted(subjectId)).thenReturn(5L);

        // When
        long count = syllabusService.countSyllabusBySubject(subjectId);

        // Then
        assertEquals(5L, count);
        verify(syllabusRepository).countBySubjectIdAndNotDeleted(subjectId);
    }

    @Test
    @DisplayName("Should throw SYLLABUS_NOT_EXISTED when deleting non-existent syllabus")
    void should_throwSyllabusNotExisted_when_deleteNotFound() {
        // Given
        Long syllabusId = 999L;
        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.deleteSyllabus(syllabusId));
        assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw SUBJECT_NOT_EXISTED when updating syllabus with non-existent new subject")
    void should_throwSubjectNotExisted_when_updateWithNonExistentSubject() {
        // Given
        Long syllabusId = 1L;
        Long newSubjectId = 999L;
        SyllabusUpdateRequest request = SyllabusUpdateRequest.builder()
                .subjectId(newSubjectId)
                .name("Updated Syllabus")
                .build();

        Syllabus existing = Syllabus.builder()
                .syllabusId(syllabusId)
                .subjectId(1L)
                .name("Old Syllabus")
                .build();

        when(syllabusRepository.findByIdAndNotDeleted(syllabusId)).thenReturn(Optional.of(existing));
        when(subjectRepository.findByIdAndNotDeleted(newSubjectId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class,
                () -> syllabusService.updateSyllabus(syllabusId, request, Language.ENGLISH));
        assertEquals(ErrorCode.SUBJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle translation failure gracefully")
    void should_handleTranslationFailure_gracefully() {
        // Given
        SyllabusCreationRequest request = SyllabusCreationRequest.builder()
                .subjectId(1L)
                .name("IELTS Foundation")
                .target("Students aiming for IELTS 6.0")
                .build();

        Subject subject = Subject.builder()
                .subjectId(1L)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();

        Syllabus syllabus = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();

        SyllabusResponse expectedResponse = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        TranslationService.TranslationResult translationResult = new TranslationService.TranslationResult();
        translationResult.setSuccess(false);

        when(subjectRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(subject));
        when(syllabusMapper.toSyllabus(request)).thenReturn(syllabus);
        when(syllabusRepository.save(syllabus)).thenReturn(syllabus);
        when(translationService.translateProfileFields(anyMap(), eq("Vietnamese")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(syllabusMapper.toSyllabusResponse(syllabus, localizationHelper)).thenReturn(expectedResponse);

        // When
        SyllabusResponse response = syllabusService.createSyllabus(request, Language.ENGLISH);

        // Then
        assertEquals(expectedResponse, response);
        // Syllabus should still be created even if translation fails
        verify(syllabusRepository).save(syllabus);
    }

    @Test
    @DisplayName("Should create syllabus in Vietnamese with partial fields")
    void should_createSyllabusInVietnamese_withPartialFields() {
        // Given
        SyllabusCreationRequest request = SyllabusCreationRequest.builder()
                .subjectId(1L)
                .name("IELTS Foundation")
                .target("Sinh viên hướng tới IELTS 6.0")
                .level(null)
                .description(null)
                .build();

        Subject subject = Subject.builder()
                .subjectId(1L)
                .nameEn("English")
                .nameVi("Tiếng Anh")
                .build();

        Syllabus syllabus = Syllabus.builder()
                .syllabusId(1L)
                .subjectId(1L)
                .name("IELTS Foundation")
                .build();

        SyllabusResponse expectedResponse = SyllabusResponse.builder()
                .syllabusId(1L)
                .name("IELTS Foundation")
                .build();

        TranslationService.TranslationResult translationResult = new TranslationService.TranslationResult();
        translationResult.setSuccess(true);
        translationResult.getTranslations().put("target", "Students aiming for IELTS 6.0");

        when(subjectRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(subject));
        when(syllabusMapper.toSyllabus(request)).thenReturn(syllabus);
        when(syllabusRepository.save(syllabus)).thenReturn(syllabus);
        when(translationService.translateProfileFields(anyMap(), eq("English")))
                .thenReturn(CompletableFuture.completedFuture(translationResult));
        when(syllabusMapper.toSyllabusResponse(syllabus, localizationHelper)).thenReturn(expectedResponse);

        // When
        SyllabusResponse response = syllabusService.createSyllabus(request, Language.VIETNAMESE);

        // Then
        assertEquals(expectedResponse, response);
        verify(translationService).translateProfileFields(anyMap(), eq("English"));
    }
}
