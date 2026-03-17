package com.sep.educonnect.mapper;

import com.sep.educonnect.dto.search.response.TutorResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SearchTutorMapper {

    public TutorResponse mapSearchResult(Map<String, Object> source, Float score) {
        // Extract basic fields
        Integer id = extractInteger(source, "id");
        String avatarUrl = extractString(source, "avatarUrl");
        Double rating = extractDouble(source, "rating");
        Integer studentCount = extractInteger(source, "studentsCount");
        Integer reviewCount = extractInteger(source, "numberReviews");
        Integer totalLessons = extractInteger(source, "totalLessons");
        // Extract localized fields (name.vi, name.en, etc.)
        String name = extractString(source, "name.en");
        String headline = extractLocalizedField(source, "headline");
        List<String> tags = extractLocalizedList(source, "teachingStyle");

        return new TutorResponse(
                id,
                name,
                headline,
                avatarUrl,
                tags,
                rating != null ? rating : 0.0,
                studentCount != null ? studentCount : 0,
                reviewCount != null ? reviewCount : 0,
                totalLessons != null ? totalLessons : 0,
                score
        );
    }

    private String extractString(Map<String, Object> source, String key) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object nestedObject = source.get(parts[0]);

            if (nestedObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nestedObject;
                return extractString(nestedMap, parts[1]);
            }
            return null;
        }

        Object value = source.get(key);
        return value != null ? value.toString() : null;
    }

    private Long extractLong(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double extractDouble(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Integer extractInteger(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractLocalizedField(Map<String, Object> source, String fieldName) {
        // Try to get the localized field directly (e.g., "name.vi")
        Object value = source.get(fieldName);
        if (value instanceof String) {
            return (String) value;
        }

        // If it's a nested object, try to extract the localized value
        if (value instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) value;
            // Try common localized suffixes
            for (String suffix : new String[]{"vi", "en"}) {
                Object localizedValue = nested.get(suffix);
                if (localizedValue instanceof String) {
                    return (String) localizedValue;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractLocalizedList(Map<String, Object> source, String fieldName) {
        Object value = source.get(fieldName);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            // Check if it's a list of localized objects like [{en=value}, {en=value}]
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return list.stream()
                    .map(item -> {
                        if (item instanceof Map) {
                            Map<String, Object> localizedItem = (Map<String, Object>) item;
                            // Try to get the localized value (vi first, then en)
                            Object viValue = localizedItem.get("vi");
                            if (viValue instanceof String) {
                                return (String) viValue;
                            }
                            Object enValue = localizedItem.get("en");
                            if (enValue instanceof String) {
                                return (String) enValue;
                            }
                        }
                        return item.toString();
                    })
                    .filter(Objects::nonNull)
                    .toList();
            }
            // If it's already a list of strings
            return (List<String>) value;
        }

        // If it's a nested object with localized arrays
        if (value instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) value;
            for (String suffix : new String[]{"vi", "en"}) {
                Object localizedValue = nested.get(suffix);
                if (localizedValue instanceof List) {
                    return (List<String>) localizedValue;
                }
            }
        }

        return List.of();
    }
}
