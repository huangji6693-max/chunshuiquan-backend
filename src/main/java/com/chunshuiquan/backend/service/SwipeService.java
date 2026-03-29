package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.SwipeResult;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.Swipe;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.SwipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final ProfileRepository profileRepository;
    private final PushNotificationService pushNotificationService;

    public SwipeService(SwipeRepository swipeRepository,
                        MatchRepository matchRepository,
                        ProfileRepository profileRepository,
                        PushNotificationService pushNotificationService) {
        this.swipeRepository = swipeRepository;
        this.matchRepository = matchRepository;
        this.profileRepository = profileRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional
    public SwipeResult swipe(UUID swiperId, UUID swipedId, String direction) {
        // 幂等：已经滑过就忽略
        if (swipeRepository.existsBySwiperIdAndSwipedId(swiperId, swipedId)) {
            return SwipeResult.noMatch();
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

                Match match;
                Optional<Match> existing = matchRepository.findByUser1IdAndUser2Id(u1, u2);
                if (existing.isEmpty()) {
                    match = new Match();
                    match.setUser1Id(u1);
                    match.setUser2Id(u2);
                    match = matchRepository.save(match);
                    final Match savedMatch = match;

                    // 通知 swipedId：有新配对
                    Optional<Profile> swiper = profileRepository.findById(swiperId);
                    String swiperName = swiper.map(Profile::getName).orElse("Ta");
                    Optional<Profile> partner = profileRepository.findById(swipedId);
                    String partnerName = partner.map(Profile::getName).orElse("Ta");
                    String partnerAvatar = partner
                            .filter(p -> p.getAvatarUrls() != null && p.getAvatarUrls().length > 0)
                            .map(p -> p.getAvatarUrls()[0])
                            .orElse(null);
                    partner.ifPresent(p -> pushNotificationService.sendNewMatchNotification(
                            p.getFcmToken(), swiperName, savedMatch.getId().toString()));
                    return new SwipeResult(true, savedMatch.getId(), partnerName, partnerAvatar);
                } else {
                    match = existing.get();
                }

                // 返回对方信息
                Optional<Profile> partner = profileRepository.findById(swipedId);
                String partnerName = partner.map(Profile::getName).orElse("Ta");
                String partnerAvatar = partner
                        .filter(p -> p.getAvatarUrls() != null && p.getAvatarUrls().length > 0)
                        .map(p -> p.getAvatarUrls()[0])
                        .orElse(null);

                return new SwipeResult(true, match.getId(), partnerName, partnerAvatar);
            }
        }
        return SwipeResult.noMatch();
    }
}
