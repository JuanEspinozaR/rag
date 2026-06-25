package com.ragplatform.infrastructure.langfuse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for Langfuse ingestion API.
 * Uses the /api/public/ingestion batch endpoint with Basic auth.
 */
@Slf4j
@Component
public class LangfuseClient {

    private final WebClient webClient;
    private final LangfuseProperties properties;

    public LangfuseClient(WebClient.Builder webClientBuilder, LangfuseProperties properties) {
        this.properties = properties;
        String credentials = properties.getPublicKey() + ":" + properties.getSecretKey();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        this.webClient = webClientBuilder
                .baseUrl(properties.getHost())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void ingest(List<Map<String, Object>> batch) {
        if (!properties.isEnabled()) {
            log.debug("Langfuse is disabled, skipping ingestion");
            return;
        }

        Map<String, Object> body = Map.of("batch", batch);

        webClient.post()
                .uri("/api/public/ingestion")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(responseBody -> log.warn(
                                        "Langfuse ingestion rejected — HTTP {}: {}",
                                        response.statusCode(), responseBody))
                                .then(Mono.empty()))
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.warn("Langfuse ingestion error — host={} key={}: {}",
                            properties.getHost(),
                            properties.getPublicKey(),
                            e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
