package com.sep.educonnect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.educonnect.constant.SearchConstants;
import com.sep.educonnect.dto.log.SearchLog;
import com.sep.educonnect.dto.rerank.RerankDTO;
import com.sep.educonnect.dto.search.request.SearchTutorParams;
import com.sep.educonnect.dto.search.response.Pagination;
import com.sep.educonnect.dto.search.response.SearchTutorResponse;
import com.sep.educonnect.dto.search.response.TutorResponse;
import com.sep.educonnect.mapper.SearchTutorMapper;
import com.sep.educonnect.utils.NativeQueryBuilder;
import com.sep.educonnect.utils.RecommendationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.data.core.OpenSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final OpenSearchOperations opensearchOperations;
    private final SearchTutorMapper searchTutorMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger searchLogger = LoggerFactory.getLogger("com.sep.educonnect.search");
    private final NativeQueryBuilder nativeQueryBuilder;
    private final EmbeddingService embeddingService;
    private final RecommendationServiceClient recommendationServiceClient;
    
    // Self-injection to enable @Cacheable to work when calling from within the same class
    // Using @Lazy to avoid circular dependency issues
    private SearchService self;
    
    @Autowired
    @Lazy
    public void setSelf(SearchService self) {
        this.self = self;
    }

    @Cacheable(cacheNames = "tutorsBySubject", key = "#subject + '_' + #page + '_' + #size")
    public SearchTutorResponse searchTutorsBySubject(String subject, Integer page, Integer size) {
        log.info("Search tutors by subject: {}, page: {}, size: {}", subject, page, size);

        try {
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 10;

            // Create params with only subject filter
            var params = new SearchTutorParams(
                    null, null, null, subject, null,
                    null, null, null, null,
                    null, pageNum, pageSize
            );

            // Build simple query with only subject filter and pagination
            var query = nativeQueryBuilder.toSearchQuery(params);

            var searchHits = opensearchOperations.search(query, Map.class, SearchConstants.Index.TUTORS);
            log.info("Total hits for subject '{}': {}", subject, searchHits.getTotalHits());

            // Map OpenSearch hits to TutorResponse (no reranking, just use OS scores)
            var tutors = searchHits.getSearchHits()
                    .stream()
                    .map(hit -> searchTutorMapper.mapSearchResult(hit.getContent(), hit.getScore()))
                    .toList();

            // Build paginated response
            long totalElements = searchHits.getTotalHits();
            int totalPages = Math.toIntExact(pageSize > 0 ? (long) Math.ceil((double) totalElements / pageSize) : 1);
            var pagination = new Pagination(pageNum, tutors.size(), totalElements, totalPages);

            return new SearchTutorResponse(
                    tutors,
                    pagination,
                    searchHits.getExecutionDuration().toMillis(),
                    "",
                    List.of("subject:" + subject)
            );
        } catch (Exception e) {
            log.error("Error during subject search operation", e);
            throw new RuntimeException("Subject search operation failed", e);
        }
    }

    public SearchTutorResponse searchTutor(SearchTutorParams params) {
        log.info("Search request received with params: {}", params);
        searchLogger.info("Search request started");

        try {
            // Prefetch a larger candidate set for reranking
            final int DEFAULT_TOP_N = 50;
            final int MAX_PREFETCH = 300;
            int requestedN = Math.max(10, (params.page() != null && params.size() != null)
                    ? ((params.page() + 1) * params.size())
                    : DEFAULT_TOP_N);
            int prefetchN = Math.min(MAX_PREFETCH, Math.max(DEFAULT_TOP_N, requestedN));

            // Create search params without pagination for caching
            var prefetchParams = new SearchTutorParams(
                    params.sessionId(), params.userId(), params.query(), params.subject(), params.level(),
                    params.rating(), params.lowestPrice(), params.highestPrice(), params.availabilities(),
                    params.styles(), 0, prefetchN
            );

            // Get cached candidates (this will cache the expensive OpenSearch queries and merging)
            // Use self-injection to call through proxy so @Cacheable works
            var candidateResult = self.getCachedCandidates(prefetchParams , prefetchN);
            var candidates = candidateResult.candidates();
            var totalHits = candidateResult.totalHits();
            var executionDurationMs = candidateResult.executionDurationMs();

            // Apply reranking if query exists
            List<TutorResponse> reranked = applyReranking(params, candidates);

            // Build paginated response with correct total hits
            var response = buildTutorResponseFromList(params, reranked, totalHits, executionDurationMs);

            if (totalHits > 0) {
                logSearchResults(params, response, candidates);
            }

            return response;
        } catch (Exception e) {
            log.error("Error during search operation", e);
            throw new RuntimeException("Search operation failed", e);
        }
    }

    /**
     * Cached method to fetch and merge BM25 + Neural candidates
     * Cache key is based on search params without pagination, so same search criteria
     * will reuse the same candidate set across different pages
     */
    @Cacheable(cacheNames = "tutors", key = "T(com.sep.educonnect.service.SearchService).cacheKey(#searchParams) + '_prefetch_' + #prefetchN")
    public CandidateResult getCachedCandidates(SearchTutorParams searchParams, int prefetchN) {
        var prefetchParams = new SearchTutorParams(
                searchParams.sessionId(), searchParams.userId(), searchParams.query(), searchParams.subject(), searchParams.level(),
                searchParams.rating(), searchParams.lowestPrice(), searchParams.highestPrice(), searchParams.availabilities(),
                searchParams.styles(), 0, prefetchN
        );
        // Run BM25 query
        var bm25Query = nativeQueryBuilder.buildBM25Query(prefetchParams);
        var bm25Hits = opensearchOperations.search(bm25Query, Map.class, SearchConstants.Index.TUTORS);
        log.info("BM25 query executed, total hits: {}, takes: {}", bm25Hits.getTotalHits(), bm25Hits.getExecutionDuration());

        // Map to store merged results by tutor ID
        Map<Integer, TutorResponse> mergedResults = new HashMap<>();
        
        // Process BM25 results
        bm25Hits.getSearchHits().forEach(hit -> {
            var tutor = searchTutorMapper.mapSearchResult(hit.getContent(), hit.getScore());
            mergedResults.put(tutor.id(), tutor);
        });
        
        // Track neural hits for total count calculation
        SearchHits<Map> neuralHits = null;
        
        // Run neural query if text query exists
        if (searchParams.hasTextQuery()) {
            var neuralQuery = nativeQueryBuilder.buildNeuralQuery(prefetchParams);
            if (neuralQuery != null) {
                neuralHits = opensearchOperations.search(neuralQuery, Map.class, SearchConstants.Index.TUTORS);
                log.info("Neural query executed, total hits: {}, take: {}", neuralHits.getTotalHits(), neuralHits.getExecutionDuration());
                
                // Merge neural results with BM25 results using weighted score combination
                neuralHits.getSearchHits().forEach(hit -> {
                    var neuralTutor = searchTutorMapper.mapSearchResult(hit.getContent(), hit.getScore());
                    mergedResults.merge(neuralTutor.id(), neuralTutor, (existing, neural) -> {
                        float bm25Score     = existing.score() != null ? existing.score() : 0f;
                        float annRawScore   = neural.score() != null ? neural.score() : -1_000_000f;
                        // Normalizing ANN score
                        float normalizedAnn = 1f / (1f + Math.abs(annRawScore));

                        // Normalizing BM25
                        float normalizedBm25 = Math.min(1f, bm25Score / 8f);

                        // Weighted fusion
                        float finalScore = 0.7f * normalizedBm25 + 0.3f * normalizedAnn;

                        return existing.withScore(finalScore);
                    });
                });
            }
        }
        
        // Convert to list and sort by combined score (descending)
        var candidates = new ArrayList<>(mergedResults.values());
        candidates.sort(Comparator.comparingDouble((TutorResponse t) -> 
                t.score() != null ? t.score() : 0.0).reversed());
        
        // Determine total hits: use the larger of BM25 or neural, or merged results count
        long totalHits;
        if (neuralHits != null) {
            // Use the maximum of BM25 and neural total hits, or merged results count
            totalHits = Math.max(bm25Hits.getTotalHits(), neuralHits.getTotalHits());
            // But don't exceed the actual merged results count
            totalHits = Math.max(totalHits, candidates.size());
        } else {
            // No neural query, use BM25 total hits
            totalHits = bm25Hits.getTotalHits();
        }
        
        log.info("Merged results: {} unique tutors from {} BM25 hits and {} neural hits (total: {})", 
                candidates.size(), bm25Hits.getTotalHits(), 
                neuralHits != null ? neuralHits.getTotalHits() : 0, totalHits);

        // Use execution duration from the query that had more hits, or BM25 if equal
        long executionDurationMs = bm25Hits.getExecutionDuration().toMillis();

        return new CandidateResult(candidates, totalHits, executionDurationMs);
    }

    /**
     * Record to hold candidate search results
     */
    private record CandidateResult(
            List<TutorResponse> candidates,
            long totalHits,
            long executionDurationMs
    ) {}

    /**
     * Apply reranking using RecommendationServiceClient if query exists
     * Returns reranked candidates or original candidates if reranking fails/unavailable
     */
    private List<TutorResponse> applyReranking(SearchTutorParams params, List<TutorResponse> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        try {
            // Generate query embedding for reranking
            List<Double> queryVector = null;
            if (params.hasTextQuery())
                queryVector = embeddingService.getTextInference(params.query().trim());

            // Build rerank request
            List<RerankDTO.RerankCandidate> rerankCandidates = candidates.stream()
                    .map(t -> new RerankDTO.RerankCandidate(
                            t.id(),
                            t.score() != null ? t.score() : 0.0
                    ))
                    .toList();

            var rerankRequest = new RerankDTO.RerankRequest(
                    params.userId() != null ? params.userId() : "",
                    queryVector,
                    rerankCandidates
            );

            // Call rerank service
            var scores = recommendationServiceClient.rerank(rerankRequest);

            // Set model version based on whether reranking succeeded
            MDC.put("rerankModelVersion", scores != null && !scores.isEmpty() ? "v2.1" : "v1.1");

            // Apply rerank scores if available and match candidates size
            if (scores != null && scores.size() == candidates.size()) {
                return applyScoresAndSort(candidates, scores);
            } else {
                log.warn("Rerank scores size mismatch or empty: expected {}, got {}", 
                        candidates.size(), scores != null ? scores.size() : 0);
                return candidates;
            }
        } catch (Exception e) {
            log.error("Error during reranking, falling back to original scores", e);
            MDC.put("rerankModelVersion", "v1.1");
            return candidates;
        }
    }

    /**
     * Apply rerank scores to candidates and sort by new scores
     */
    private List<TutorResponse> applyScoresAndSort(List<TutorResponse> candidates, List<Double> scores) {
        return IntStream.range(0, candidates.size())
                .mapToObj(i -> candidates.get(i).withScore(scores.get(i).floatValue()))
                .sorted(Comparator.comparingDouble(t -> -t.score())) // Negative for descending
                .toList();
    }

    private SearchTutorResponse buildTutorResponseFromList(SearchTutorParams params, List<TutorResponse> topN, long totalHits, long executionDurationMs) {
        int page = params.page() != null ? params.page() : 0;
        int size = params.size() != null ? params.size() : 10;

        int from = Math.max(0, page * size);
        int to = Math.min(topN.size(), from + size);
        List<TutorResponse> pageItems = from < to ? topN.subList(from, to) : java.util.Collections.emptyList();

        long totalElements = totalHits;
        int totalPages = Math.toIntExact(size > 0 ? (long) Math.ceil((double) totalElements / size) : 1);

        var pagination = new Pagination(page, pageItems.size(), totalElements, totalPages);

        return new SearchTutorResponse(
                pageItems,
                pagination,
                executionDurationMs,
                params.query() != null ? params.query() : "",
                extractFilters(params)
        );
    }

    public static String cacheKey(SearchTutorParams params) {
        String user = params.userId() != null ? params.userId() : "anon";
        String q = params.query() != null ? params.query().trim() : "";
        String subject = params.subject() != null ? params.subject().trim() : "";

        String levels = params.level() != null ? params.level().stream()
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("") : "";

        String rating = params.rating() != null ? String.valueOf(params.rating()) : "";

        String price = (params.lowestPrice() != null ? params.lowestPrice() : "") + "-" +
                (params.highestPrice() != null ? params.highestPrice() : "");

        String styles = params.styles() != null ? params.styles().stream()
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("") : "";

        // availability days only
        String days = params.availabilities() != null ? params.availabilities().stream()
                .map(SearchTutorParams.AvailabilityRange::day)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("") : "";

        return String.join("|",
                "v1",
                user,
                q,
                subject,
                levels,
                rating,
                price,
                styles,
                days
        );
    }

    private List<String> extractFilters(SearchTutorParams params) {
        List<String> filters = new ArrayList<>();

        // Subject filter
        if (params.subject() != null && !params.subject().trim().isEmpty()) {
            filters.add("subject:" + params.subject());
        }

        // Level filters
        if (params.level() != null && !params.level().isEmpty()) {
            params.level().forEach(level -> filters.add("level:" + level));
        }

        // Rating filter
        if (params.rating() != null) {
            filters.add("rating:>=" + params.rating());
        }

        if (params.lowestPrice() != null || params.highestPrice() != null)
            filters.add("price:" +
                    (params.lowestPrice() != null ? params.lowestPrice() : "0") +
                    "-" +
                    (params.highestPrice() != null ? params.highestPrice() : "∞"));

        // Teaching style filters
        if (params.styles() != null && !params.styles().isEmpty()) {
            params.styles().forEach(style -> filters.add("style:" + style));
        }

        if (params.availabilities() != null && !params.availabilities().isEmpty()) {
            String availability = params.availabilities().stream()
                    .map(a -> a.day() + "[" + a.start() + "-" + a.end() + "]")
                    .collect(Collectors.joining(","));
            filters.add("availability:" + availability);
        }

        return filters;
    }

    private void logSearchResults(SearchTutorParams params, SearchTutorResponse response, List<TutorResponse> originalCandidates) {
        try {
            // Create search results with OS scores from original candidates
            var searchResults = extractSearchResults(response.tutors(), originalCandidates);
            String resultsJson = objectMapper.writeValueAsString(searchResults);

            // Set MDC values for structured logging
            MDC.put("sessionId", params.sessionId() != null ? params.sessionId() : "");
            MDC.put("version", "v2.1");
            MDC.put("userId", params.userId() != null ? params.userId() : "");
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("page", String.valueOf(params.page() != null ? params.page() : 0));
            MDC.put("size", String.valueOf(params.size() != null ? params.size() : 10));
            MDC.put("query", params.query() != null ? params.query() : "");
            MDC.put("filters", extractFilters(params).toString());
            MDC.put("results", resultsJson);

            // Log the search result
            searchLogger.info("Search completed successfully");

            // Clear MDC to avoid memory leaks
            MDC.clear();
        } catch (Exception e) {
            log.error("Error logging search results", e);
            MDC.clear();
        }
    }

    private List<SearchLog.SearchResult> extractSearchResults(List<TutorResponse> responses, List<TutorResponse> originalCandidates) {
        // Create a map of tutor ID to original OS score for quick lookup
        Map<Integer, Float> osScoreMap = originalCandidates.stream()
                .collect(Collectors.toMap(
                        TutorResponse::id,
                        t -> t.score() != null ? t.score() : 0.0f,
                        (existing, replacement) -> existing
                ));

        return IntStream.range(0, responses.size())
                .mapToObj(i -> {
                    TutorResponse tutor = responses.get(i);
                    Float osScore = osScoreMap.getOrDefault(tutor.id(), null);
                    return new SearchLog.SearchResult(
                            tutor.id(),
                            i + 1,
                            osScore,
                            tutor.score()
                    );
                })
                .toList();
    }
}
