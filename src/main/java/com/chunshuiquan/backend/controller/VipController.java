package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.VipStatusDto;
import com.chunshuiquan.backend.dto.VipSubscribeRequest;
import com.chunshuiquan.backend.service.VipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vip")
public class VipController {

    private final VipService vipService;

    public VipController(VipService vipService) {
        this.vipService = vipService;
    }

    /** GET /api/vip/status — 查询VIP状态 */
    @GetMapping("/status")
    public ResponseEntity<VipStatusDto> status(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(vipService.getStatus(UUID.fromString(userId)));
    }

    /** GET /api/vip/plans — 套餐列表 */
    @GetMapping("/plans")
    public ResponseEntity<Map<String, int[]>> plans() {
        return ResponseEntity.ok(vipService.getPlans());
    }

    /** POST /api/vip/subscribe — 订阅VIP */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@AuthenticationPrincipal String userId,
                                       @RequestBody VipSubscribeRequest request) {
        try {
            VipStatusDto result = vipService.subscribe(
                    UUID.fromString(userId),
                    request.getPlanId(),
                    request.getReceipt(),
                    request.getPlatform());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
