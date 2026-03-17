package com.sep.educonnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final OpenSearchClient openSearchClient;
    @Value("${spring.opensearch.uris}")
    private String baseUrl;

    @Value("${spring.opensearch.username}")
    private String user;

    @Value("${spring.opensearch.password}")
    private String password;

    @Value("${spring.opensearch.model}")
    private String modelId;

    public List<Double> embed(String text) {
        String inferencePath = "/_plugins/_ml/_predict/text_embedding/" + modelId;
        try {
            String fullBase = baseUrl.startsWith("https") ? baseUrl : "https://" + baseUrl;
            String url = fullBase + ":9200" + "/_plugins/_ml/_predict/text_embedding/" + modelId;
            // Header + auth
            HttpHeaders headers = new HttpHeaders();
            String auth = user + ":" + password;
            headers.setBasicAuth(auth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Request body — OpenSearch Inference API (text embedding) expects "input"
            Map<String, Object> body = Map.of(
                    "text_docs", List.of(text),
                    "parameters", Map.of("content_type", "passage"),
                    "target_response", List.of("sentence_embedding")
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Inference API failed with status: " + response.getStatusCode());
            }

            // Extract vector
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) ((Map) ((List) ((Map) ((List) response.getBody()
                    .get("inference_results")).get(0)).get("output")).get(0)).get("data");

            return vector;
        } catch (Exception e) {
            log.error("Failed to embed query text", e);
            return null;
        }
    }

    public List<Double> getTextInference(String text) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_docs", List.of(text));
        requestBody.put("return_number", true);
        requestBody.put("target_response", List.of("sentence_embedding"));

        ObjectMapper objectMapper = new ObjectMapper();
        requestBody = objectMapper.convertValue(requestBody, Map.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);

            // Make the predict API call using Requests.builder()
            var response = openSearchClient.generic().execute(
                    Requests.builder().endpoint("/_plugins/_ml/_predict/text_embedding/" + modelId)
                            .method("POST")
                            .json(jsonBody)
                            .build()
            );

            String body = response.getBody().get().bodyAsString();
            JsonNode root = mapper.readTree(body);
            JsonNode dataNode = root.path("inference_results").get(0).path("output").get(0).path("data");
            List<Double> embedding = new ArrayList<>();
            for (JsonNode val : dataNode) {
                embedding.add(val.doubleValue());
            }
            return embedding;

        } catch (Exception e) {
            log.error("Error during inference: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
