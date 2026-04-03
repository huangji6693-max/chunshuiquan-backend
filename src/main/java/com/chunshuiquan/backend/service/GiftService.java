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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GiftService {

    private final GiftRepository giftRepository;
    private final GiftRecordRepository giftRecordRepository;
    private final ProfileRepository profileRepository;
    private final MatchRepository matchRepository;
    private final CoinService coinService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    public GiftService(GiftRepository giftRepository,
                       GiftRecordRepository giftRecordRepository,
                       ProfileRepository profileRepository,
                       MatchRepository matchRepository,
                       CoinService coinService,
                       SimpMessagingTemplate messagingTemplate,
                       PushNotificationService pushNotificationService) {
        this.giftRepository = giftRepository;
        this.giftRecordRepository = giftRecordRepository;
        this.profileRepository = profileRepository;
        this.matchRepository = matchRepository;
        this.coinService = coinService;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
    }

    /** 获取所有上架礼物（缓存30分钟） */
    public List<Gift> listGifts() {
        return giftRepository.findByIsActiveTrueOrderByCoinsAsc();
    }

    /** 送礼物 */
    @Transactional
    public GiftRecordDto sendGift(UUID senderId, UUID matchId, Long giftId) {
        // 1. 校验礼物
        Gift gift = giftRepository.findById(giftId)
                .orElseThrow(() -> new IllegalArgumentException("礼物不存在"));
        if (!Boolean.TRUE.equals(gift.getIsActive())) {
            throw new IllegalArgumentException("该礼物已下架");
        }

        // 2. 校验匹配关系，确定收礼者
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("匹配不存在"));
        if (!Boolean.TRUE.equals(match.getIsActive())) {
            throw new IllegalArgumentException("匹配已解除");
        }
        UUID receiverId;
        if (match.getUser1Id().equals(senderId)) {
            receiverId = match.getUser2Id();
        } else if (match.getUser2Id().equals(senderId)) {
            receiverId = match.getUser1Id();
        } else {
            throw new IllegalArgumentException("你不在该匹配中");
        }

        // 3. 扣金币
        Profile sender = profileRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (sender.getCoins() < gift.getCoins()) {
            throw new IllegalArgumentException("金币不足，需要 " + gift.getCoins() + " 个金币");
        }
        sender.setCoins(sender.getCoins() - gift.getCoins());
        profileRepository.save(sender);

        // 记录金币消费流水
        coinService.recordSpend(senderId, gift.getCoins(), sender.getCoins(),
                "gift_sent", "送出礼物: " + gift.getName());

        // 4. 保存礼物记录
        GiftRecord record = new GiftRecord();
        record.setSenderId(senderId);
        record.setReceiverId(receiverId);
        record.setGiftId(giftId);
        record.setMatchId(matchId);
        record = giftRecordRepository.save(record);

        // 5. 组装返回
        Profile receiver = profileRepository.findById(receiverId).orElse(null);
        GiftRecordDto dto = toDto(record, gift, sender, receiver);

        // 6. WebSocket 实时推送给对方（/topic/user/{receiverId}/gifts）
        messagingTemplate.convertAndSend("/topic/user/" + receiverId + "/gifts",
                java.util.Map.of(
                        "type", "gift_received",
                        "giftName", gift.getName(),
                        "giftIcon", gift.getIcon(),
                        "giftCoins", gift.getCoins(),
                        "senderName", sender.getName(),
                        "senderId", senderId.toString(),
                        "matchId", matchId.toString()
                ));

        // 7. FCM 离线推送
        if (receiver != null && receiver.getFcmToken() != null) {
            pushNotificationService.sendGiftNotification(
                    receiver.getFcmToken(), sender.getName(), gift.getName(), matchId.toString());
        }

        return dto;
    }

    /** 查询我收到的礼物 */
    public List<GiftRecordDto> receivedGifts(UUID userId) {
        return giftRecordRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    /** 查询我送出的礼物 */
    public List<GiftRecordDto> sentGifts(UUID userId) {
        return giftRecordRepository.findBySenderIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    // ========== 私有方法 ==========

    private GiftRecordDto toDto(GiftRecord record) {
        Gift gift = giftRepository.findById(record.getGiftId()).orElse(null);
        Profile sender = profileRepository.findById(record.getSenderId()).orElse(null);
        Profile receiver = profileRepository.findById(record.getReceiverId()).orElse(null);
        return toDto(record, gift, sender, receiver);
    }

    private GiftRecordDto toDto(GiftRecord record, Gift gift, Profile sender, Profile receiver) {
        GiftRecordDto dto = new GiftRecordDto();
        dto.setId(record.getId());
        dto.setSenderId(record.getSenderId());
        dto.setSenderName(sender != null ? sender.getName() : null);
        dto.setReceiverId(record.getReceiverId());
        dto.setReceiverName(receiver != null ? receiver.getName() : null);
        dto.setGiftId(record.getGiftId());
        if (gift != null) {
            dto.setGiftName(gift.getName());
            dto.setGiftIcon(gift.getIcon());
            dto.setGiftCoins(gift.getCoins());
        }
        dto.setMatchId(record.getMatchId());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }
}
