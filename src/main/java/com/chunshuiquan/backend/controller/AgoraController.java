package com.chunshuiquan.backend.controller;

import io.agora.media.RtcTokenBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agora")
public class AgoraController {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    /**
     * GET /api/agora/token?channelName={matchId}&uid=0
     *
     * Returns an RTC token valid for 1 hour.
     * channelName 传 matchId (UUID string)，双方用同一个 matchId 就能进同一频道。
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestParam String channelName,
            @RequestParam(defaultValue = "0") int uid,
            @AuthenticationPrincipal UserDetails userDetails) {

        int expireTs = (int) (System.currentTimeMillis() / 1000) + 3600;

        String token = new RtcTokenBuilder().buildTokenWithUid(
                appId, appCertificate, channelName, uid,
                RtcTokenBuilder.Role.Role_Publisher, expireTs);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "appId", appId,
                "channelName", channelName,
                "uid", uid
        ));
    }
}
