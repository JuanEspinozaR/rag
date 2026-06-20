package com.ragplatform.application.port;

import com.ragplatform.application.dto.request.KnowledgeSourceRequest;
import com.ragplatform.application.dto.response.DocumentResponse;
import com.ragplatform.application.dto.response.KnowledgeSourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface KnowledgeSourceUseCase {

    KnowledgeSourceResponse create(KnowledgeSourceRequest request);

    KnowledgeSourceResponse findById(UUID id);

    Page<KnowledgeSourceResponse> findAll(Pageable pageable);

    KnowledgeSourceResponse update(UUID id, KnowledgeSourceRequest request);

    void delete(UUID id);

    void sync(UUID id);

    Page<DocumentResponse> findDocuments(UUID knowledgeSourceId, Pageable pageable);
}
