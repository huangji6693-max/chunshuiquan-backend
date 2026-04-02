package com.chunshuiquan.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    private boolean enabled = false;

    @PostConstruct
    public void init() {
        String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            logger.warn("FIREBASE_SERVICE_ACCOUNT not set, push notifications disabled");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))))
                        .build();
                FirebaseApp.initializeApp(options);
            }
            enabled = true;
            logger.info("Firebase initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase", e);
        }
    }

    public void sendNewMatchNotification(String fcmToken, String matcherName, String matchId) {
        if (!enabled || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("新配对!")
                            .setBody(matcherName + " 喜欢你，快去聊天吧！")
                            .build())
                    .putData("type", "new_match")
                    .putData("matchId", matchId)
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            logger.error("Failed to send match notification to token {}", fcmToken, e);
        }
    }

    public void sendGiftNotification(String fcmToken, String senderName, String giftName, String matchId) {
        if (!enabled || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("收到礼物!")
                            .setBody(senderName + " 送了你一个" + giftName + " ❤️")
                            .build())
                    .putData("type", "gift_received")
                    .putData("matchId", matchId)
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            logger.error("Failed to send gift notification to token {}", fcmToken, e);
        }
    }

    public void sendNewMessageNotification(String fcmToken, String senderName, String preview, String matchId) {
        if (!enabled || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(senderName)
                            .setBody(preview)
                            .build())
                    .putData("type", "new_message")
                    .putData("matchId", matchId)
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            logger.error("Failed to send message notification to token {}", fcmToken, e);
        }
    }

    public void sendCallInvite(String fcmToken, String callerName, String matchId) {
        if (!enabled || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("语音通话")
                            .setBody(callerName + " 想和你语音通话")
                            .build())
                    .putData("type", "call_invite")
                    .putData("matchId", matchId)
                    .putData("callerName", callerName)
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            logger.error("Failed to send call invite to token {}", fcmToken, e);
        }
    }
}
