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

    /** 批量查询每个match的最后一条消息 */
    @Query(value = """
        SELECT DISTINCT ON (m.match_id) m.* FROM messages m
        WHERE m.match_id IN :matchIds
        ORDER BY m.match_id, m.created_at DESC
        """, nativeQuery = true)
    List<Message> findLastMessagesByMatchIds(@Param("matchIds") Collection<UUID> matchIds);

    /** 批量查询每个match的未读消息数（排除指定发送者） */
    @Query(value = """
        SELECT m.match_id AS matchId, COUNT(*) AS cnt
        FROM messages m
        WHERE m.match_id IN :matchIds
          AND m.sender_id != :excludeSenderId
          AND m.is_read = false
        GROUP BY m.match_id
        """, nativeQuery = true)
    List<Object[]> countUnreadByMatchIds(@Param("matchIds") Collection<UUID> matchIds,
                                         @Param("excludeSenderId") UUID excludeSenderId);

    /** 批量查询每个match的消息总数 */
    @Query(value = """
        SELECT m.match_id AS matchId, COUNT(*) AS cnt
        FROM messages m
        WHERE m.match_id IN :matchIds
        GROUP BY m.match_id
        """, nativeQuery = true)
    List<Object[]> countByMatchIds(@Param("matchIds") Collection<UUID> matchIds);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.matchId = :matchId AND m.senderId != :readerId AND m.isRead = false")
    int markAsRead(@Param("matchId") UUID matchId, @Param("readerId") UUID readerId);
}
