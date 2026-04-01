package com.chunshuiquan.backend.dto;

import lombok.Data;

import java.util.UUID;

/**
 * 送礼物请求
 */
@Data
public class SendGiftRequest {
    private UUID matchId;
    private Long giftId;
}
