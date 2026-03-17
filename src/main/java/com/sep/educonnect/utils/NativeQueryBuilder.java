package com.sep.educonnect.utils;

import com.sep.educonnect.dto.search.request.SearchTutorParams;
import com.sep.educonnect.dto.search.request.TutorQueryRules;
import com.sep.educonnect.constant.SearchConstants.Tutor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.ArrayList;

@Slf4j
@Component
public class NativeQueryBuilder {
    @Value("${spring.opensearch.model}")
    private String modelId;
    // Grouped rule lists
    public static final List<QueryRule<SearchTutorParams>> FILTER_QUERY_RULES = Arrays.asList(
        TutorQueryRules.SUBJECT_QUERY,
        TutorQueryRules.LEVEL_QUERY,
        TutorQueryRules.AVAILABILITY_QUERY
    );

    public static final List<QueryRule<SearchTutorParams>> MUST_QUERY_RULES = Arrays.asList(
        TutorQueryRules.RATING_QUERY,
        TutorQueryRules.PRICE_RANGE_QUERY,
        TutorQueryRules.SEARCH_QUERY
    );

    public static final List<QueryRule<SearchTutorParams>> SHOULD_QUERY_RULES = Arrays.asList(
        TutorQueryRules.TEACHING_STYLE_QUERY
    );

    /**
     * Build BM25 query only (bool query with filters and text search)
     */
    public NativeQuery buildBM25Query(SearchTutorParams params) {
        var filterQueries = buildQueries(FILTER_QUERY_RULES, params);
        var mustQueries = buildQueries(MUST_QUERY_RULES, params);
        var shouldQueries = buildQueries(SHOULD_QUERY_RULES, params);

        var boolQuery = BoolQuery.of(builder -> builder
                .filter(filterQueries)
                .must(mustQueries)
                .should(shouldQueries)
        );

        Query bm25Query = Query.of(builder -> builder.bool(boolQuery));

        int page = params.page() != null ? params.page() : 0;
        int size = params.size() != null ? params.size() : 10;

        return NativeQuery.builder()
                .withQuery(bm25Query)
                .withPageable(PageRequest.of(page, size))
                .withTrackTotalHits(true)
                .withSourceFilter(new FetchSourceFilter(
                        buildSourceIncludes().toArray(new String[0]), null))
                .build();
    }

    /**
     * Build neural query only (semantic search without filters)
     * Returns null if no text query is provided
     * Filters will be applied during backend merging with BM25 results
     */
    public NativeQuery buildNeuralQuery(SearchTutorParams params) {
        if (!params.hasTextQuery()) {
            return null;
        }

        // Build standalone neural query without any filters
        // This allows it to return semantic matches without restrictions
        // Filters will be applied when merging results in SearchService
        Query neuralQuery = OpenSearchUtil.buildNeuralQuery(
                params.query().trim(),
                modelId,
                Tutor.EMBEDDING
        );

        int page = params.page() != null ? params.page() : 0;
        int size = params.size() != null ? params.size() : 10;

        return NativeQuery.builder()
                .withQuery(neuralQuery)
                .withPageable(PageRequest.of(page, size))
                .withTrackTotalHits(true)
                .withSourceFilter(new FetchSourceFilter(
                        buildSourceIncludes().toArray(new String[0]), null))
                .build();
    }

    /**
     * Legacy method for backward compatibility (non-text queries)
     * For text queries, use buildBM25Query and buildNeuralQuery separately
     */
    public NativeQuery toSearchQuery(SearchTutorParams params) {
        return buildBM25Query(params);
    }

    private static <T> List<Query> buildQueries(List<QueryRule<T>> queryRules, T params) {
        return queryRules.stream()
            .map(qr -> qr.build(params))
            .flatMap(Optional::stream)
            .toList();
    }

    // Build _source includes: localized fields + general fields
    private static List<String> buildSourceIncludes() {
        List<String> includes = new ArrayList<>();
        // General fields
        includes.add(Tutor.ID);
        includes.add(Tutor.AVATAR_URL);
        includes.add(Tutor.PRICE);
        includes.add(Tutor.RATING);
        includes.add(Tutor.STUDENTS_COUNT);
        includes.add(Tutor.TOTAL_LESSONS);
        includes.add(Tutor.IS_PROFESSIONAL);
        includes.add(Tutor.TOTAL_LESSONS);
        // Always choose english name
        includes.add(Tutor.NAME_EN);
        // Localized fields
       includes.add(SearchFieldResolver.localized(Tutor.HEADLINE));
       includes.add(SearchFieldResolver.localized(Tutor.TEACHING_STYLE));

        // Exclude: description, country name, availability
        return includes;
    }
}
