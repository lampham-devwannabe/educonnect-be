package com.sep.educonnect.service.unit;

import com.sep.educonnect.constant.SearchConstants;
import com.sep.educonnect.dto.rerank.RerankDTO;
import com.sep.educonnect.dto.search.request.SearchTutorParams;
import com.sep.educonnect.dto.search.response.SearchTutorResponse;
import com.sep.educonnect.dto.search.response.TutorResponse;
import com.sep.educonnect.mapper.SearchTutorMapper;
import com.sep.educonnect.service.EmbeddingService;
import com.sep.educonnect.service.SearchService;
import com.sep.educonnect.utils.NativeQueryBuilder;
import com.sep.educonnect.utils.RecommendationServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private OpenSearchOperations opensearchOperations;

    @Mock
    private SearchTutorMapper searchTutorMapper;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RecommendationServiceClient recommendationServiceClient;

    @Mock
    private NativeQueryBuilder nativeQueryBuilder;

    @InjectMocks
    private SearchService searchService;

    @Test
    @DisplayName("Should search tutors by subject successfully")
    void should_searchTutorsBySubject_successfully() {
        // Given
        String subject = "Math";
        NativeQuery nativeQuery = mock(NativeQuery.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHits<Map> searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(searchHits.getExecutionDuration()).thenReturn(Duration.ofMillis(10));

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHit<Map> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(Map.of("id", 1));
        when(searchHit.getScore()).thenReturn(1.0f);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));

        when(nativeQueryBuilder.toSearchQuery(any(SearchTutorParams.class))).thenReturn(nativeQuery);
        when(opensearchOperations.search(eq(nativeQuery), eq(Map.class), eq(SearchConstants.Index.TUTORS)))
                .thenReturn(searchHits);

        TutorResponse tutorResponse = new TutorResponse(1, "Name", "Headline", "Avatar", List.of(), 5.0, 10, 5, 20,
                1.0f);
        when(searchTutorMapper.mapSearchResult(any(), anyFloat())).thenReturn(tutorResponse);

        // When
        SearchTutorResponse result = searchService.searchTutorsBySubject(subject, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.tutors().size());
        assertEquals(1, result.pagination().totalElements());
    }

    @Test
    @DisplayName("Should search tutor with query and reranking")
    void should_searchTutor_withQueryAndReranking() throws Exception {
        // Given
        SearchTutorParams params = new SearchTutorParams(
                "session", "user", "query", "subject", null, null, null, null, null, null, 0, 10);
        List<Double> vector = List.of(0.1, 0.2);

        // Set self-injection for caching
        setSelfInjection();

        NativeQuery bm25Query = mock(NativeQuery.class);
        NativeQuery neuralQuery = mock(NativeQuery.class);

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHits<Map> bm25Hits = mock(SearchHits.class);
        when(bm25Hits.getTotalHits()).thenReturn(1L);
        when(bm25Hits.getExecutionDuration()).thenReturn(Duration.ofMillis(10));

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHits<Map> neuralHits = mock(SearchHits.class);
        when(neuralHits.getTotalHits()).thenReturn(1L);

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHit<Map> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(Map.of("id", 1));
        when(searchHit.getScore()).thenReturn(1.0f);
        when(bm25Hits.getSearchHits()).thenReturn(List.of(searchHit));
        when(neuralHits.getSearchHits()).thenReturn(List.of(searchHit));

        when(nativeQueryBuilder.buildBM25Query(any(SearchTutorParams.class))).thenReturn(bm25Query);
        when(nativeQueryBuilder.buildNeuralQuery(any(SearchTutorParams.class))).thenReturn(neuralQuery);
        when(opensearchOperations.search(eq(bm25Query), eq(Map.class), eq(SearchConstants.Index.TUTORS)))
                .thenReturn(bm25Hits);
        when(opensearchOperations.search(eq(neuralQuery), eq(Map.class), eq(SearchConstants.Index.TUTORS)))
                .thenReturn(neuralHits);

        TutorResponse tutorResponse = new TutorResponse(1, "Name", "Headline", "Avatar", List.of(), 5.0, 10, 5, 20,
                1.0f);
        when(searchTutorMapper.mapSearchResult(any(), anyFloat())).thenReturn(tutorResponse);

        when(embeddingService.getTextInference("query")).thenReturn(vector);
        when(recommendationServiceClient.rerank(any(RerankDTO.RerankRequest.class))).thenReturn(List.of(0.9));

        // When
        SearchTutorResponse result = searchService.searchTutor(params);

        // Then
        assertNotNull(result);
        assertEquals(1, result.tutors().size());
        assertEquals(0.9f, result.tutors().get(0).score());
    }

    @Test
    @DisplayName("Should fallback when reranking fails")
    void should_fallback_whenRerankingFails() throws Exception {
        // Given
        SearchTutorParams params = new SearchTutorParams(
                "session", "user", "query", null, null, null, null, null, null, null, 0, 10);
        List<Double> vector = List.of(0.1);
        
        // Set self-injection for caching
        setSelfInjection();

        NativeQuery bm25Query = mock(NativeQuery.class);
        NativeQuery neuralQuery = mock(NativeQuery.class);

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHits<Map> bm25Hits = mock(SearchHits.class);
        when(bm25Hits.getTotalHits()).thenReturn(1L);
        when(bm25Hits.getExecutionDuration()).thenReturn(Duration.ofMillis(10));

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHits<Map> neuralHits = mock(SearchHits.class);
        when(neuralHits.getTotalHits()).thenReturn(1L);

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchHit<Map> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(Map.of("id", 1));
        when(searchHit.getScore()).thenReturn(10.0f); // High score
        when(bm25Hits.getSearchHits()).thenReturn(List.of(searchHit));
        when(neuralHits.getSearchHits()).thenReturn(List.of(searchHit));

        when(nativeQueryBuilder.buildBM25Query(any(SearchTutorParams.class))).thenReturn(bm25Query);
        when(nativeQueryBuilder.buildNeuralQuery(any(SearchTutorParams.class))).thenReturn(neuralQuery);
        when(opensearchOperations.search(eq(bm25Query), eq(Map.class), eq(SearchConstants.Index.TUTORS)))
                .thenReturn(bm25Hits);
        when(opensearchOperations.search(eq(neuralQuery), eq(Map.class), eq(SearchConstants.Index.TUTORS)))
                .thenReturn(neuralHits);

        TutorResponse tutorResponse = new TutorResponse(1, "Name", "Headline", "Avatar", List.of(), 5.0, 10, 5, 20,
                10.0f);
        when(searchTutorMapper.mapSearchResult(any(), anyFloat())).thenReturn(tutorResponse);

        when(embeddingService.getTextInference("query")).thenReturn(vector);
        when(recommendationServiceClient.rerank(any())).thenReturn(null); // Rerank fails

        // When
        SearchTutorResponse result = searchService.searchTutor(params);

        // Then
        assertNotNull(result);
        assertEquals(1, result.tutors().size());
        // When reranking fails, the original merged score is used (normalized from BM25 and neural)
        assertNotNull(result.tutors().get(0).score());
    }

    /**
     * Helper method to set self-injection for caching mechanism
     */
    private void setSelfInjection() throws Exception {
        Field selfField = SearchService.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(searchService, searchService);
    }
}
