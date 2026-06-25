package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownConnector implements DataSourceConnector {

    private final WebClient.Builder webClientBuilder;

    @Override
    public SourceType supportedType() {
        return SourceType.MARKDOWN_URL;
    }

    @Override
    public List<FetchedDocument> fetch(String url, Map<String, Object> config) {
        log.info("Fetching Markdown from URL: {}", url);
        try {
            String content = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("User-Agent", "RAGPlatformBot/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (content == null || content.isBlank()) {
                log.warn("Empty content from Markdown URL: {}", url);
                return List.of();
            }

            String title = extractMarkdownTitle(content, url);

            Map<String, Object> metadata = Map.of(
                    "url", url,
                    "source_type", "MARKDOWN_URL"
            );

            return List.of(new FetchedDocument(url, title, content, metadata));

        } catch (Exception e) {
            log.error("Failed to fetch Markdown from URL: {}", url, e);
            throw new RuntimeException("Failed to fetch Markdown: " + url, e);
        }
    }

    private String extractMarkdownTitle(String content, String url) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return url.substring(url.lastIndexOf('/') + 1).replace(".md", "");
    }
}
