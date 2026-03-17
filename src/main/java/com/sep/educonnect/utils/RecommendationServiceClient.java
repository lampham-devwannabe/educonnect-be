package com.sep.educonnect.utils;

import com.sep.educonnect.dto.rerank.RerankDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceClient {

    @Qualifier("recommendationClient")
    private final WebClient recommendationClient;

    public List<Double> rerank(RerankDTO.RerankRequest rerankRequest) {
        try {
            var response = recommendationClient.post()
                    .uri("/rerank-new")
                    .bodyValue(rerankRequest)
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(RerankDTO.RerankResponse.class)
                    .timeout(Duration.ofSeconds(2))
                    .retryWhen(Retry.backoff(1, Duration.ofMillis(100)))
                    .block();

            return response != null ? response.scores() : List.of();
        } catch (Exception ex) {
            log.warn("Rerank service failed, falling back to OS ranking: {}", ex.toString());
            return List.of();
        }
    }
}
