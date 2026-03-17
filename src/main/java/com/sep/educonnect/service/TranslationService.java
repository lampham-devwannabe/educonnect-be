package com.sep.educonnect.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {
    private final AiService aiService;

    @Async("translationExecutor")

    public CompletableFuture<TranslationResult> translateProfileFields(
            Map<String, String> fieldsToTranslate, String targetLanguage) {

        try {
            log.info("Starting translation for {} fields to {}", fieldsToTranslate.size(), targetLanguage);

            // Call AiService to translate the fields
            Map<String, String> translatedFields = aiService.translateFields(fieldsToTranslate, targetLanguage);

            TranslationResult result = new TranslationResult();
            result.setTranslations(translatedFields);
            result.setSuccess(true);

            log.info("Translation completed successfully for {} fields", translatedFields.size());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Translation failed for fields: {}", fieldsToTranslate.keySet(), e);

            TranslationResult result = new TranslationResult();
            result.setSuccess(false);
            result.setTranslations(new HashMap<>());

            return CompletableFuture.completedFuture(result);
        }
    }

    @Data
    public static class TranslationResult {
        private Map<String, String> translations = new HashMap<>();
        private boolean success;

        public String getTranslation(String fieldName) {
            return translations.get(fieldName);
        }
    }
}
