package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    boolean existsByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    // 查询某用户的所有有效匹配
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :uid OR m.user2Id = :uid) AND m.isActive = true ORDER BY m.createdAt DESC")
    List<Match> findActiveMatchesByUserId(@Param("uid") UUID uid);
}
