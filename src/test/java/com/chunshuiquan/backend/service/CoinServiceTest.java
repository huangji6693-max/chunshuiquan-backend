package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.entity.CoinTransaction;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.CoinTransactionRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @InjectMocks
    private CoinService coinService;

    private UUID userId;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        profile = new Profile();
        profile.setId(userId);
        profile.setCoins(100);
    }

    @Test
    @DisplayName("getPackages - 返回充值包列表")
    void getPackages_returnsAllPackages() {
        var packages = coinService.getPackages();

        assertEquals(4, packages.size());
        assertEquals(60, packages.get("small"));
        assertEquals(300, packages.get("medium"));
        assertEquals(980, packages.get("large"));
        assertEquals(2000, packages.get("mega"));
    }

    @Test
    @DisplayName("recordSpend - 记录消费流水")
    void recordSpend_savesTransaction() {
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        coinService.recordSpend(userId, 10, 90, "gift_sent", "送出礼物: 玫瑰");

        ArgumentCaptor<CoinTransaction> captor = ArgumentCaptor.forClass(CoinTransaction.class);
        verify(coinTransactionRepository).save(captor.capture());

        CoinTransaction saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(-10, saved.getAmount()); // 消费为负数
        assertEquals(90, saved.getBalanceAfter());
        assertEquals("gift_sent", saved.getType());
        assertEquals("送出礼物: 玫瑰", saved.getNote());
    }

    @Test
    @DisplayName("getTransactions - 返回交易列表")
    void getTransactions_returnsList() {
        CoinTransaction tx1 = new CoinTransaction();
        tx1.setUserId(userId);
        tx1.setAmount(60);
        tx1.setType("recharge");

        CoinTransaction tx2 = new CoinTransaction();
        tx2.setUserId(userId);
        tx2.setAmount(-10);
        tx2.setType("gift_sent");

        when(coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(tx1, tx2));

        List<CoinTransaction> result = coinService.getTransactions(userId);

        assertEquals(2, result.size());
        assertEquals("recharge", result.get(0).getType());
        assertEquals("gift_sent", result.get(1).getType());
        verify(coinTransactionRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("recharge - 充值成功增加金币")
    void recharge_success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CoinTransaction result = coinService.recharge(userId, "small", "receipt_123", "ios");

        assertEquals(160, profile.getCoins()); // 100 + 60
        verify(profileRepository).save(profile);
        assertEquals(60, result.getAmount());
        assertEquals("recharge", result.getType());
    }

    @Test
    @DisplayName("recharge - 无效充值包抛异常")
    void recharge_invalidPackage_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> coinService.recharge(userId, "invalid_pkg", "receipt", "ios"));

        assertTrue(ex.getMessage().contains("无效的充值包"));
        verify(profileRepository, never()).save(any());
    }
}
