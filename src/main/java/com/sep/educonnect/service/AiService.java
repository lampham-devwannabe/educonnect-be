package com.sep.educonnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${openai.api-key}")
    String OPENAI_KEY;
    OpenAIClient openAIClient;

    @PostConstruct
    public void init() {
        this.openAIClient = new OpenAIOkHttpClient.Builder()
                .apiKey(OPENAI_KEY)
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public Map<String, String> translateFields(Map<String, String> fieldsToTranslate, String targetLanguage) {
        String prompt = buildTranslateFieldsPrompt(fieldsToTranslate, targetLanguage);

        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4_1)  // small, cost-efficient model
                .addSystemMessage("You are a translation assistant. Respond strictly in JSON.")
                .addUserMessage(prompt)
                .temperature(0.2)
                .build();

        var response = openAIClient.chat().completions().create(request);
        String content = response.choices().get(0).message().content().get();

        try {
            JsonNode json = objectMapper.readTree(content);
            Map<String, String> result = new HashMap<>();
            fieldsToTranslate.keySet().forEach(field ->
                    result.put(field, json.has(field) ? json.get(field).asText() : null)
            );
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse translation JSON: " + content, e);
        }
    }

    private String buildTranslateFieldsPrompt(Map<String, String> fields, String targetLanguage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Translate the following fields to %s.\n", targetLanguage));
        prompt.append("Respond strictly as JSON with the same keys and translated text values.\n\n");

        fields.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                prompt.append(String.format("%s: %s\n", key, value));
            }
        });
        return prompt.toString();
    }
}
