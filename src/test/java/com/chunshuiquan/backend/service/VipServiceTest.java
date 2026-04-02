package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.VipStatusDto;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.VipOrder;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.VipOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VipServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private VipOrderRepository vipOrderRepository;

    @InjectMocks
    private VipService vipService;

    private UUID userId;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = new Profile();
        profile.setId(userId);
        profile.setCoins(100);
        profile.setVipTier("none");
        profile.setVipExpiresAt(null);
    }

    // ========== getStatus 测试 ==========

    @Test
    @DisplayName("getStatus - VIP有效期内返回active状态")
    void getStatus_activeVip_returnsActive() {
        profile.setVipTier("gold");
        profile.setVipExpiresAt(OffsetDateTime.now().plusDays(15));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        VipStatusDto status = vipService.getStatus(userId);

        assertEquals("gold", status.getTier());
        assertTrue(status.getIsActive());
        assertTrue(status.getDaysLeft() > 0);
    }

    @Test
    @DisplayName("getStatus - VIP已过期自动降级为none")
    void getStatus_expiredVip_downgradeToNone() {
        profile.setVipTier("gold");
        profile.setVipExpiresAt(OffsetDateTime.now().minusDays(1));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        VipStatusDto status = vipService.getStatus(userId);

        assertEquals("none", status.getTier());
        assertFalse(status.getIsActive());
        assertEquals(0, status.getDaysLeft());
        // 验证降级后保存了profile
        verify(profileRepository).save(profile);
    }

    @Test
    @DisplayName("getStatus - 从未订阅VIP返回none")
    void getStatus_noVip_returnsNone() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        VipStatusDto status = vipService.getStatus(userId);

        assertEquals("none", status.getTier());
        assertFalse(status.getIsActive());
        assertEquals(0, status.getDaysLeft());
    }

    @Test
    @DisplayName("getStatus - 用户不存在抛异常")
    void getStatus_userNotFound_throwsException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> vipService.getStatus(userId));
    }

    // ========== subscribe 测试 ==========

    @Test
    @DisplayName("subscribe - 新订阅gold_monthly设置正确的30天过期时间")
    void subscribe_newGoldMonthly_setsCorrectExpiry() {
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime before = OffsetDateTime.now();
        VipStatusDto result = vipService.subscribe(userId, "gold_monthly", "receipt123", "apple");
        OffsetDateTime after = OffsetDateTime.now();

        assertEquals("gold", result.getTier());
        assertTrue(result.getIsActive());
        // 过期时间应在 now+30天 附近
        assertTrue(result.getExpiresAt().isAfter(before.plusDays(29)));
        assertTrue(result.getExpiresAt().isBefore(after.plusDays(31)));
    }

    @Test
    @DisplayName("subscribe - 续期在现有过期时间基础上叠加")
    void subscribe_renewal_extendsFromCurrentExpiry() {
        OffsetDateTime existingExpiry = OffsetDateTime.now().plusDays(10);
        profile.setVipTier("gold");
        profile.setVipExpiresAt(existingExpiry);
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        VipStatusDto result = vipService.subscribe(userId, "gold_monthly", "receipt456", "apple");

        // 新过期时间 = 现有过期时间(+10天) + 30天 = 约+40天
        assertTrue(result.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(39)));
        assertTrue(result.getDaysLeft() >= 39);
    }

    @Test
    @DisplayName("subscribe - gold升diamond从当前时间开始计算")
    void subscribe_goldToDiamond_startsFromNow() {
        OffsetDateTime existingExpiry = OffsetDateTime.now().plusDays(20);
        profile.setVipTier("gold");
        profile.setVipExpiresAt(existingExpiry);
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime before = OffsetDateTime.now();
        VipStatusDto result = vipService.subscribe(userId, "diamond_monthly", "receipt789", "apple");

        assertEquals("diamond", result.getTier());
        // 升级后从当前时间算起，不是从existingExpiry叠加
        // 新过期时间 = now + 30天，而不是 existingExpiry + 30天
        assertTrue(result.getExpiresAt().isBefore(before.plusDays(31)));
        assertTrue(result.getExpiresAt().isAfter(before.plusDays(29)));
    }

    @Test
    @DisplayName("subscribe - 无效planId抛异常")
    void subscribe_invalidPlanId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> vipService.subscribe(userId, "invalid_plan", "receipt", "apple"));
    }

    @Test
    @DisplayName("subscribe - diamond订阅赠送200金币")
    void subscribe_diamond_grants200Coins() {
        profile.setCoins(50);
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        vipService.subscribe(userId, "diamond_monthly", "receipt", "apple");

        // profile.save 被调用两次（设置VIP一次，加金币一次）
        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository, atLeast(2)).save(captor.capture());
        // 最后一次save时金币应为 50 + 200 = 250
        Profile lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(250, lastSaved.getCoins());
    }

    @Test
    @DisplayName("subscribe - gold订阅赠送100金币")
    void subscribe_gold_grants100Coins() {
        profile.setCoins(50);
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        vipService.subscribe(userId, "gold_monthly", "receipt", "apple");

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository, atLeast(2)).save(captor.capture());
        Profile lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(150, lastSaved.getCoins());
    }

    @Test
    @DisplayName("subscribe - 保存VipOrder记录包含正确信息")
    void subscribe_savesVipOrder_withCorrectData() {
        when(profileRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(vipOrderRepository.save(any(VipOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        vipService.subscribe(userId, "gold_quarterly", "receipt_abc", "google");

        ArgumentCaptor<VipOrder> captor = ArgumentCaptor.forClass(VipOrder.class);
        verify(vipOrderRepository).save(captor.capture());
        VipOrder order = captor.getValue();
        assertEquals(userId, order.getUserId());
        assertEquals("gold", order.getTier());
        assertEquals(90, order.getDays());
        assertEquals(6800, order.getAmount());
        assertEquals("google", order.getPlatform());
        assertEquals("receipt_abc", order.getExternalOrderId());
    }
}
