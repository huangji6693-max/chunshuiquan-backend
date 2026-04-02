package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.service.PushNotificationService;
import com.chunshuiquan.backend.util.agora.RtcTokenBuilder2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/agora")
public class AgoraController {

    @Value("${agora.appId}")
    private String appId;

    @Value("${agora.appCertificate}")
    private String appCertificate;

    private final MatchRepository matchRepository;
    private final ProfileRepository profileRepository;
    private final PushNotificationService pushService;

    private static final int TOKEN_EXPIRE_SECONDS = 3600;
    private static final int PRIVILEGE_EXPIRE_SECONDS = 3600;

    public AgoraController(MatchRepository matchRepository,
                           ProfileRepository profileRepository,
                           PushNotificationService pushService) {
        this.matchRepository = matchRepository;
        this.profileRepository = profileRepository;
        this.pushService = pushService;
    }

    /**
     * GET /api/agora/token?channelName={matchId}
     * Requires valid JWT (Spring Security). uid 统一用 0，Agora 自动分配。
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(@RequestParam String channelName) throws Exception {
        String token = new RtcTokenBuilder2().buildTokenWithUid(
                appId, appCertificate,
                channelName,
                0,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                TOKEN_EXPIRE_SECONDS,
                PRIVILEGE_EXPIRE_SECONDS
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "appId", appId,
                "uid", 0,
                "channelName", channelName
        ));
    }

    /** POST /api/agora/invite — 发起语音通话邀请（推送通知给对方） */
    @PostMapping("/invite")
    public ResponseEntity<?> inviteCall(
            @AuthenticationPrincipal String userId,
            @RequestParam String matchId) {
        UUID myId = UUID.fromString(userId);
        UUID mId = UUID.fromString(matchId);

        Optional<Match> matchOpt = matchRepository.findById(mId);
        if (matchOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "匹配不存在"));
        }

        Match match = matchOpt.get();
        UUID otherId = match.getUser1Id().equals(myId) ? match.getUser2Id() : match.getUser1Id();

        Optional<Profile> caller = profileRepository.findById(myId);
        Optional<Profile> callee = profileRepository.findById(otherId);

        if (callee.isPresent() && callee.get().getFcmToken() != null) {
            String callerName = caller.map(Profile::getName).orElse("Ta");
            pushService.sendCallInvite(
                    callee.get().getFcmToken(), callerName, matchId);
        }

        return ResponseEntity.ok(Map.of("message", "通话邀请已发送"));
    }
}
