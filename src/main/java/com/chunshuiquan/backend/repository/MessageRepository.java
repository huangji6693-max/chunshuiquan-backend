package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    long countByMatchIdAndSenderIdNotAndIsReadFalse(UUID matchId, UUID senderId);
}
