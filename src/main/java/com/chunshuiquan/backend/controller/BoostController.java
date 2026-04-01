package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.service.CoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/boost")
public class BoostController {

    private static final int BOOST_COST = 100; // 100金币 = 30分钟曝光加速
    private static final int BOOST_MINUTES = 30;

    private final ProfileRepository profileRepository;
    private final CoinService coinService;

    public BoostController(ProfileRepository profileRepository, CoinService coinService) {
        this.profileRepository = profileRepository;
        this.coinService = coinService;
    }

    /** GET /api/boost/status — 查询当前Boost状态 */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal String userId) {
        Profile profile = profileRepository.findById(UUID.fromString(userId)).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        OffsetDateTime boostUntil = profile.getBoostUntil();
        boolean active = boostUntil != null && boostUntil.isAfter(OffsetDateTime.now());
        long minutesLeft = 0;
        if (active) {
            minutesLeft = java.time.Duration.between(OffsetDateTime.now(), boostUntil).toMinutes();
        }

        return ResponseEntity.ok(Map.of(
                "active", active,
                "minutesLeft", minutesLeft,
                "cost", BOOST_COST
        ));
    }

    /** POST /api/boost — 激活Boost（扣金币） */
    @PostMapping
    @Transactional
    public ResponseEntity<?> activate(@AuthenticationPrincipal String userId) {
        UUID myId = UUID.fromString(userId);
        Profile profile = profileRepository.findById(myId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        // VIP免费检查
        boolean isVip = !"none".equals(profile.getVipTier()) &&
                profile.getVipExpiresAt() != null &&
                profile.getVipExpiresAt().isAfter(OffsetDateTime.now());

        if (!isVip) {
            if (profile.getCoins() < BOOST_COST) {
                return ResponseEntity.badRequest().body(Map.of("error", "金币不足，需要" + BOOST_COST + "金币"));
            }
            profile.setCoins(profile.getCoins() - BOOST_COST);
            coinService.recordSpend(myId, BOOST_COST, profile.getCoins(), "boost", "曝光加速30分钟");
        }

        // 如果当前有活跃Boost，在此基础上续期
        OffsetDateTime base = profile.getBoostUntil() != null &&
                profile.getBoostUntil().isAfter(OffsetDateTime.now())
                ? profile.getBoostUntil() : OffsetDateTime.now();
        profile.setBoostUntil(base.plusMinutes(BOOST_MINUTES));
        profileRepository.save(profile);

        return ResponseEntity.ok(Map.of(
                "active", true,
                "minutesLeft", BOOST_MINUTES,
                "message", isVip ? "VIP免费曝光加速已激活！" : "曝光加速已激活！"
        ));
    }
}
