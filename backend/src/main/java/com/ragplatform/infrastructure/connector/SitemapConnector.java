package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapConnector implements DataSourceConnector {

    private final WebClient.Builder webClientBuilder;
    private static final int MAX_URLS = 50;

    @Override
    public SourceType supportedType() {
        return SourceType.SITEMAP;
    }

    @Override
    public List<FetchedDocument> fetch(String url, Map<String, Object> config) {
        log.info("Fetching sitemap: {}", url);
        List<String> urls = parseSitemapUrls(url);

        if (urls.isEmpty()) {
            log.warn("No URLs found in sitemap: {}", url);
            return List.of();
        }

        log.info("Found {} URLs in sitemap, processing up to {}", urls.size(), MAX_URLS);
        List<FetchedDocument> documents = new ArrayList<>();

        for (String pageUrl : urls.subList(0, Math.min(urls.size(), MAX_URLS))) {
            try {
                String html = webClientBuilder.build()
                        .get()
                        .uri(pageUrl)
                        .header("User-Agent", "RAGPlatformBot/1.0")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (html == null || html.isBlank()) continue;

                Document doc = Jsoup.parse(html, pageUrl);
                doc.select("nav, header, footer, script, style, noscript, iframe").remove();

                String title = doc.title();
                String text = doc.body().text();

                if (!text.isBlank()) {
                    documents.add(new FetchedDocument(
                            pageUrl, title, text,
                            Map.of("url", pageUrl, "source_type", "SITEMAP", "sitemap_url", url)
                    ));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch page from sitemap: {}", pageUrl, e);
            }
        }

        log.info("Successfully fetched {} documents from sitemap", documents.size());
        return documents;
    }

    private List<String> parseSitemapUrls(String sitemapUrl) {
        try {
            String xml = webClientBuilder.build()
                    .get()
                    .uri(sitemapUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null) return List.of();

            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            return doc.select("url > loc").stream()
                    .map(el -> el.text().trim())
                    .filter(u -> !u.isBlank())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse sitemap: {}", sitemapUrl, e);
            return List.of();
        }
    }
}
