package com.sep.educonnect.service;

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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SyllabusService {

    SyllabusRepository syllabusRepository;
    SubjectRepository subjectRepository;
    SyllabusMapper syllabusMapper;
    LocalizationHelper localizationHelper;
    TranslationService translationService;

    public SyllabusResponse createSyllabus(SyllabusCreationRequest request, String language) {
        log.info("Creating new syllabus: {}", request.getName());

        if (subjectRepository.findByIdAndNotDeleted(request.getSubjectId()).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_EXISTED);
        }
        Syllabus syllabus = syllabusMapper.toSyllabus(request);

        Map<String, String> fieldsToTranslate = new HashMap<>();

        if (Language.VIETNAMESE.equals(language)) {
            if (request.getTarget() != null && !request.getTarget().isEmpty()) {
                syllabus.setTargetVi(request.getTarget());
                fieldsToTranslate.put("target", request.getTarget());
            }
            if (request.getLevel() != null && !request.getLevel().isEmpty()) {
                syllabus.setLevelVi(request.getLevel());
                fieldsToTranslate.put("level", request.getLevel());
            }
            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                syllabus.setDescriptionVi(request.getDescription());
                fieldsToTranslate.put("description", request.getDescription());
            }
        } else {
            if (request.getTarget() != null && !request.getTarget().isEmpty()) {
                syllabus.setTargetEn(request.getTarget());
                fieldsToTranslate.put("target", request.getTarget());
            }
            if (request.getLevel() != null && !request.getLevel().isEmpty()) {
                syllabus.setLevelEn(request.getLevel());
                fieldsToTranslate.put("level", request.getLevel());
            }
            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                syllabus.setDescriptionEn(request.getDescription());
                fieldsToTranslate.put("description", request.getDescription());
            }
        }

        try {
            syllabus = syllabusRepository.save(syllabus);
        } catch (DataIntegrityViolationException exception) {
            log.error("Data integrity violation when creating syllabus", exception);
            throw new AppException(ErrorCode.SYLLABUS_EXISTED);
        }

        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = Language.VIETNAMESE.equals(language) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture = translationService
                    .translateProfileFields(fieldsToTranslate, targetLanguage);

            Syllabus finalSyllabus = syllabus;
            translationFuture.thenAccept(result -> {
                if (result.isSuccess()) {

                    if (Language.VIETNAMESE.equals(language)) {
                        if (result.getTranslation("target") != null) {
                            finalSyllabus.setTargetEn(result.getTranslation("target"));
                        }
                        if (result.getTranslation("level") != null) {
                            finalSyllabus.setLevelEn(result.getTranslation("level"));
                        }
                        if (result.getTranslation("description") != null) {
                            finalSyllabus.setDescriptionEn(result.getTranslation("description"));
                        }
                    } else {
                        if (result.getTranslation("target") != null) {
                            finalSyllabus.setTargetVi(result.getTranslation("target"));
                        }
                        if (result.getTranslation("level") != null) {
                            finalSyllabus.setLevelVi(result.getTranslation("level"));
                        }
                        if (result.getTranslation("description") != null) {
                            finalSyllabus.setDescriptionVi(result.getTranslation("description"));
                        }
                    }

                    syllabusRepository.save(finalSyllabus);

                } else {
                    log.warn("Translation failed for syllabus ID: {}", finalSyllabus.getSyllabusId());
                }

            }).exceptionally(ex -> {
                log.error("Error during asynchronous translation for syllabus ID: {}", finalSyllabus.getSyllabusId(),
                        ex);
                return null;
            });
        }

        return syllabusMapper.toSyllabusResponse(syllabus, localizationHelper);
    }

    public Page<SyllabusResponse> getAllActiveSyllabus(
            int page,
            int size,
            String sortBy,
            String direction,
            String name,
            String level,
            String status) {
        log.info("Fetching all active syllabus with pagination");
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Syllabus> syllabusPage =
                syllabusRepository.searchActiveSyllabus(
                        normalize(name), normalize(level), normalize(status), pageable);
        return syllabusPage.map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper));
    }


    public SyllabusResponse getSyllabusById(Long syllabusId) {
        log.info("Fetching syllabus with ID: {}", syllabusId);
        Syllabus syllabus = syllabusRepository.findByIdAndNotDeleted(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));
        return syllabusMapper.toSyllabusResponse(syllabus, localizationHelper);
    }

    public SyllabusResponse getSyllabusByName(String name) {
        log.info("Fetching syllabus with name: {}", name);
        Syllabus syllabus = syllabusRepository.findByNameAndNotDeleted(name)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));
        return syllabusMapper.toSyllabusResponse(syllabus, localizationHelper);
    }

    public List<SyllabusResponse> getSyllabusBySubjectId(Long subjectId) {
        log.info("Fetching syllabus for subject ID: {}", subjectId);

        // Validate that subject exists
        if (subjectRepository.findByIdAndNotDeleted(subjectId).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_EXISTED);
        }

        return syllabusRepository.findBySubjectIdAndNotDeleted(subjectId)
                .stream()
                .map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper))
                .toList();
    }

    public List<SyllabusResponse> searchSyllabusByName(String name) {
        log.info("Searching syllabus containing name: {}", name);
        return syllabusRepository.findByNameContainingAndNotDeleted(name)
                .stream()
                .map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper))
                .toList();
    }

    public List<SyllabusResponse> getSyllabusByLevel(String level) {
        log.info("Fetching syllabus with level: {}", level);
        return syllabusRepository.findByLevelAndNotDeleted(level)
                .stream()
                .map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper))
                .toList();
    }

    public List<SyllabusResponse> getSyllabusByStatus(String status) {
        log.info("Fetching syllabus with status: {}", status);
        return syllabusRepository.findByStatusAndNotDeleted(status)
                .stream()
                .map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper))
                .toList();
    }

    public List<SyllabusResponse> getSyllabusBySubjectAndLevel(Long subjectId, String level) {
        log.info("Fetching syllabus for subject ID: {} and level: {}", subjectId, level);

        // Validate that subject exists
        if (subjectRepository.findByIdAndNotDeleted(subjectId).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_EXISTED);
        }

        return syllabusRepository.findBySubjectIdAndLevelAndNotDeleted(subjectId, level)
                .stream()
                .map(s -> syllabusMapper.toSyllabusResponse(s, localizationHelper))
                .toList();
    }

    public SyllabusResponse updateSyllabus(Long syllabusId, SyllabusUpdateRequest request, String language) {
        log.info("Updating syllabus with ID: {}", syllabusId);

        Syllabus existingSyllabus = syllabusRepository.findByIdAndNotDeleted(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));

        // Validate that subject exists if subject ID is being changed
        if (!existingSyllabus.getSubjectId().equals(request.getSubjectId()) &&
                subjectRepository.findByIdAndNotDeleted(request.getSubjectId()).isEmpty()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_EXISTED);
        }

        Map<String, String> fieldsToTranslate = new HashMap<>();

        // Set values based on language and prepare translation
        if (Language.VIETNAMESE.equals(language)) {
            if (request.getTarget() != null && !request.getTarget().isEmpty()) {
                existingSyllabus.setTargetVi(request.getTarget());
                fieldsToTranslate.put("target", request.getTarget());
            }
            if (request.getLevel() != null && !request.getLevel().isEmpty()) {
                existingSyllabus.setLevelVi(request.getLevel());
                fieldsToTranslate.put("level", request.getLevel());
            }
            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                existingSyllabus.setDescriptionVi(request.getDescription());
                fieldsToTranslate.put("description", request.getDescription());
            }
        } else {
            if (request.getTarget() != null && !request.getTarget().isEmpty()) {
                existingSyllabus.setTargetEn(request.getTarget());
                fieldsToTranslate.put("target", request.getTarget());
            }
            if (request.getLevel() != null && !request.getLevel().isEmpty()) {
                existingSyllabus.setLevelEn(request.getLevel());
                fieldsToTranslate.put("level", request.getLevel());
            }
            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                existingSyllabus.setDescriptionEn(request.getDescription());
                fieldsToTranslate.put("description", request.getDescription());
            }
        }

        // Update other fields (name, subjectId, status, etc.)
        syllabusMapper.updateSyllabus(existingSyllabus, request);

        try {
            existingSyllabus = syllabusRepository.save(existingSyllabus);
        } catch (DataIntegrityViolationException exception) {
            log.error("Data integrity violation when updating syllabus", exception);
            throw new AppException(ErrorCode.SYLLABUS_EXISTED);
        }

        // Asynchronous translation if needed
        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = Language.VIETNAMESE.equals(language) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture = translationService
                    .translateProfileFields(fieldsToTranslate, targetLanguage);

            Syllabus finalSyllabus = existingSyllabus;
            translationFuture.thenAccept(result -> {
                if (result.isSuccess()) {

                    if (Language.VIETNAMESE.equals(language)) {
                        if (result.getTranslation("target") != null) {
                            finalSyllabus.setTargetEn(result.getTranslation("target"));
                        }
                        if (result.getTranslation("level") != null) {
                            finalSyllabus.setLevelEn(result.getTranslation("level"));
                        }
                        if (result.getTranslation("description") != null) {
                            finalSyllabus.setDescriptionEn(result.getTranslation("description"));
                        }
                    } else {
                        if (result.getTranslation("target") != null) {
                            finalSyllabus.setTargetVi(result.getTranslation("target"));
                        }
                        if (result.getTranslation("level") != null) {
                            finalSyllabus.setLevelVi(result.getTranslation("level"));
                        }
                        if (result.getTranslation("description") != null) {
                            finalSyllabus.setDescriptionVi(result.getTranslation("description"));
                        }
                    }

                    syllabusRepository.save(finalSyllabus);

                } else {
                    log.warn("Translation failed for syllabus ID: {}", finalSyllabus.getSyllabusId());
                }
            }).exceptionally(ex -> {
                log.error("Error during asynchronous translation for syllabus ID: {}", finalSyllabus.getSyllabusId(),
                        ex);
                return null;
            });
        }

        return syllabusMapper.toSyllabusResponse(existingSyllabus, localizationHelper);
    }

    public void deleteSyllabus(Long syllabusId) {
        log.info("Soft deleting syllabus with ID: {}", syllabusId);

        Syllabus syllabus = syllabusRepository.findByIdAndNotDeleted(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_EXISTED));

        syllabus.setIsDeleted(true);
        syllabusRepository.save(syllabus);
    }

    public boolean existsByName(String name) {
        return syllabusRepository.existsByNameAndNotDeleted(name);
    }

    public long countSyllabusBySubject(Long subjectId) {
        log.info("Counting syllabus for subject ID: {}", subjectId);
        return syllabusRepository.countBySubjectIdAndNotDeleted(subjectId);
    }

    public Page<SubjectSyllabusResponse> getAllSubjectsWithSyllabuses(int page, int size, String sortBy, String direction) {
        log.info("Fetching all subjects with their syllabuses with pagination");
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Subject> subjectPage = subjectRepository.findAllActiveSubjects(pageable);

        List<Long> subjectIds = subjectPage.getContent().stream()
                .map(Subject::getSubjectId)
                .toList();

        Map<Long, List<Syllabus>> syllabusesBySubject = syllabusRepository.findBySubjectIdsAndNotDeleted(subjectIds)
                .stream()
                .collect(Collectors.groupingBy(Syllabus::getSubjectId));

        return subjectPage.map(subject -> {
            List<Syllabus> syllabuses = syllabusesBySubject.getOrDefault(subject.getSubjectId(), Collections.emptyList());
            return syllabusMapper.toSubjectSyllabusResponse(subject, syllabuses, localizationHelper);
        });
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

}
