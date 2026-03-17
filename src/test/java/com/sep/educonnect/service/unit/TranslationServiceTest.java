package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sep.educonnect.service.AiService;
import com.sep.educonnect.service.TranslationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationService Unit Tests")
class TranslationServiceTest {

    @Mock private AiService aiService;

    @InjectMocks private TranslationService translationService;

    @Test
    @DisplayName("Should return successful translation result")
    void should_returnSuccessfulTranslation() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào");
        Map<String, String> translated = Map.of("bio", "Hello");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        CompletableFuture<TranslationService.TranslationResult> future =
                translationService.translateProfileFields(fields, "English");
        TranslationService.TranslationResult result = future.join();

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Hello", result.getTranslation("bio"));
        verify(aiService).translateFields(fields, "English");
    }

    @Test
    @DisplayName("Should return failed result when AI throws")
    void should_returnFailedResultWhenException() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào");
        when(aiService.translateFields(anyMap(), eq("English")))
                .thenThrow(new RuntimeException("API error"));

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getTranslations().isEmpty());
    }

    // ==================== Additional tests for translateProfileFields ====================

    @Test
    @DisplayName("Should handle empty fields map")
    void should_handleEmptyFieldsMap() {
        // Given
        Map<String, String> fields = Map.of();
        Map<String, String> translated = Map.of();

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getTranslations().isEmpty());
        verify(aiService).translateFields(fields, "English");
    }

    @Test
    @DisplayName("Should translate multiple fields successfully")
    void should_translateMultipleFields() {
        // Given
        Map<String, String> fields =
                Map.of(
                        "bio", "Xin chào",
                        "headline", "Giáo viên toán",
                        "experience", "5 năm kinh nghiệm");
        Map<String, String> translated =
                Map.of(
                        "bio", "Hello",
                        "headline", "Math teacher",
                        "experience", "5 years experience");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTranslations().size());
        assertEquals("Hello", result.getTranslation("bio"));
        assertEquals("Math teacher", result.getTranslation("headline"));
        assertEquals("5 years experience", result.getTranslation("experience"));
    }

    @Test
    @DisplayName("Should translate to Vietnamese language")
    void should_translateToVietnamese() {
        // Given
        Map<String, String> fields = Map.of("bio", "Hello world");
        Map<String, String> translated = Map.of("bio", "Xin chào thế giới");

        when(aiService.translateFields(anyMap(), eq("Vietnamese"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "Vietnamese").join();

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Xin chào thế giới", result.getTranslation("bio"));
        verify(aiService).translateFields(fields, "Vietnamese");
    }

    @Test
    @DisplayName("Should translate to different target languages")
    void should_translateToDifferentLanguages() {
        // Given
        Map<String, String> fields = Map.of("bio", "Hello");

        when(aiService.translateFields(anyMap(), eq("Spanish"))).thenReturn(Map.of("bio", "Hola"));
        when(aiService.translateFields(anyMap(), eq("French")))
                .thenReturn(Map.of("bio", "Bonjour"));
        when(aiService.translateFields(anyMap(), eq("German")))
                .thenReturn(Map.of("bio", "Guten Tag"));

        // When & Then - Spanish
        TranslationService.TranslationResult resultSpanish =
                translationService.translateProfileFields(fields, "Spanish").join();
        assertTrue(resultSpanish.isSuccess());
        assertEquals("Hola", resultSpanish.getTranslation("bio"));

        // When & Then - French
        TranslationService.TranslationResult resultFrench =
                translationService.translateProfileFields(fields, "French").join();
        assertTrue(resultFrench.isSuccess());
        assertEquals("Bonjour", resultFrench.getTranslation("bio"));

        // When & Then - German
        TranslationService.TranslationResult resultGerman =
                translationService.translateProfileFields(fields, "German").join();
        assertTrue(resultGerman.isSuccess());
        assertEquals("Guten Tag", resultGerman.getTranslation("bio"));
    }

    @Test
    @DisplayName("Should return null for non-existent field in translation result")
    void should_returnNullForNonExistentField() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào");
        Map<String, String> translated = Map.of("bio", "Hello");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertTrue(result.isSuccess());
        assertNull(result.getTranslation("nonExistentField"));
    }

    @Test
    @DisplayName("Should handle RuntimeException from AI service")
    void should_handleRuntimeException() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào");
        when(aiService.translateFields(anyMap(), eq("English")))
                .thenThrow(new RuntimeException("Connection timeout"));

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getTranslations().isEmpty());
        assertNull(result.getTranslation("bio"));
    }

    @Test
    @DisplayName("Should handle generic Exception from AI service")
    void should_handleGenericException() {
        // Given
        Map<String, String> fields = Map.of("bio", "Test");
        when(aiService.translateFields(anyMap(), eq("English")))
                .thenThrow(new IllegalArgumentException("Invalid language"));

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getTranslations().isEmpty());
    }

    @Test
    @DisplayName("Should handle NullPointerException from AI service")
    void should_handleNullPointerException() {
        // Given
        Map<String, String> fields = Map.of("bio", "Test");
        when(aiService.translateFields(anyMap(), eq("English")))
                .thenThrow(new NullPointerException("Null response"));

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getTranslations().isEmpty());
    }

    @Test
    @DisplayName("Should translate large number of fields")
    void should_translateLargeNumberOfFields() {
        // Given
        Map<String, String> fields =
                Map.of(
                        "field1", "value1",
                        "field2", "value2",
                        "field3", "value3",
                        "field4", "value4",
                        "field5", "value5",
                        "field6", "value6",
                        "field7", "value7",
                        "field8", "value8",
                        "field9", "value9",
                        "field10", "value10");
        Map<String, String> translated =
                Map.of(
                        "field1", "translated1",
                        "field2", "translated2",
                        "field3", "translated3",
                        "field4", "translated4",
                        "field5", "translated5",
                        "field6", "translated6",
                        "field7", "translated7",
                        "field8", "translated8",
                        "field9", "translated9",
                        "field10", "translated10");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(10, result.getTranslations().size());
        assertEquals("translated5", result.getTranslation("field5"));
    }

    @Test
    @DisplayName("Should return CompletableFuture that completes successfully")
    void should_returnCompletableFutureThatCompletes() {
        // Given
        Map<String, String> fields = Map.of("bio", "Test");
        Map<String, String> translated = Map.of("bio", "Translated");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        CompletableFuture<TranslationService.TranslationResult> future =
                translationService.translateProfileFields(fields, "English");

        // Then
        assertNotNull(future);
        assertFalse(future.isCompletedExceptionally());
        TranslationService.TranslationResult result = future.join();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should return CompletableFuture that completes on exception")
    void should_returnCompletableFutureThatCompletesOnException() {
        // Given
        Map<String, String> fields = Map.of("bio", "Test");
        when(aiService.translateFields(anyMap(), eq("English")))
                .thenThrow(new RuntimeException("Error"));

        // When
        CompletableFuture<TranslationService.TranslationResult> future =
                translationService.translateProfileFields(fields, "English");

        // Then
        assertNotNull(future);
        assertFalse(future.isCompletedExceptionally()); // Exception is caught internally
        TranslationService.TranslationResult result = future.join();
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should verify aiService is called with correct parameters")
    void should_verifyAiServiceCalledWithCorrectParameters() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào", "headline", "Giáo viên");
        Map<String, String> translated = Map.of("bio", "Hello", "headline", "Teacher");

        when(aiService.translateFields(fields, "English")).thenReturn(translated);

        // When
        translationService.translateProfileFields(fields, "English").join();

        // Then
        verify(aiService, times(1)).translateFields(fields, "English");
        verify(aiService, times(1)).translateFields(anyMap(), eq("English"));
    }

    @Test
    @DisplayName("Should handle single field translation")
    void should_handleSingleFieldTranslation() {
        // Given
        Map<String, String> fields = Map.of("bio", "Xin chào");
        Map<String, String> translated = Map.of("bio", "Hello");

        when(aiService.translateFields(anyMap(), eq("English"))).thenReturn(translated);

        // When
        TranslationService.TranslationResult result =
                translationService.translateProfileFields(fields, "English").join();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTranslations().size());
        assertEquals("Hello", result.getTranslation("bio"));
    }

    // ==================== TranslationResult Tests ====================

    @Test
    @DisplayName("Should create TranslationResult with default values")
    void should_createTranslationResultWithDefaults() {
        // Given & When
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();

        // Then
        assertNotNull(result);
        assertNotNull(result.getTranslations());
        assertTrue(result.getTranslations().isEmpty());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should set and get translations in TranslationResult")
    void should_setAndGetTranslations() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        Map<String, String> translations = Map.of("bio", "Hello", "headline", "Teacher");

        // When
        result.setTranslations(translations);

        // Then
        assertEquals(translations, result.getTranslations());
        assertEquals("Hello", result.getTranslation("bio"));
        assertEquals("Teacher", result.getTranslation("headline"));
    }

    @Test
    @DisplayName("Should set and get success flag in TranslationResult")
    void should_setAndGetSuccessFlag() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();

        // When & Then - Set to true
        result.setSuccess(true);
        assertTrue(result.isSuccess());

        // When & Then - Set to false
        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle null key in getTranslation")
    void should_handleNullKeyInGetTranslation() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        // Use HashMap instead of Map.of() to support null keys
        Map<String, String> translations = new HashMap<>();
        translations.put("bio", "Hello");
        result.setTranslations(translations);

        // When
        String translation = result.getTranslation(null);

        // Then
        assertNull(translation);
    }

    @Test
    @DisplayName("Should handle empty string key in getTranslation")
    void should_handleEmptyStringKeyInGetTranslation() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of("bio", "Hello", "", "Empty key value"));

        // When
        String translation = result.getTranslation("");

        // Then
        assertEquals("Empty key value", translation);
    }

    @Test
    @DisplayName("Should test equals and hashCode of TranslationResult")
    void should_testEqualsAndHashCode() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(Map.of("bio", "Hello"));
        result1.setSuccess(true);

        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of("bio", "Hello"));
        result2.setSuccess(true);

        TranslationService.TranslationResult result3 = new TranslationService.TranslationResult();
        result3.setTranslations(Map.of("bio", "Different"));
        result3.setSuccess(false);

        // Then
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1.hashCode(), result2.hashCode());
        assertNotEquals(result1.hashCode(), result3.hashCode());

        // Test reflexive property
        assertEquals(result1, result1);

        // Test with null
        assertNotEquals(null, result1);

        // Test with different class
        assertNotEquals("string", result1);
    }

    @Test
    @DisplayName("Should test toString of TranslationResult")
    void should_testToString() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of("bio", "Hello"));
        result.setSuccess(true);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("TranslationResult"));
        assertTrue(toString.contains("success=true") || toString.contains("success = true"));
    }

    @Test
    @DisplayName("Should handle mutable translations map")
    void should_handleMutableTranslationsMap() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        Map<String, String> translations = new java.util.HashMap<>();
        translations.put("bio", "Hello");
        result.setTranslations(translations);

        // When - Modify the map after setting
        translations.put("headline", "Teacher");

        // Then - The result should reflect the change (same reference)
        assertEquals(2, result.getTranslations().size());
        assertEquals("Teacher", result.getTranslation("headline"));
    }

    @Test
    @DisplayName("Should handle setting empty translations map")
    void should_handleSettingEmptyTranslationsMap() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();

        // When
        result.setTranslations(Map.of());

        // Then
        assertNotNull(result.getTranslations());
        assertTrue(result.getTranslations().isEmpty());
        assertNull(result.getTranslation("anyKey"));
    }

    @Test
    @DisplayName("Should handle canEqual method for TranslationResult")
    void should_handleCanEqual() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();

        // Then - Both should be equal when empty
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("Should handle multiple consecutive getTranslation calls")
    void should_handleMultipleGetTranslationCalls() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of("bio", "Hello"));

        // When & Then - Call multiple times
        assertEquals("Hello", result.getTranslation("bio"));
        assertEquals("Hello", result.getTranslation("bio"));
        assertEquals("Hello", result.getTranslation("bio"));
        assertNull(result.getTranslation("nonexistent"));
        assertNull(result.getTranslation("nonexistent"));
    }

    @Test
    @DisplayName("Should handle TranslationResult with large translations map")
    void should_handleLargeTranslationsMap() {
        // Given
        Map<String, String> largeMap = new java.util.HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeMap.put("field" + i, "value" + i);
        }
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();

        // When
        result.setTranslations(largeMap);

        // Then
        assertEquals(100, result.getTranslations().size());
        assertEquals("value50", result.getTranslation("field50"));
        assertNull(result.getTranslation("field100"));
    }

    @Test
    @DisplayName("Should handle special characters in translation keys")
    void should_handleSpecialCharactersInKeys() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        Map<String, String> translations =
                Map.of(
                        "key@with#special$chars", "value1",
                        "key with spaces", "value2",
                        "key\twith\ttabs", "value3");
        result.setTranslations(translations);

        // When & Then
        assertEquals("value1", result.getTranslation("key@with#special$chars"));
        assertEquals("value2", result.getTranslation("key with spaces"));
        assertEquals("value3", result.getTranslation("key\twith\ttabs"));
    }

    @Test
    @DisplayName("Should handle null translations map in equals")
    void should_handleNullTranslationsInEquals() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(null);
        result1.setSuccess(true);

        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of("bio", "Hello"));
        result2.setSuccess(true);

        // Then
        assertNotEquals(result1, result2);
        assertNotEquals(result2, result1);
    }

    @Test
    @DisplayName("Should handle symmetric property of equals")
    void should_handleSymmetricPropertyOfEquals() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(Map.of("bio", "Hello"));
        result1.setSuccess(true);

        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of("bio", "Hello"));
        result2.setSuccess(true);

        // Then - Symmetric: x.equals(y) iff y.equals(x)
        assertEquals(result1, result2);
        assertEquals(result2, result1);
    }

    @Test
    @DisplayName("Should handle transitive property of equals")
    void should_handleTransitivePropertyOfEquals() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(Map.of("bio", "Hello"));
        result1.setSuccess(true);

        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of("bio", "Hello"));
        result2.setSuccess(true);

        TranslationService.TranslationResult result3 = new TranslationService.TranslationResult();
        result3.setTranslations(Map.of("bio", "Hello"));
        result3.setSuccess(true);

        // Then - Transitive: if x.equals(y) and y.equals(z), then x.equals(z)
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals(result1, result3);
    }

    @Test
    @DisplayName("Should test hashCode consistency")
    void should_testHashCodeConsistency() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of("bio", "Hello"));
        result.setSuccess(true);

        // When - Call hashCode multiple times
        int hash1 = result.hashCode();
        int hash2 = result.hashCode();
        int hash3 = result.hashCode();

        // Then - hashCode should be consistent
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    @DisplayName("Should test hashCode with null translations")
    void should_testHashCodeWithNullTranslations() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(null);
        result.setSuccess(true);

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> result.hashCode());

        // Two objects with null translations should have same hashCode
        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(null);
        result2.setSuccess(true);

        assertEquals(result.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("Should test toString with null translations")
    void should_testToStringWithNullTranslations() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(null);
        result.setSuccess(true);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("TranslationResult"));
        assertTrue(toString.contains("null") || toString.contains("translations"));
    }

    @Test
    @DisplayName("Should test toString with false success")
    void should_testToStringWithFalseSuccess() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of());
        result.setSuccess(false);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("success=false") || toString.contains("success = false"));
    }

    @Test
    @DisplayName("Should test getTranslation with null translations map")
    void should_testGetTranslationWithNullTranslationsMap() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(null);

        // When & Then - Should handle null gracefully
        assertThrows(NullPointerException.class, () -> result.getTranslation("bio"));
    }

    @Test
    @DisplayName("Should verify all fields are covered in equals")
    void should_verifyAllFieldsInEquals() {
        // Given - Two results identical in translations but different in success
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(Map.of("bio", "Hello"));
        result1.setSuccess(true);

        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of("bio", "Hello"));
        result2.setSuccess(false);

        // Then
        assertNotEquals(result1, result2);

        // Given - Two results identical in success but different in translations
        TranslationService.TranslationResult result3 = new TranslationService.TranslationResult();
        result3.setTranslations(Map.of("headline", "Teacher"));
        result3.setSuccess(true);

        // Then
        assertNotEquals(result1, result3);
    }

    @Test
    @DisplayName("Should handle empty translations map in all methods")
    void should_handleEmptyTranslationsInAllMethods() {
        // Given
        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(Map.of());
        result.setSuccess(true);

        // When & Then - Test all methods
        assertEquals(Map.of(), result.getTranslations());
        assertTrue(result.isSuccess());
        assertNull(result.getTranslation("anyKey"));
        assertNotNull(result.toString());
        assertNotNull(result.hashCode());

        // Test equals with another empty result
        TranslationService.TranslationResult result2 = new TranslationService.TranslationResult();
        result2.setTranslations(Map.of());
        result2.setSuccess(true);
        assertEquals(result, result2);
    }

    @Test
    @DisplayName("Should handle complex equals scenarios")
    void should_handleComplexEqualsScenarios() {
        // Given
        TranslationService.TranslationResult baseResult =
                new TranslationService.TranslationResult();
        baseResult.setTranslations(Map.of("bio", "Hello"));
        baseResult.setSuccess(true);

        // Test with Object type variable
        Object objResult = baseResult;
        assertTrue(baseResult.equals(objResult));

        // Test with different object type
        Object differentType = new Object();
        assertFalse(baseResult.equals(differentType));

        // Test with subclass (if possible)
        assertFalse(baseResult.equals(new Object()));
    }

    @Test
    @DisplayName("Should handle canEqual with different types")
    void should_handleCanEqualWithDifferentTypes() {
        // Given
        TranslationService.TranslationResult result1 = new TranslationService.TranslationResult();
        result1.setTranslations(Map.of("bio", "Hello"));
        result1.setSuccess(true);

        // Create an object of different type
        String differentType = "string";

        // Then - Should not be equal to different type
        assertFalse(result1.equals(differentType));
    }

    @Test
    @DisplayName("Should handle modifications after setting translations")
    void should_handleModificationsAfterSettingTranslations() {
        // Given
        Map<String, String> originalMap = new java.util.HashMap<>();
        originalMap.put("bio", "Hello");

        TranslationService.TranslationResult result = new TranslationService.TranslationResult();
        result.setTranslations(originalMap);

        // When - Modify the original map after setting
        originalMap.put("headline", "Teacher");
        originalMap.put("experience", "5 years");

        // Then - Result should reflect the changes
        assertEquals(3, result.getTranslations().size());
        assertEquals("Teacher", result.getTranslation("headline"));
    }
}
