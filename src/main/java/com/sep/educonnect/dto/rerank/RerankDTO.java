package com.sep.educonnect.dto.rerank;

import java.util.List;

public class RerankDTO {
    public record RerankCandidate(Integer tutorId, double os_score) {}
    public record RerankRequest(String user_id, List<Double> query_vector, List<RerankCandidate> candidates) {}

    public record RerankResponse(List<Double> scores) {}
}
