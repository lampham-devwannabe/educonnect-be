package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.module.ModuleRequest;
import com.sep.educonnect.dto.module.ModuleResponse;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.mapper.ModuleMapper;
import com.sep.educonnect.repository.ModuleRepository;
import com.sep.educonnect.repository.SyllabusRepository;
import com.sep.educonnect.service.ModuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService Unit Tests")
class ModuleServiceTest {

        @Mock
        private ModuleRepository moduleRepository;

        @Mock
        private SyllabusRepository syllabusRepository;

        @Mock
        private ModuleMapper moduleMapper;

        @InjectMocks
        private ModuleService moduleService;

        @Test
        @DisplayName("Should get modules by syllabus with pagination and ascending sort")
        void should_getModulesBySyllabus_withAscendingSort() {
                // Given
                Long syllabusId = 1L;
                int page = 0;
                int size = 10;
                String sortBy = "orderNumber";
                String direction = "asc";

                Module module1 = Module.builder()
                                .moduleId(1L)
                                .syllabusId(syllabusId)
                                .title("Module 1")
                                .orderNumber(1)
                                .build();

                Module module2 = Module.builder()
                                .moduleId(2L)
                                .syllabusId(syllabusId)
                                .title("Module 2")
                                .orderNumber(2)
                                .build();

                List<Module> modules = List.of(module1, module2);
                Page<Module> modulePage = new PageImpl<>(modules, PageRequest.of(page, size), modules.size());

                ModuleResponse response1 = ModuleResponse.builder()
                                .moduleId(1L)
                                .syllabusId(syllabusId)
                                .title("Module 1")
                                .orderNumber(1)
                                .build();

                ModuleResponse response2 = ModuleResponse.builder()
                                .moduleId(2L)
                                .syllabusId(syllabusId)
                                .title("Module 2")
                                .orderNumber(2)
                                .build();

                when(moduleRepository.findBySyllabusIdOrderByOrderNumberAscWithPaging(
                                eq(syllabusId), any(Pageable.class))).thenReturn(modulePage);
                when(moduleMapper.toResponse(module1)).thenReturn(response1);
                when(moduleMapper.toResponse(module2)).thenReturn(response2);

                // When
                Page<ModuleResponse> result = moduleService.getModulesBySyllabus(syllabusId, page, size, sortBy,
                                direction);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(1L, result.getContent().get(0).getModuleId());
                assertEquals(2L, result.getContent().get(1).getModuleId());
                verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAscWithPaging(eq(syllabusId),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should get modules by syllabus with descending sort")
        void should_getModulesBySyllabus_withDescendingSort() {
                // Given
                Long syllabusId = 1L;
                int page = 0;
                int size = 10;
                String sortBy = "orderNumber";
                String direction = "desc";

                Module module1 = Module.builder()
                                .moduleId(1L)
                                .syllabusId(syllabusId)
                                .title("Module 1")
                                .orderNumber(1)
                                .build();

                List<Module> modules = List.of(module1);
                Page<Module> modulePage = new PageImpl<>(modules);

                ModuleResponse response1 = ModuleResponse.builder()
                                .moduleId(1L)
                                .syllabusId(syllabusId)
                                .title("Module 1")
                                .orderNumber(1)
                                .build();

                when(moduleRepository.findBySyllabusIdOrderByOrderNumberAscWithPaging(
                                eq(syllabusId), any(Pageable.class))).thenReturn(modulePage);
                when(moduleMapper.toResponse(module1)).thenReturn(response1);

                // When
                Page<ModuleResponse> result = moduleService.getModulesBySyllabus(syllabusId, page, size, sortBy,
                                direction);

                // Then
                assertNotNull(result);
                verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAscWithPaging(eq(syllabusId),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should get module by ID successfully")
        void should_getById_successfully() {
                // Given
                Long moduleId = 1L;
                Module module = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Test Module")
                                .orderNumber(1)
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

                // When
                Optional<Module> result = moduleService.getById(moduleId);

                // Then
                assertTrue(result.isPresent());
                assertEquals(moduleId, result.get().getModuleId());
                verify(moduleRepository).findById(moduleId);
        }

        @Test
        @DisplayName("Should return empty Optional when module not found by ID")
        void should_returnEmptyOptional_when_moduleNotFound() {
                // Given
                Long moduleId = 999L;
                when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                // When
                Optional<Module> result = moduleService.getById(moduleId);

                // Then
                assertTrue(result.isEmpty());
                verify(moduleRepository).findById(moduleId);
        }

        @Test
        @DisplayName("Should get module response by ID successfully")
        void should_getResponseById_successfully() {
                // Given
                Long moduleId = 1L;
                Module module = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Test Module")
                                .orderNumber(1)
                                .build();

                ModuleResponse expectedResponse = ModuleResponse.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Test Module")
                                .orderNumber(1)
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
                when(moduleMapper.toResponse(module)).thenReturn(expectedResponse);

                // When
                ModuleResponse result = moduleService.getResponseById(moduleId);

                // Then
                assertNotNull(result);
                assertEquals(moduleId, result.getModuleId());
                assertEquals("Test Module", result.getTitle());
                verify(moduleRepository).findById(moduleId);
                verify(moduleMapper).toResponse(module);
        }

        @Test
        @DisplayName("Should throw AppException when module not found for getResponseById")
        void should_throwException_when_moduleNotFoundForGetResponseById() {
                // Given
                Long moduleId = 999L;
                when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> {
                        moduleService.getResponseById(moduleId);
                });

                assertEquals(ErrorCode.MODULE_NOT_EXISTED, exception.getErrorCode());
                verify(moduleRepository).findById(moduleId);
                verify(moduleMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should create module with orderNumber provided")
        void should_createModule_withOrderNumberProvided() {
                // Given
                ModuleRequest request = ModuleRequest.builder()
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(5)
                                .status("DRAFT")
                                .build();

                Module module = Module.builder()
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(5)
                                .status("DRAFT")
                                .build();

                Module savedModule = Module.builder()
                                .moduleId(1L)
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(5)
                                .status("DRAFT")
                                .build();

                ModuleResponse expectedResponse = ModuleResponse.builder()
                                .moduleId(1L)
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(5)
                                .status("DRAFT")
                                .build();

                when(moduleMapper.toEntity(request)).thenReturn(module);
                when(syllabusRepository.findById(1L)).thenReturn(Optional.of(new com.sep.educonnect.entity.Syllabus()));
                when(moduleRepository.save(module)).thenReturn(savedModule);
                when(moduleMapper.toResponse(savedModule)).thenReturn(expectedResponse);

                // When
                ModuleResponse result = moduleService.create(request);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getModuleId());
                assertEquals("New Module", result.getTitle());
                assertEquals(5, result.getOrderNumber());
                verify(moduleMapper).toEntity(request);
                verify(moduleRepository).save(module);
                verify(moduleRepository, never()).findBySyllabusIdOrderByOrderNumberAsc(anyLong());
                verify(moduleMapper).toResponse(savedModule);
        }

        @Test
        @DisplayName("Should create module with auto-generated orderNumber when orderNumber is null")
        void should_createModule_withAutoGeneratedOrderNumber() {
                // Given
                ModuleRequest request = ModuleRequest.builder()
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(null)
                                .status("DRAFT")
                                .build();

                Module module = Module.builder()
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(null)
                                .status("DRAFT")
                                .build();

                Module existingModule1 = Module.builder()
                                .moduleId(1L)
                                .syllabusId(1L)
                                .orderNumber(1)
                                .build();

                Module existingModule2 = Module.builder()
                                .moduleId(2L)
                                .syllabusId(1L)
                                .orderNumber(2)
                                .build();

                List<Module> existingModules = List.of(existingModule1, existingModule2);

                Module savedModule = Module.builder()
                                .moduleId(3L)
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(3)
                                .status("DRAFT")
                                .build();

                ModuleResponse expectedResponse = ModuleResponse.builder()
                                .moduleId(3L)
                                .syllabusId(1L)
                                .title("New Module")
                                .orderNumber(3)
                                .status("DRAFT")
                                .build();

                when(moduleMapper.toEntity(request)).thenReturn(module);
                when(syllabusRepository.findById(1L)).thenReturn(Optional.of(new com.sep.educonnect.entity.Syllabus()));
                when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(1L)).thenReturn(existingModules);
                when(moduleRepository.save(any(Module.class))).thenReturn(savedModule);
                when(moduleMapper.toResponse(savedModule)).thenReturn(expectedResponse);

                // When
                ModuleResponse result = moduleService.create(request);

                // Then
                assertNotNull(result);
                assertEquals(3L, result.getModuleId());
                assertEquals(3, result.getOrderNumber());
                verify(moduleMapper).toEntity(request);
                verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAsc(1L);

                ArgumentCaptor<Module> moduleCaptor = ArgumentCaptor.forClass(Module.class);
                verify(moduleRepository).save(moduleCaptor.capture());
                Module capturedModule = moduleCaptor.getValue();
                assertEquals(3, capturedModule.getOrderNumber());

                verify(moduleMapper).toResponse(savedModule);
        }

        @Test
        @DisplayName("Should create module with orderNumber 1 when no existing modules")
        void should_createModule_withOrderNumber1_whenNoExistingModules() {
                // Given
                ModuleRequest request = ModuleRequest.builder()
                                .syllabusId(1L)
                                .title("First Module")
                                .orderNumber(null)
                                .build();

                Module module = Module.builder()
                                .syllabusId(1L)
                                .title("First Module")
                                .orderNumber(null)
                                .build();

                Module savedModule = Module.builder()
                                .moduleId(1L)
                                .syllabusId(1L)
                                .title("First Module")
                                .orderNumber(1)
                                .build();

                ModuleResponse expectedResponse = ModuleResponse.builder()
                                .moduleId(1L)
                                .syllabusId(1L)
                                .title("First Module")
                                .orderNumber(1)
                                .build();

                when(moduleMapper.toEntity(request)).thenReturn(module);
                when(syllabusRepository.findById(1L)).thenReturn(Optional.of(new com.sep.educonnect.entity.Syllabus()));
                when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(1L)).thenReturn(new ArrayList<>());
                when(moduleRepository.save(any(Module.class))).thenReturn(savedModule);
                when(moduleMapper.toResponse(savedModule)).thenReturn(expectedResponse);

                // When
                ModuleResponse result = moduleService.create(request);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getOrderNumber());

                ArgumentCaptor<Module> moduleCaptor = ArgumentCaptor.forClass(Module.class);
                verify(moduleRepository).save(moduleCaptor.capture());
                assertEquals(1, moduleCaptor.getValue().getOrderNumber());
        }

        @Test
        @DisplayName("Should throw AppException when syllabus not found for create")
        void should_throwException_when_syllabusNotFoundForCreate() {
            // Given
            ModuleRequest request = ModuleRequest.builder()
                    .syllabusId(999L)
                    .title("New Module")
                    .build();

            // Remove the unnecessary moduleMapper stubbing
            when(syllabusRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            AppException exception = assertThrows(AppException.class, () -> {
                moduleService.create(request);
            });

            assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
            verify(syllabusRepository).findById(999L);
            verify(moduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update module successfully")
        void should_updateModule_successfully() {
                // Given
                Long moduleId = 1L;
                ModuleRequest request = ModuleRequest.builder()
                                .syllabusId(1L)
                                .title("Updated Module")
                                .orderNumber(2)
                                .status("PUBLISHED")
                                .build();

                Module existingModule = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Original Module")
                                .orderNumber(1)
                                .status("DRAFT")
                                .build();

                Module updatedModule = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Updated Module")
                                .orderNumber(2)
                                .status("PUBLISHED")
                                .build();

                ModuleResponse expectedResponse = ModuleResponse.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Updated Module")
                                .orderNumber(2)
                                .status("PUBLISHED")
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(existingModule));
                doNothing().when(moduleMapper).updateEntity(existingModule, request);
                when(moduleRepository.save(existingModule)).thenReturn(updatedModule);
                when(moduleMapper.toResponse(updatedModule)).thenReturn(expectedResponse);

                // When
                ModuleResponse result = moduleService.update(moduleId, request);

                // Then
                assertNotNull(result);
                assertEquals(moduleId, result.getModuleId());
                assertEquals("Updated Module", result.getTitle());
                assertEquals(2, result.getOrderNumber());
                assertEquals("PUBLISHED", result.getStatus());
                verify(moduleRepository).findById(moduleId);
                verify(moduleMapper).updateEntity(existingModule, request);
                verify(moduleRepository).save(existingModule);
                verify(moduleMapper).toResponse(updatedModule);
        }

        @Test
        @DisplayName("Should throw AppException when module not found for update")
        void should_throwException_when_moduleNotFoundForUpdate() {
                // Given
                Long moduleId = 999L;
                ModuleRequest request = ModuleRequest.builder()
                                .title("Updated Module")
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> {
                        moduleService.update(moduleId, request);
                });

                assertEquals(ErrorCode.MODULE_NOT_EXISTED, exception.getErrorCode());
                verify(moduleRepository).findById(moduleId);
                verify(moduleMapper, never()).updateEntity(any(), any());
                verify(moduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw AppException when syllabus not found for update")
        void should_throwException_when_syllabusNotFoundForUpdate() {
                // Given
                Long moduleId = 1L;
                Long newSyllabusId = 999L;
                ModuleRequest request = ModuleRequest.builder()
                                .syllabusId(newSyllabusId)
                                .title("Updated Module")
                                .build();

                Module existingModule = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Original Module")
                                .orderNumber(1)
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(existingModule));
                when(syllabusRepository.findById(newSyllabusId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> {
                        moduleService.update(moduleId, request);
                });

                assertEquals(ErrorCode.SYLLABUS_NOT_EXISTED, exception.getErrorCode());
                verify(moduleRepository).findById(moduleId);
                verify(syllabusRepository).findById(newSyllabusId);
                verify(moduleMapper, never()).updateEntity(any(), any());
                verify(moduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should delete module successfully")
        void should_deleteModule_successfully() {
                // Given
                Long moduleId = 1L;
                Module module = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Module to Delete")
                                .orderNumber(1)
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
                doNothing().when(moduleRepository).delete(module);

                // When
                moduleService.delete(moduleId);

                // Then
                verify(moduleRepository).findById(moduleId);
                verify(moduleRepository).delete(module);
        }

        @Test
        @DisplayName("Should throw AppException when module not found for delete")
        void should_throwException_when_moduleNotFoundForDelete() {
                // Given
                Long moduleId = 999L;
                when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> {
                        moduleService.delete(moduleId);
                });

                assertEquals(ErrorCode.MODULE_NOT_EXISTED, exception.getErrorCode());
                verify(moduleRepository).findById(moduleId);
                verify(moduleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should reorder module successfully")
        void should_reorderModule_successfully() {
                // Given
                Long moduleId = 1L;
                Integer newOrderNumber = 5;
                Module module = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Module")
                                .orderNumber(1)
                                .build();

                Module reorderedModule = Module.builder()
                                .moduleId(moduleId)
                                .syllabusId(1L)
                                .title("Module")
                                .orderNumber(newOrderNumber)
                                .build();

                when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
                when(moduleRepository.save(module)).thenReturn(reorderedModule);

                // When
                moduleService.reorder(moduleId, newOrderNumber);

                // Then
                verify(moduleRepository).findById(moduleId);
                verify(moduleRepository).save(module);
                assertEquals(newOrderNumber, module.getOrderNumber());
        }

        @Test
        @DisplayName("Should throw AppException when module not found for reorder")
        void should_throwException_when_moduleNotFoundForReorder() {
                // Given
                Long moduleId = 999L;
                Integer newOrderNumber = 5;
                when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                // When & Then
                AppException exception = assertThrows(AppException.class, () -> {
                        moduleService.reorder(moduleId, newOrderNumber);
                });

                assertEquals(ErrorCode.MODULE_NOT_EXISTED, exception.getErrorCode());
                verify(moduleRepository).findById(moduleId);
                verify(moduleRepository, never()).save(any());
        }
}
