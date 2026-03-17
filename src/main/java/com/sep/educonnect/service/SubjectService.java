package com.sep.educonnect.service;

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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubjectService {

    SubjectRepository subjectRepository;
    SubjectMapper subjectMapper;
    LocalizationHelper localizationHelper;
    TranslationService translationService;

    public SubjectResponse createSubject(SubjectCreationRequest request, String language) {
        log.info("Creating new subject: {}", request.getSubjectName());

        Subject subject = subjectMapper.toSubject(request);
        Map<String, String> fieldsToTranslate = new HashMap<>();

        if (Language.VIETNAMESE.equals(language)) {
            subject.setNameVi(request.getSubjectName());
            subject.setNameEn(
                    request.getSubjectName()); // Temporary value, will be replaced by translation
            fieldsToTranslate.put("name", request.getSubjectName());
        } else {
            subject.setNameEn(request.getSubjectName());
            subject.setNameVi(
                    request.getSubjectName()); // Temporary value, will be replaced by translation
            fieldsToTranslate.put("name", request.getSubjectName());
        }

        try {
            subject = subjectRepository.save(subject);
        } catch (DataIntegrityViolationException exception) {
            log.error("Data integrity violation when creating subject", exception);
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = Language.VIETNAMESE.equals(language) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture =
                    translationService.translateProfileFields(fieldsToTranslate, targetLanguage);

            Subject finalSubject = subject;
            translationFuture
                    .thenAccept(
                            result -> {
                                if (result.isSuccess()) {
                                    String translatedName = result.getTranslation("name");
                                    if (translatedName != null && !translatedName.isBlank()) {
                                        if (Language.VIETNAMESE.equals(language)) {
                                            finalSubject.setNameEn(translatedName);
                                        } else {
                                            finalSubject.setNameVi(translatedName);
                                        }
                                        subjectRepository.save(finalSubject);
                                    }
                                } else {
                                    log.warn(
                                            "Translation failed for subject ID: {}",
                                            finalSubject.getSubjectId());
                                }
                            })
                    .exceptionally(
                            ex -> {
                                log.error(
                                        "Error during asynchronous translation for subject ID: {}",
                                        finalSubject.getSubjectId(),
                                        ex);
                                return null;
                            });
        }

        return subjectMapper.toSubjectResponse(subject, localizationHelper);
    }

    public Page<SubjectResponse> getAllActiveSubjects(
            int page, int size, String sortBy, String direction, String name) {
        log.info("Fetching all active subjects with pagination");

        // Map "name" to "nameVi" or "nameEn" based on current language, default to "nameVi"
        String actualSortBy = sortBy;
        if ("name".equalsIgnoreCase(sortBy)) {
            Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
            boolean isEnglish = "en".equalsIgnoreCase(locale.getLanguage());
            actualSortBy = isEnglish ? "nameEn" : "nameVi";
        }

        // Validate sortBy field to prevent errors
        List<String> validSortFields = List.of("subjectId", "nameVi", "nameEn", "createdAt", "modifiedAt");
        if (!validSortFields.contains(actualSortBy)) {
            log.warn("Invalid sort field: {}, using default: subjectId", actualSortBy);
            actualSortBy = "subjectId";
        }

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, actualSortBy));
        Page<Subject> subjectPage =
                subjectRepository.searchActiveSubjects(normalize(name), pageable);
        return subjectPage.map(s -> subjectMapper.toSubjectResponse(s, localizationHelper));
    }

    public SubjectResponse getSubjectById(Long subjectId) {
        log.info("Fetching subject with ID: {}", subjectId);
        Subject subject =
                subjectRepository
                        .findByIdAndNotDeleted(subjectId)
                        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));
        return subjectMapper.toSubjectResponse(subject, localizationHelper);
    }

    public SubjectResponse getSubjectByName(String subjectName) {
        log.info("Fetching subject with name: {}", subjectName);
        Subject subject =
                subjectRepository
                        .findBySubjectNameAndNotDeleted(subjectName)
                        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));
        return subjectMapper.toSubjectResponse(subject, localizationHelper);
    }

    public List<SubjectResponse> searchSubjectsByName(String subjectName) {
        log.info("Searching subjects containing name: {}", subjectName);
        return subjectRepository.findBySubjectNameContainingAndNotDeleted(subjectName).stream()
                .map(s -> subjectMapper.toSubjectResponse(s, localizationHelper))
                .toList();
    }

    public SubjectResponse updateSubject(
            Long subjectId, SubjectUpdateRequest request, String language) {
        log.info("Updating subject with ID: {}", subjectId);

        Subject existingSubject =
                subjectRepository
                        .findByIdAndNotDeleted(subjectId)
                        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));

        if (subjectRepository.existsBySubjectNameAndNotDeleted(request.getSubjectName())) {
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        Map<String, String> fieldsToTranslate = new HashMap<>();

        // Set values based on language and prepare translation
        if (Language.VIETNAMESE.equals(language)) {
            if (request.getSubjectName() != null && !request.getSubjectName().isEmpty()) {
                existingSubject.setNameVi(request.getSubjectName());
                fieldsToTranslate.put("name", request.getSubjectName());
            }
        } else {
            if (request.getSubjectName() != null && !request.getSubjectName().isEmpty()) {
                existingSubject.setNameEn(request.getSubjectName());
                fieldsToTranslate.put("name", request.getSubjectName());
            }
        }

        // Update other fields
        subjectMapper.updateSubject(existingSubject, request);

        try {
            existingSubject = subjectRepository.save(existingSubject);
        } catch (DataIntegrityViolationException exception) {
            log.error("Data integrity violation when updating subject", exception);
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        // Asynchronous translation if needed
        if (!fieldsToTranslate.isEmpty()) {
            String targetLanguage = Language.VIETNAMESE.equals(language) ? "English" : "Vietnamese";
            CompletableFuture<TranslationService.TranslationResult> translationFuture =
                    translationService.translateProfileFields(fieldsToTranslate, targetLanguage);

            Subject finalSubject = existingSubject;
            translationFuture
                    .thenAccept(
                            result -> {
                                if (result.isSuccess()) {
                                    String translatedName = result.getTranslation("name");
                                    if (translatedName != null && !translatedName.isBlank()) {
                                        if (Language.VIETNAMESE.equals(language)) {
                                            finalSubject.setNameEn(translatedName);
                                        } else {
                                            finalSubject.setNameVi(translatedName);
                                        }
                                        subjectRepository.save(finalSubject);
                                    }
                                } else {
                                    log.warn(
                                            "Translation failed for subject ID: {}",
                                            finalSubject.getSubjectId());
                                }
                            })
                    .exceptionally(
                            ex -> {
                                log.error(
                                        "Error during asynchronous translation for subject ID: {}",
                                        finalSubject.getSubjectId(),
                                        ex);
                                return null;
                            });
        }

        return subjectMapper.toSubjectResponse(existingSubject, localizationHelper);
    }

    public void deleteSubject(Long subjectId) {
        log.info("Soft deleting subject with ID: {}", subjectId);

        Subject subject =
                subjectRepository
                        .findByIdAndNotDeleted(subjectId)
                        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_EXISTED));

        subject.setIsDeleted(true);
        subjectRepository.save(subject);
    }

    public boolean existsByName(String subjectName) {
        return subjectRepository.existsBySubjectNameAndNotDeleted(subjectName);
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
