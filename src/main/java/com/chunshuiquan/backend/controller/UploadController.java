package com.chunshuiquan.backend.controller;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final Cloudinary cloudinary;

    public UploadController(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    @GetMapping("/signature")
    public ResponseEntity<?> signature(@AuthenticationPrincipal String userId) {
        long timestamp = System.currentTimeMillis() / 1000;
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        try {
            String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);
            return ResponseEntity.ok(Map.of(
                    "signature", signature,
                    "timestamp", timestamp,
                    "apiKey", cloudinary.config.apiKey,
                    "cloudName", cloudinary.config.cloudName
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("签名生成失败");
        }
    }
}
