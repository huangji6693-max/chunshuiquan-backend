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
}
