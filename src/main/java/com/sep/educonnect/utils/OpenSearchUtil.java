package com.sep.educonnect.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.sep.educonnect.constant.SearchConstants;
import com.sep.educonnect.constant.SearchConstants.Fuzzy;
import com.sep.educonnect.dto.search.request.SearchTutorParams;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;

@Slf4j
public class OpenSearchUtil {
    public static Query buildTermQuery(String field, String value, float boost) {
        var termQuery = TermQuery.of(builder -> builder
                .field(field)
                .boost(boost)
                .value(FieldValue.of(value))
                .caseInsensitive(true)
        );

        return Query.of(builder -> builder.term(termQuery));
    }

    public static Query buildRangeQuery(String field, UnaryOperator<RangeQuery.Builder> function) {
        RangeQuery rangeQuery = function.apply(new RangeQuery.Builder().field(field)).build();
        return Query.of(builder -> builder.range(rangeQuery));
    }

    public static Query buildMultimatchQuery(List<String> fields, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Query.of(builder -> builder.matchAll(MatchAllQuery.of(b -> b)));
        }

        var multiMatchQuery = MultiMatchQuery.of(builder -> builder
                .query(searchTerm)
                .fields(fields)
                .fuzziness(Fuzzy.LEVEL)
                .prefixLength(Fuzzy.PREFIX_LENGTH)
                .type(TextQueryType.MostFields)
                .operator(Operator.Or)
        );

