package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.CoinBalanceDto;
import com.chunshuiquan.backend.dto.RechargeRequest;
import com.chunshuiquan.backend.entity.CoinTransaction;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.service.CoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/coins")
public class CoinController {

    private final CoinService coinService;
    private final ProfileRepository profileRepository;

    public CoinController(CoinService coinService, ProfileRepository profileRepository) {
        this.coinService = coinService;
        this.profileRepository = profileRepository;
    }

    /** GET /api/coins/balance — 查询金币余额 */
    @GetMapping("/balance")
    public ResponseEntity<?> balance(@AuthenticationPrincipal String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(p -> ResponseEntity.ok(new CoinBalanceDto(p.getCoins())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/coins/packages — 充值包列表 */
    @GetMapping("/packages")
    public ResponseEntity<Map<String, Integer>> packages() {
        return ResponseEntity.ok(coinService.getPackages());
    }

    /** POST /api/coins/recharge — 充值 */
    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@AuthenticationPrincipal String userId,
                                      @RequestBody RechargeRequest request) {
        try {
            CoinTransaction tx = coinService.recharge(
                    UUID.fromString(userId),
                    request.getPackageId(),
                    request.getReceipt(),
                    request.getPlatform());
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/coins/transactions — 金币流水 */
    @GetMapping("/transactions")
    public ResponseEntity<List<CoinTransaction>> transactions(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(coinService.getTransactions(UUID.fromString(userId)));
    }
}
