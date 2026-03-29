package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    Page<Message> findByMatchIdOrderByCreatedAtDesc(UUID matchId, Pageable pageable);

    long countByMatchId(UUID matchId);

    long countByMatchIdAndSenderIdNotAndIsReadFalse(UUID matchId, UUID senderId);

    void deleteByMatchIdIn(Collection<UUID> matchIds);
}
