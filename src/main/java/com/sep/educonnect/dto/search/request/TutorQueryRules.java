package com.sep.educonnect.dto.search.request;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import com.sep.educonnect.constant.SearchConstants.Tutor;
import com.sep.educonnect.utils.OpenSearchUtil;
import com.sep.educonnect.utils.SearchFieldResolver;
import com.sep.educonnect.utils.QueryRule;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TutorQueryRules {

    public static final String BOOST_FIELD_FORMAT = "%s^%f";

    // Static, DI-free rules using SearchFieldResolver.localized()
    public static final QueryRule<SearchTutorParams> SUBJECT_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.subject()) && !srp.subject().trim().isEmpty(),
        srp -> OpenSearchUtil.buildNestedQuery(Tutor.SUBJECT,
                OpenSearchUtil.buildTermQuery(SearchFieldResolver.localized(Tutor.SUBJECT), srp.subject(), 1.0f))
    );

    public static final QueryRule<SearchTutorParams> LEVEL_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.level()) && !srp.level().isEmpty(),
        srp -> {
            var innerQuery = OpenSearchUtil.buildTermsQuery(
                    SearchFieldResolver.localized(Tutor.LEVEL),
                    srp.level(),
                    1.0f
            );
            var nestedInner = OpenSearchUtil.buildNestedQuery(Tutor.LEVEL, innerQuery);
            return OpenSearchUtil.buildNestedQuery(Tutor.SUBJECT, nestedInner);
        }
    );

    public static final QueryRule<SearchTutorParams> RATING_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.rating()),
        srp -> OpenSearchUtil.buildRangeQuery(Tutor.RATING,
                range -> range.gte(JsonData.of(srp.rating())))
    );

    public static final QueryRule<SearchTutorParams> PRICE_RANGE_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.lowestPrice()) || Objects.nonNull(srp.highestPrice()),
        srp -> OpenSearchUtil.buildRangeQuery(Tutor.PRICE,
            range -> {
                if (srp.lowestPrice() != null) range.gte(JsonData.of(Double.valueOf(srp.lowestPrice())));
                if (srp.highestPrice() != null) range.lte(JsonData.of(Double.valueOf(srp.highestPrice())));
                return range;
            })
    );

    public static final QueryRule<SearchTutorParams> AVAILABILITY_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.availabilities()) && !srp.availabilities().isEmpty(),
        srp -> buildAvailabilityNestedQuery(srp.availabilities())
    );

    public static final QueryRule<SearchTutorParams> TEACHING_STYLE_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.styles()) && !srp.styles().isEmpty(),
        srp -> OpenSearchUtil.buildNestedQuery(Tutor.TEACHING_STYLE,
            OpenSearchUtil.buildTermsQuery(SearchFieldResolver.localized(Tutor.TEACHING_STYLE), srp.styles(), 1.0f))
    );

    public static final QueryRule<SearchTutorParams> SEARCH_QUERY = QueryRule.of(
        srp -> Objects.nonNull(srp.query()) && !srp.query().trim().isEmpty(),
        srp -> {
            List<String> searchFields = Arrays.asList(
                    SearchFieldResolver.localized(Tutor.NAME) + "^2.0",
                    SearchFieldResolver.localized(Tutor.HEADLINE),
                    SearchFieldResolver.localized(Tutor.DESCRIPTION)
            );
            return OpenSearchUtil.buildMultimatchQuery(searchFields, srp.query());
        }
    );

    private static Query buildAvailabilityNestedQuery(List<SearchTutorParams.AvailabilityRange> availabilities) {
        // Build nested query for availability matching
        // Match day and check if the time range overlaps
        return OpenSearchUtil.buildNestedQuery(Tutor.AVAILABILITY,
                OpenSearchUtil.buildAvailabilityBoolQuery(availabilities));
    }
}
