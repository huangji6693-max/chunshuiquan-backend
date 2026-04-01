package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    Page<Message> findByMatchIdOrderByCreatedAtDesc(UUID matchId, Pageable pageable);

    long countByMatchId(UUID matchId);

    long countByMatchIdAndSenderIdNotAndIsReadFalse(UUID matchId, UUID senderId);

    // 查询某个 match 的最后一条消息
    Optional<Message> findTopByMatchIdOrderByCreatedAtDesc(UUID matchId);

    void deleteByMatchIdIn(Collection<UUID> matchIds);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.matchId = :matchId AND m.senderId != :readerId AND m.isRead = false")
    int markAsRead(@Param("matchId") UUID matchId, @Param("readerId") UUID readerId);
}
