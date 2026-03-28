package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Swipe;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.SwipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;

    public SwipeService(SwipeRepository swipeRepository, MatchRepository matchRepository) {
        this.swipeRepository = swipeRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public boolean swipe(UUID swiperId, UUID swipedId, String direction) {
        // 幂等：已经滑过就忽略
        if (swipeRepository.existsBySwiperIdAndSwipedId(swiperId, swipedId)) {
            return false;
        }

        Swipe swipe = new Swipe();
        swipe.setSwiperId(swiperId);
        swipe.setSwipedId(swipedId);
        swipe.setDirection(direction);
        swipeRepository.save(swipe);

        // 只有 like/superlike 才检查是否匹配
        if (!"nope".equals(direction)) {
            boolean mutualLike = swipeRepository.existsBySwipedIdAndSwiperIdAndDirectionIn(
                    swiperId, swipedId, List.of("like", "superlike"));
            if (mutualLike) {
                UUID u1 = swiperId.compareTo(swipedId) < 0 ? swiperId : swipedId;
                UUID u2 = swiperId.compareTo(swipedId) < 0 ? swipedId : swiperId;
                if (!matchRepository.existsByUser1IdAndUser2Id(u1, u2)) {
                    Match match = new Match();
                    match.setUser1Id(u1);
                    match.setUser2Id(u2);
                    matchRepository.save(match);
                }
                return true;
            }
        }
        return false;
    }
}
