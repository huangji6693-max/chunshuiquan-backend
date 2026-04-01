package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    // 检查对方是否 like/superlike 过自己（用于判断是否匹配）
    boolean existsBySwipedIdAndSwiperIdAndDirectionIn(UUID swipedId, UUID swiperId, List<String> directions);

    // 防止重复滑动
    boolean existsBySwiperIdAndSwipedId(UUID swiperId, UUID swipedId);

    void deleteBySwipedIdOrSwiperId(UUID swipedId, UUID swiperId);

    /** 查找所有右滑/SuperLike了我的人（我还没有滑过他们的） */
    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM Swipe s WHERE s.swipedId = :userId AND s.direction IN ('RIGHT','UP') " +
        "AND NOT EXISTS (SELECT 1 FROM Swipe s2 WHERE s2.swiperId = :userId AND s2.swipedId = s.swiperId)")
    List<Swipe> findWhoLikedMe(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
