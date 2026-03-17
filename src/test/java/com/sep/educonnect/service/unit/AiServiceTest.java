package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.sep.educonnect.service.AiService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient openAIClient;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        // Create service manually and inject mock (avoid @PostConstruct)
        aiService = new AiService();
        ReflectionTestUtils.setField(aiService, "openAIClient", openAIClient);
    }

    @Test
    @DisplayName("Should translate fields successfully")
    void should_translateFields_successfully() {
        // Given
        Map<String, String> fields = new HashMap<>();
        fields.put("title", "Hello");
        fields.put("description", "World");
        String targetLanguage = "Vietnamese";

        String jsonResponse = "{\"title\": \"Xin chào\", \"description\": \"Thế giới\"}";

        // Mock OpenAI chain using deep stubs
        when(openAIClient
                        .chat()
                        .completions()
                        .create(any(ChatCompletionCreateParams.class))
                        .choices()
                        .get(0)
                        .message()
                        .content())
                .thenReturn(Optional.of(jsonResponse));

        // When
        Map<String, String> result = aiService.translateFields(fields, targetLanguage);

        // Then
        assertNotNull(result);
        assertEquals("Xin chào", result.get("title"));
        assertEquals("Thế giới", result.get("description"));
    }

    @Test
    @DisplayName("Should handle empty string values in input fields")
    void should_handleEmptyStringValues_inInputFields() {
        // Given
        Map<String, String> fields = new HashMap<>();
        fields.put("title", ""); // Empty string
        fields.put("description", "World");
        String targetLanguage = "German";

        // Empty strings are filtered out by isBlank() check, so response won't have it
        String jsonResponse = "{\"description\": \"Welt\"}";

        when(openAIClient
                        .chat()
                        .completions()
                        .create(any(ChatCompletionCreateParams.class))
                        .choices()
                        .get(0)
                        .message()
                        .content())
                .thenReturn(Optional.of(jsonResponse));

        // When
        Map<String, String> result = aiService.translateFields(fields, targetLanguage);

        // Then
        assertNotNull(result);
        assertNull(result.get("title")); // Empty string filtered out, not in response
        assertEquals("Welt", result.get("description"));
    }

    @Test
    @DisplayName("Should handle different target languages")
    void should_handleDifferentTargetLanguages() {
        // Given
        Map<String, String> fields = Map.of("greeting", "Hello");
        String jsonResponse = "{\"greeting\": \"Привет\"}";

        when(openAIClient
                        .chat()
                        .completions()
                        .create(any(ChatCompletionCreateParams.class))
                        .choices()
                        .get(0)
                        .message()
                        .content())
                .thenReturn(Optional.of(jsonResponse));

        // When
        Map<String, String> result = aiService.translateFields(fields, "Russian");

        // Then
        assertNotNull(result);
        assertEquals("Привет", result.get("greeting"));
    }

    @Test
    @DisplayName("Should handle single field translation")
    void should_handleSingleField_translation() {
        // Given
        Map<String, String> fields = Map.of("message", "Good morning");
        String targetLanguage = "Italian";
        String jsonResponse = "{\"message\": \"Buongiorno\"}";

        when(openAIClient
                        .chat()
                        .completions()
                        .create(any(ChatCompletionCreateParams.class))
                        .choices()
                        .get(0)
                        .message()
                        .content())
                .thenReturn(Optional.of(jsonResponse));

        // When
        Map<String, String> result = aiService.translateFields(fields, targetLanguage);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Buongiorno", result.get("message"));
    }
}
