package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.GiftRecordDto;
import com.chunshuiquan.backend.dto.SendGiftRequest;
import com.chunshuiquan.backend.entity.Gift;
import com.chunshuiquan.backend.service.GiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gifts")
public class GiftController {

    private final GiftService giftService;

    public GiftController(GiftService giftService) {
        this.giftService = giftService;
    }

    /** GET /api/gifts — 获取所有上架礼物列表 */
    @GetMapping
    public ResponseEntity<List<Gift>> listGifts() {
        return ResponseEntity.ok(giftService.listGifts());
    }

    /** POST /api/gifts/send — 送礼物（扣金币） */
    @PostMapping("/send")
    public ResponseEntity<?> sendGift(@AuthenticationPrincipal String userId,
                                      @RequestBody SendGiftRequest request) {
        try {
            GiftRecordDto result = giftService.sendGift(
                    UUID.fromString(userId), request.getMatchId(), request.getGiftId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/gifts/received — 我收到的礼物 */
    @GetMapping("/received")
    public ResponseEntity<List<GiftRecordDto>> receivedGifts(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(giftService.receivedGifts(UUID.fromString(userId)));
    }

    /** GET /api/gifts/sent — 我送出的礼物 */
    @GetMapping("/sent")
    public ResponseEntity<List<GiftRecordDto>> sentGifts(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(giftService.sentGifts(UUID.fromString(userId)));
    }
}
