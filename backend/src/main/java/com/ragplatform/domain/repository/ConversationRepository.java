package com.ragplatform.domain.repository;

import com.ragplatform.domain.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
