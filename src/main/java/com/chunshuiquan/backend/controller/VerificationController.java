package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.Verification;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.VerificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationRepository verificationRepository;
    private final ProfileRepository profileRepository;

    public VerificationController(VerificationRepository verificationRepository,
                                  ProfileRepository profileRepository) {
        this.verificationRepository = verificationRepository;
        this.profileRepository = profileRepository;
    }

    /** GET /api/verification/status — 查询认证状态 */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal String userId) {
        UUID myId = UUID.fromString(userId);
        Profile profile = profileRepository.findById(myId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        var latest = verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(myId);
        if (latest.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "verified", false,
                    "status", "none",
                    "message", "尚未提交认证"
            ));
        }

        Verification v = latest.get();
        return ResponseEntity.ok(Map.of(
                "verified", "approved".equals(v.getStatus()),
                "status", v.getStatus(),
                "realName", v.getRealName(),
                "rejectReason", v.getRejectReason() != null ? v.getRejectReason() : "",
                "createdAt", v.getCreatedAt().toString()
        ));
    }

    /** POST /api/verification/submit — 提交认证 */
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@AuthenticationPrincipal String userId,
                                    @RequestBody Map<String, String> body) {
        UUID myId = UUID.fromString(userId);
        String realName = body.get("realName");
        String idPhotoUrl = body.get("idPhotoUrl");
        String selfieUrl = body.get("selfieUrl");

        if (realName == null || idPhotoUrl == null || selfieUrl == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请填写完整信息"));
        }

        // 检查是否有待审核的申请
        var latest = verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(myId);
        if (latest.isPresent() && "pending".equals(latest.get().getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "您有待审核的认证申请"));
        }

        Verification v = new Verification();
        v.setUserId(myId);
        v.setRealName(realName);
        v.setIdPhotoUrl(idPhotoUrl);
        v.setSelfieUrl(selfieUrl);
        verificationRepository.save(v);

        return ResponseEntity.ok(Map.of("message", "认证已提交，请等待审核", "status", "pending"));
    }

    /** GET /api/admin/verifications — 管理员查看待审核列表 */
    @GetMapping("/admin/pending")
    public ResponseEntity<List<Verification>> pendingList() {
        return ResponseEntity.ok(verificationRepository.findByStatusOrderByCreatedAtAsc("pending"));
    }

    /** PUT /api/admin/verifications/{id}/review — 管理员审核 */
    @PutMapping("/admin/{id}/review")
    public ResponseEntity<?> review(@PathVariable UUID id,
                                    @RequestBody Map<String, String> body) {
        String action = body.get("action"); // approve / reject
        String reason = body.get("reason");

        return verificationRepository.findById(id)
                .map(v -> {
                    if ("approve".equals(action)) {
                        v.setStatus("approved");
                        // 给用户加认证标记
                        profileRepository.findById(v.getUserId()).ifPresent(p -> {
                            p.setIsActive(true); // 确保账号活跃
                            profileRepository.save(p);
                        });
                    } else if ("reject".equals(action)) {
                        v.setStatus("rejected");
                        v.setRejectReason(reason);
                    } else {
                        return ResponseEntity.badRequest().body((Object) Map.of("error", "action 必须为 approve/reject"));
                    }
                    v.setReviewedAt(OffsetDateTime.now());
                    verificationRepository.save(v);
                    return ResponseEntity.ok((Object) v);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
