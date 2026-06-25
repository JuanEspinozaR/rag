package com.ragplatform.infrastructure.rag;

import com.ragplatform.domain.model.KnowledgeSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Indirection component that lets KnowledgeSourceService trigger an async sync
 * without self-invocation, so Spring's @Async proxy is correctly applied.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSourceSyncTrigger {

    private final DocumentSyncService documentSyncService;

    @Async("syncExecutor")
    public void trigger(KnowledgeSource source) {
        log.info("Auto-syncing knowledge source: {} ({})", source.getName(), source.getId());
        documentSyncService.syncKnowledgeSource(source);
    }
}
