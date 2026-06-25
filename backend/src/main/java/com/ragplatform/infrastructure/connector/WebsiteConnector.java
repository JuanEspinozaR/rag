package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebsiteConnector implements DataSourceConnector {

    private final WebClient.Builder webClientBuilder;

    @Override
    public SourceType supportedType() {
        return SourceType.WEBSITE_URL;
    }

    @Override
    public List<FetchedDocument> fetch(String url, Map<String, Object> config) {
        log.info("Fetching website URL: {}", url);
        try {
            String html = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("User-Agent", "RAGPlatformBot/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (html == null || html.isBlank()) {
                log.warn("Empty response from URL: {}", url);
                return List.of();
            }

            Document doc = Jsoup.parse(html, url);

            // Remove boilerplate elements
            doc.select("nav, header, footer, script, style, noscript, iframe, .cookie-banner, #cookie").remove();

            String title = doc.title();
            String text = doc.body().text();

            if (text.isBlank()) {
                log.warn("No text content extracted from URL: {}", url);
                return List.of();
            }

            Map<String, Object> metadata = Map.of(
                    "url", url,
                    "source_type", "WEBSITE_URL"
            );

            return List.of(new FetchedDocument(url, title, text, metadata));

        } catch (Exception e) {
            log.error("Failed to fetch website URL: {}", url, e);
            throw new RuntimeException("Failed to fetch URL: " + url, e);
        }
    }
}
