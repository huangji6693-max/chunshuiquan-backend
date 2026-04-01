package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.VipStatusDto;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.VipOrder;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.VipOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class VipService {

    /** 套餐定义: planId -> { tier, days, amount(分) } */
    private static final Map<String, int[]> PLANS = Map.of(
            "gold_monthly",    new int[]{30, 2800},     // ¥28/月
            "gold_quarterly",  new int[]{90, 6800},     // ¥68/季 (省16)
            "gold_yearly",     new int[]{365, 19800},   // ¥198/年 (省138)
            "diamond_monthly", new int[]{30, 4800},     // ¥48/月
            "diamond_quarterly", new int[]{90, 11800},  // ¥118/季 (省26)
            "diamond_yearly",  new int[]{365, 36800}    // ¥368/年 (省208)
    );

    private final ProfileRepository profileRepository;
    private final VipOrderRepository vipOrderRepository;

    public VipService(ProfileRepository profileRepository,
                      VipOrderRepository vipOrderRepository) {
        this.profileRepository = profileRepository;
        this.vipOrderRepository = vipOrderRepository;
    }

    /** 获取VIP状态 */
    public VipStatusDto getStatus(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String tier = profile.getVipTier();
        OffsetDateTime expiresAt = profile.getVipExpiresAt();
        boolean active = !"none".equals(tier) && expiresAt != null && expiresAt.isAfter(OffsetDateTime.now());

        // 自动过期降级
        if (!active && !"none".equals(tier)) {
            profile.setVipTier("none");
            profile.setVipExpiresAt(null);
            profileRepository.save(profile);
            tier = "none";
            expiresAt = null;
        }

        int daysLeft = 0;
        if (active && expiresAt != null) {
            daysLeft = (int) ChronoUnit.DAYS.between(OffsetDateTime.now(), expiresAt);
        }

        return new VipStatusDto(tier, expiresAt, daysLeft, active);
    }

    /** 获取套餐列表 */
    public Map<String, int[]> getPlans() {
        return PLANS;
    }

    /** 订阅VIP */
    @Transactional
    public VipStatusDto subscribe(UUID userId, String planId, String receipt, String platform) {
        int[] plan = PLANS.get(planId);
        if (plan == null) {
            throw new IllegalArgumentException("无效的套餐: " + planId);
        }

        String tier = planId.startsWith("diamond") ? "diamond" : "gold";
        int days = plan[0];
        int amount = plan[1];

        // TODO: 根据 platform 校验 receipt

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 如果当前已有VIP且未过期，在现有基础上续期
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime currentExpiry = profile.getVipExpiresAt();
        OffsetDateTime baseTime = (currentExpiry != null && currentExpiry.isAfter(now))
                ? currentExpiry : now;

        // 如果升级(gold->diamond)，从当前时间算起
        if ("diamond".equals(tier) && "gold".equals(profile.getVipTier())) {
            baseTime = now;
        }

        OffsetDateTime newExpiry = baseTime.plusDays(days);
        profile.setVipTier(tier);
        profile.setVipExpiresAt(newExpiry);
        profileRepository.save(profile);

        // 保存订单
        VipOrder order = new VipOrder();
        order.setUserId(userId);
        order.setTier(tier);
        order.setDays(days);
        order.setAmount(amount);
        order.setPlatform(platform);
        order.setExternalOrderId(receipt);
        vipOrderRepository.save(order);

        // 赠送金币（VIP福利）
        int bonusCoins = "diamond".equals(tier) ? 200 : 100;
        profile.setCoins(profile.getCoins() + bonusCoins);
        profileRepository.save(profile);

        int daysLeft = (int) ChronoUnit.DAYS.between(now, newExpiry);
        return new VipStatusDto(tier, newExpiry, daysLeft, true);
    }
}
