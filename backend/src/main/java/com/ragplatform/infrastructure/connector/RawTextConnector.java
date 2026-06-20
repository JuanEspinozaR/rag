package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RawTextConnector implements DataSourceConnector {

    @Override
    public SourceType supportedType() {
        return SourceType.RAW_TEXT;
    }

    @Override
    public List<FetchedDocument> fetch(String content, Map<String, Object> config) {
        if (content == null || content.isBlank()) {
            log.warn("Empty raw text content provided");
            return List.of();
        }

        String title = config != null && config.containsKey("title")
                ? config.get("title").toString()
                : "Raw Text Document";

        Map<String, Object> metadata = Map.of("source_type", "RAW_TEXT");

        return List.of(new FetchedDocument(
                "raw-text-" + System.currentTimeMillis(),
                title,
                content,
                metadata
        ));
    }
}
