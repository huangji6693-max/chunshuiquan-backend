package com.chunshuiquan.backend.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 礼物记录返回DTO（包含礼物详情和对方信息）
 */
@Data
public class GiftRecordDto {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private UUID receiverId;
    private String receiverName;
    private Long giftId;
    private String giftName;
    private String giftIcon;
    private Integer giftCoins;
    private UUID matchId;
    private OffsetDateTime createdAt;
}
