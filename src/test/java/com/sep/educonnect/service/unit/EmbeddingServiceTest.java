package com.sep.educonnect.service.unit;

import com.sep.educonnect.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Response;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OpenSearchClient openSearchClient;

    @InjectMocks
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(embeddingService, "baseUrl", "localhost");
        ReflectionTestUtils.setField(embeddingService, "user", "admin");
        ReflectionTestUtils.setField(embeddingService, "password", "admin");
        ReflectionTestUtils.setField(embeddingService, "modelId", "model-1");
        ReflectionTestUtils.setField(embeddingService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Should embed text successfully")
    void should_embedText_successfully() {
        // Given
        String text = "hello";
        Map<String, Object> responseBody = Map.of(
                "inference_results", List.of(
                        Map.of("output", List.of(
                                Map.of("data", List.of(0.1, 0.2, 0.3))))));
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        List<Double> result = embeddingService.embed(text);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(0.1, result.get(0));
    }

    @Test
    @DisplayName("Should return null on embed failure")
    void should_returnNull_onEmbedFailure() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Error"));

        // When
        List<Double> result = embeddingService.embed("text");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should get text inference successfully")
    void should_getTextInference_successfully() throws Exception, IOException {
        // Given
        String text = "hello";
        String jsonResponse = "{\"inference_results\": [{\"output\": [{\"data\": [0.1, 0.2, 0.3]}]}]}";

        OpenSearchGenericClient genericClient = mock(OpenSearchGenericClient.class);
        when(openSearchClient.generic()).thenReturn(genericClient);

        Response response = mock(Response.class);
        Body body = mock(Body.class);
        when(body.bodyAsString()).thenReturn(jsonResponse);
        when(response.getBody()).thenReturn(Optional.of(body));

        when(genericClient.execute(any())).thenReturn(response);

        // When
        List<Double> result = embeddingService.getTextInference(text);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(0.1, result.get(0));
    }

    @Test
    @DisplayName("Should return null on inference failure")
    void should_returnNull_onInferenceFailure() throws IOException {
        // Given
        OpenSearchGenericClient genericClient = mock(OpenSearchGenericClient.class);
        when(openSearchClient.generic()).thenReturn(genericClient);
        when(genericClient.execute(any())).thenThrow(new RuntimeException("Error"));

        // When
        List<Double> result = embeddingService.getTextInference("text");

        // Then
        assertNull(result);
    }
}
