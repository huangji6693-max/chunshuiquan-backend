package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    boolean existsByEmail(String email);

    // 推荐列表：排除自己、已滑过的、已封号的、双向屏蔽
    @Query("""
        SELECT p FROM Profile p
        WHERE p.id != :myId
          AND p.isActive = true
          AND p.id NOT IN (
              SELECT s.swipedId FROM Swipe s WHERE s.swiperId = :myId
          )
          AND p.id NOT IN (
              SELECT bu.blockedId FROM BlockedUser bu WHERE bu.blockerId = :myId
          )
          AND p.id NOT IN (
              SELECT bu.blockerId FROM BlockedUser bu WHERE bu.blockedId = :myId
          )
        ORDER BY p.lastActive DESC
        """)
    List<Profile> findFeed(@Param("myId") UUID myId, Pageable pageable);
}
