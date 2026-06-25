package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfConnector implements DataSourceConnector {

    private final WebClient.Builder webClientBuilder;

    @Override
    public SourceType supportedType() {
        return SourceType.PDF_URL;
    }

    private String normalizeUrl(String url) {
        // Convert GitHub blob viewer URL to raw download URL:
        // https://github.com/{user}/{repo}/blob/{ref}/{path}
        // → https://raw.githubusercontent.com/{user}/{repo}/{ref}/{path}
        if (url.contains("github.com") && url.contains("/blob/")) {
            return url.replace("https://github.com/", "https://raw.githubusercontent.com/")
                      .replace("/blob/", "/");
        }
        return url;
    }

    @Override
    public List<FetchedDocument> fetch(String url, Map<String, Object> config) {
        String resolvedUrl = normalizeUrl(url);
        if (!resolvedUrl.equals(url)) {
            log.info("GitHub blob URL normalized to raw URL: {}", resolvedUrl);
        }
        log.info("Fetching PDF from URL: {}", resolvedUrl);
        try {
            byte[] pdfBytes = webClientBuilder.build()
                    .get()
                    .uri(resolvedUrl)
                    .header("User-Agent", "RAGPlatformBot/1.0")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("Empty PDF response from URL: {}", url);
                return List.of();
            }

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                String title = document.getDocumentInformation().getTitle();
                if (title == null || title.isBlank()) {
                    title = url.substring(url.lastIndexOf('/') + 1);
                }

                Map<String, Object> metadata = Map.of(
                        "url", url,
                        "source_type", "PDF_URL",
                        "page_count", document.getNumberOfPages()
                );

                return List.of(new FetchedDocument(url, title, text.trim(), metadata));
            }
        } catch (Exception e) {
            log.error("Failed to fetch/parse PDF from URL: {}", url, e);
            throw new RuntimeException("Failed to fetch PDF: " + url, e);
        }
    }
}
