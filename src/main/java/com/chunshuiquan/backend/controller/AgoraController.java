package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.util.agora.RtcTokenBuilder2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/agora")
public class AgoraController {

    @Value("${agora.appId}")
    private String appId;

    @Value("${agora.appCertificate}")
    private String appCertificate;

    private static final int TOKEN_EXPIRE_SECONDS = 3600;
    private static final int PRIVILEGE_EXPIRE_SECONDS = 3600;

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
}
