package com.ragplatform.infrastructure.connector;

import com.ragplatform.domain.enums.SourceType;

import java.util.List;

/**
 * Pluggable connector interface for fetching documents from external sources.
 * Implement this interface to add support for new data source types.
 */
public interface DataSourceConnector {

    /**
     * Returns the source type this connector handles.
     */
    SourceType supportedType();

    /**
     * Fetches raw document content from the source URL.
     *
     * @param url    the source URL or content string
     * @param config optional additional configuration map (from KnowledgeSource.config)
     * @return list of fetched documents with title and content
     */
    List<FetchedDocument> fetch(String url, java.util.Map<String, Object> config);

    record FetchedDocument(
            String externalId,
            String title,
            String content,
            java.util.Map<String, Object> metadata
    ) {}
}