        return Query.of(builder -> builder.multiMatch(multiMatchQuery));
    }

    public static Query buildScriptScoreQuery(
            Query baseQuery,
            float[] queryVector,
            String vectorField,
            double keywordWeight,
            double semanticWeight) {

        String scriptSource =
            "double cosine = cosineSimilarity(params.query_vector, doc['" + vectorField + "']); " +
                    "return " + semanticWeight + " * cosine + " + keywordWeight + " * _score;";

        return Query.of(q -> q
            .scriptScore(ss -> ss
                .query(baseQuery)
                .script(script -> script
                    .inline(in -> in
                        .source(scriptSource)
                        .params("query_vector", JsonData.of(queryVector))
                    )
                )
            )
        );
    }

    public static Query buildScriptScoreQuery(
            Query baseQuery,
            List<Double> queryVector,
            String vectorField,
            double keywordWeight,
            double semanticWeight) {
        String scriptSource =
            "double cosine = cosineSimilarity(params.query_vector, doc['" + vectorField + "']); " +
                    "return " + semanticWeight + " * cosine + " + keywordWeight + " * _score;";

        return Query.of(q -> q
            .scriptScore(ss -> ss
                .query(baseQuery)
                .script(script -> script
                    .inline(in -> in
                        .source(scriptSource)
                        .params("query_vector", JsonData.of(queryVector))
                    )
                )
            )
        );
    }

    /**
     * Build a neural embedding query for semantic search
     */
    public static Query buildNeuralQuery(String queryText, String modelId, String embeddingField) {
        var neuralQuery = NeuralQuery.of(builder -> builder
                .queryText(queryText)
                .field(embeddingField)
                .modelId(modelId)
        );

        return Query.of(builder -> builder.neural(neuralQuery));
    }

    /**
     * Build a nested query for filtering nested fields
     */
    public static Query buildNestedQuery(String path, Query innerQuery) {
        return Query.of(q -> q
            .nested(n -> n
                .path(path)
                .query(innerQuery)
            )
        );
    }

    /**
     * Build a terms query for multiple values
     */
    public static Query buildTermsQuery(String field, List<String> values, float boost) {
        var termsQuery = TermsQuery.of(builder -> builder
                .field(field)
                .terms(t -> t.value(values.stream()
                        .map(FieldValue::of)
                        .toList()))
                .boost(boost)
        );

        return Query.of(builder -> builder.terms(termsQuery));
    }

    /**
     * Build a bool query for availability matching
     * Matches day and checks time range overlap
     */
    public static Query buildAvailabilityBoolQuery(List<SearchTutorParams.AvailabilityRange> availabilities) {
        return Query.of(builder -> builder
            .bool(bool -> bool
                .should(availabilities.stream()
                    .map(OpenSearchUtil::buildSingleAvailabilityQuery)
                    .toList())
            )
        );
    }

    /**
     * Build query for a single availability range
     */
    public static Query buildSingleAvailabilityQuery(SearchTutorParams.AvailabilityRange availability) {
        return Query.of(builder -> builder
                .bool(bool -> bool
                        .must(buildTermQuery(SearchConstants.Tutor.AVAILABILITY_DAY, availability.day(), 1.0f))
                        .must(buildRangeQuery(SearchConstants.Tutor.AVAILABILITY_START_TIME,
                                range -> range.lte(JsonData.of(availability.end()))))
                        .must(buildRangeQuery(SearchConstants.Tutor.AVAILABILITY_END_TIME,
                                range -> range.gte(JsonData.of(availability.start()))))
                )
        );
    }

    // ========== Indexing Methods for CDC Sync ==========

    /**
     * Index a full document to OpenSearch with pipeline processing.
     * Used when status changes to APPROVED (full document creation).
     * 
     * @param client OpenSearch client
     * @param profileId Document ID (tutor profile ID)
     * @param document Full document to index
     * @param pipelineName Pipeline name for processing
     */
    public static void indexFullDocument(
            OpenSearchClient client,
            Long profileId,
            Map<String, Object> document,
            String pipelineName) throws java.io.IOException {
        try {
            var request = IndexRequest.of(b -> b
                    .index(SearchConstants.Index.TUTORS.getIndexName())
                    .id(String.valueOf(profileId))
                    .document(document)
                    .pipeline(pipelineName)
            );
            var response = client.index(request);
            log.info("Indexed full document: profileId={}, result={}", profileId, response.result());
        } catch (Exception e) {
            log.error("Failed to index full document: profileId={}", profileId, e);
            throw e;
        }
    }

    /**
     * Perform a partial update to OpenSearch document.
     * Used for non-embedding field updates (price, availability, tags, etc.).
     * 
     * @param client OpenSearch client
     * @param profileId Document ID
     * @param partialDoc Fields to update
     */
    public static void partialUpdate(
            OpenSearchClient client,
            Long profileId,
            Map<String, Object> partialDoc) throws IOException {
        try {
            var request = UpdateRequest.of(b -> b
                    .index(SearchConstants.Index.TUTORS.getIndexName())
                    .id(String.valueOf(profileId))
                    .doc(partialDoc)
            );
            var response = client.update((UpdateRequest<Object, Object>) request, Object.class);
            log.info("Partial update completed: profileId={}, result={}", profileId, response.result());
        } catch (OpenSearchException e) {
            if (e.status() == 404) {
                log.warn("Document not found for partial update: profileId={}", profileId);
            } else {
                log.error("Failed to perform partial update: profileId={}", profileId, e);
            }
        }
    }

    /**
     * Update document using script (for metrics like totalLessons).
     * 
     * @param client OpenSearch client
     * @param profileId Document ID
     * @param scriptSource Script source code
     * @param scriptParams Script parameters
     */
    public static void scriptUpdate(
            OpenSearchClient client,
            Long profileId,
            String scriptSource,
            Map<String, JsonData> scriptParams) throws java.io.IOException {
        scriptUpdate(client, profileId, scriptSource, scriptParams, "painless");
    }

    public static void scriptUpdate(
            OpenSearchClient client,
            Long profileId,
            String scriptSource,
            Map<String, JsonData> scriptParams,
            String lang) throws java.io.IOException {
        try {
            var scriptBuilder = Script.of(s -> s
                    .inline(in -> in
                            .source(scriptSource)
                            .params(scriptParams != null ? scriptParams : Map.of())
                    )
            );

            var request = UpdateRequest.of(b -> b
                    .index(SearchConstants.Index.TUTORS.getIndexName())
                    .id(String.valueOf(profileId))
                    .script(scriptBuilder)
            );
            var response = client.update((UpdateRequest<Object, Object>) request, Object.class);
            log.info("Script update completed: profileId={}, result={}", profileId, response.result());
        } catch (OpenSearchException e) {
            if (e.status() == 404) {
                log.warn("Document not found for script update: profileId={}", profileId);
            } else {
                log.error("Failed to perform script update: profileId={}", profileId, e);
                throw e;
            }
        }
    }

    /**
     * Delete document from OpenSearch.
     * Used when profile status changes from APPROVED to DRAFT/REJECTED or profile is deleted.
     * 
     * @param client OpenSearch client
     * @param profileId Document ID
     */
    public static void deleteDocument(
            OpenSearchClient client,
            Long profileId) throws java.io.IOException {
        try {
            var request = DeleteRequest.of(b -> b
                    .index(SearchConstants.Index.TUTORS.getIndexName())
                    .id(String.valueOf(profileId))
            );
            var response = client.delete(request);
            log.info("Deleted document: profileId={}, result={}", profileId, response.result());
        } catch (OpenSearchException e) {
            if (e.status() == 404) {
                log.warn("Document not found for deletion: profileId={}", profileId);
            } else {
                log.error("Failed to delete document: profileId={}", profileId, e);
                throw e;
            }
        }
    }

    // ========== Student Indexing Methods ==========

    /**
     * Index a full student document to OpenSearch.
     * Used when student is created (email verified + role STUDENT).
     */
    public static void indexStudentDocument(
            OpenSearchClient client,
            String userId,
            Map<String, Object> document) throws java.io.IOException {
        try {
            var request = IndexRequest.of(b -> b
                    .index(SearchConstants.Index.STUDENTS.getIndexName())
                    .id(userId)
                    .document(document)
            );
            var response = client.index(request);
            log.info("Indexed student document: userId={}, result={}", userId, response.result());
        } catch (Exception e) {
            log.error("Failed to index student document: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Perform a partial update to student document.
     * 
     * @param client OpenSearch client
     * @param userId Student user ID
     * @param partialDoc Fields to update
     */
    public static void partialUpdateStudent(
            OpenSearchClient client,
            String userId,
            Map<String, Object> partialDoc) throws IOException {
        try {
            var request = UpdateRequest.of(b -> b
                    .index(SearchConstants.Index.STUDENTS.getIndexName())
                    .id(userId)
                    .doc(partialDoc)
            );
            var response = client.update((UpdateRequest<Object, Object>) request, Object.class);
            log.info("Partial update completed for student: userId={}, result={}", userId, response.result());
        } catch (OpenSearchException e) {
            if (e.status() == 404) {
                log.warn("Student document not found for partial update: userId={}", userId);
            } else {
                log.error("Failed to perform partial update for student: userId={}", userId, e);
                throw e;
            }
        }
    }

    /**
     * Delete student document from OpenSearch.
     * 
     * @param client OpenSearch client
     * @param userId Student user ID
     */
    public static void deleteStudentDocument(
            OpenSearchClient client,
            String userId) throws java.io.IOException {
        try {
            var request = DeleteRequest.of(b -> b
                    .index(SearchConstants.Index.STUDENTS.getIndexName())
                    .id(userId)
            );
            var response = client.delete(request);
            log.info("Deleted student document: userId={}, result={}", userId, response.result());
        } catch (OpenSearchException e) {
            if (e.status() == 404) {
                log.warn("Student document not found for deletion: userId={}", userId);
            } else {
                log.error("Failed to delete student document: userId={}", userId, e);
                throw e;
            }
        }
    }
}
