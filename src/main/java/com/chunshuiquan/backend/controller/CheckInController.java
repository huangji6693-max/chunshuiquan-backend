package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.CheckInResultDto;
import com.chunshuiquan.backend.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /** GET /api/checkin/status — 查询签到状态 */
    @GetMapping("/status")
    public ResponseEntity<CheckInResultDto> status(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(checkInService.getStatus(UUID.fromString(userId)));
    }

    /** POST /api/checkin — 执行签到 */
    @PostMapping
    public ResponseEntity<?> checkIn(@AuthenticationPrincipal String userId) {
        try {
            CheckInResultDto result = checkInService.checkIn(UUID.fromString(userId));
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
