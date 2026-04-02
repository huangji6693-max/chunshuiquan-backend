package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.GiftRecordDto;
import com.chunshuiquan.backend.entity.Gift;
import com.chunshuiquan.backend.entity.GiftRecord;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.GiftRecordRepository;
import com.chunshuiquan.backend.repository.GiftRepository;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {

    @Mock
    private GiftRepository giftRepository;

    @Mock
    private GiftRecordRepository giftRecordRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private CoinService coinService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private GiftService giftService;

    private UUID senderId;
    private UUID receiverId;
    private UUID matchId;
    private Gift gift;
    private Match match;
    private Profile sender;
    private Profile receiver;

    @BeforeEach
    void setUp() {
        senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        receiverId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        matchId = UUID.randomUUID();

        gift = new Gift();
        gift.setId(1L);
        gift.setName("玫瑰");
        gift.setIcon("🌹");
        gift.setCoins(10);
        gift.setIsActive(true);

        match = new Match();
        match.setId(matchId);
        match.setUser1Id(senderId);
        match.setUser2Id(receiverId);
        match.setIsActive(true);

        sender = new Profile();
        sender.setId(senderId);
        sender.setName("Alice");
        sender.setCoins(100);

        receiver = new Profile();
        receiver.setId(receiverId);
        receiver.setName("Bob");
        receiver.setFcmToken("bob_fcm_token");
    }

    @Test
    @DisplayName("listGifts - 返回上架礼物列表")
    void listGifts_returnsActiveGifts() {
        Gift gift2 = new Gift();
        gift2.setId(2L);
        gift2.setName("钻戒");
        gift2.setIcon("💍");
        gift2.setCoins(100);
        gift2.setIsActive(true);

        when(giftRepository.findByIsActiveTrueOrderByCoinsAsc()).thenReturn(List.of(gift, gift2));

        List<Gift> result = giftService.listGifts();

        assertEquals(2, result.size());
        assertEquals("玫瑰", result.get(0).getName());
        assertEquals("钻戒", result.get(1).getName());
        verify(giftRepository).findByIsActiveTrueOrderByCoinsAsc();
    }

    @Test
    @DisplayName("sendGift - 正常送礼：扣金币+记录+推送")
    void sendGift_success() {
        GiftRecord savedRecord = new GiftRecord();
        savedRecord.setId(UUID.randomUUID());
        savedRecord.setSenderId(senderId);
        savedRecord.setReceiverId(receiverId);
        savedRecord.setGiftId(1L);
        savedRecord.setMatchId(matchId);

        when(giftRepository.findById(1L)).thenReturn(Optional.of(gift));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(profileRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(giftRecordRepository.save(any(GiftRecord.class))).thenReturn(savedRecord);

        GiftRecordDto result = giftService.sendGift(senderId, matchId, 1L);

        assertNotNull(result);
        assertEquals(senderId, result.getSenderId());
        assertEquals(receiverId, result.getReceiverId());
        assertEquals("玫瑰", result.getGiftName());

        // 验证扣金币
        assertEquals(90, sender.getCoins());
        verify(profileRepository).save(sender);

        // 验证记录金币流水
        verify(coinService).recordSpend(eq(senderId), eq(10), eq(90), eq("gift_sent"), contains("玫瑰"));

        // 验证WebSocket推送
        verify(messagingTemplate).convertAndSend(eq("/topic/user/" + receiverId + "/gifts"), anyMap());

        // 验证FCM推送
        verify(pushNotificationService).sendGiftNotification(
                eq("bob_fcm_token"), eq("Alice"), eq("玫瑰"), eq(matchId.toString()));
    }

    @Test
    @DisplayName("sendGift - 金币不足抛异常")
    void sendGift_insufficientCoins_throwsException() {
        sender.setCoins(5); // 不够10金币

        when(giftRepository.findById(1L)).thenReturn(Optional.of(gift));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(profileRepository.findById(senderId)).thenReturn(Optional.of(sender));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> giftService.sendGift(senderId, matchId, 1L));

        assertTrue(ex.getMessage().contains("金币不足"));
        verify(giftRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendGift - 匹配不存在抛异常")
    void sendGift_matchNotFound_throwsException() {
        when(giftRepository.findById(1L)).thenReturn(Optional.of(gift));
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> giftService.sendGift(senderId, matchId, 1L));

        assertTrue(ex.getMessage().contains("匹配不存在"));
        verify(giftRecordRepository, never()).save(any());
    }
}
