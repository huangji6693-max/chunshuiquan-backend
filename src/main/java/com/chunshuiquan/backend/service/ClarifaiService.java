package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.repository.ProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ClarifaiService {

    private static final Logger logger = LoggerFactory.getLogger(ClarifaiService.class);
    private static final String API_URL = "https://api.clarifai.com/v2/models/nsfw-recognition/outputs";
    /** checkPhoto 使用的阈值（异步状态标记） */
    private static final double NSFW_THRESHOLD = 0.75;
    /** isImageSafe 使用的阈值（同步预检，更严格） */
    private static final double NSFW_SAFE_THRESHOLD = 0.85;

    private final ProfileRepository profileRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClarifaiService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * 异步判断图片是否安全。
     * 返回 true = 安全；false = NSFW score > 0.85（违规）。
     * API key 未配置时 fail-open（返回 true）。
     */
    @Async
    public CompletableFuture<Boolean> isImageSafe(String imageUrl) {
        String apiKey = System.getenv("CLARIFAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("CLARIFAI_API_KEY not set, skipping moderation (fail-open) for {}", imageUrl);
            return CompletableFuture.completedFuture(true);
        }
        try {
            double score = fetchNsfwScore(apiKey, imageUrl);
            boolean safe = score <= NSFW_SAFE_THRESHOLD;
            logger.info("Clarifai nsfw={} threshold={} safe={} for {}", score, NSFW_SAFE_THRESHOLD, safe, imageUrl);
            return CompletableFuture.completedFuture(safe);
        } catch (Exception e) {
            logger.error("Clarifai isImageSafe failed for {}, fail-open", imageUrl, e);
            return CompletableFuture.completedFuture(true);
        }
    }

    /**
     * 异步审核单张图片，结果写回 profile.photoStatuses[photoIndex]。
     * 由 UserController.addAvatar 在预检通过后触发，用于后台状态记录。
     */
    @Async
    public void checkPhoto(UUID profileId, int photoIndex, String imageUrl) {
        String apiKey = System.getenv("CLARIFAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("CLARIFAI_API_KEY not set, skipping moderation for profile {}", profileId);
            updatePhotoStatus(profileId, photoIndex, "approved");
            return;
        }
        try {
            double score = fetchNsfwScore(apiKey, imageUrl);
            String status = score > NSFW_THRESHOLD ? "rejected" : "approved";
            logger.info("checkPhoto profile={} index={} nsfw={} -> {}", profileId, photoIndex, score, status);
            updatePhotoStatus(profileId, photoIndex, status);
        } catch (Exception e) {
            logger.error("Clarifai checkPhoto failed for profile {} index {}", profileId, photoIndex, e);
            updatePhotoStatus(profileId, photoIndex, "approved"); // fail-open
        }
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /** 调用 Clarifai API 返回 NSFW score（0.0–1.0）。 */
    private double fetchNsfwScore(String apiKey, String imageUrl) throws Exception {
        String body = """
                {"inputs":[{"data":{"image":{"url":"%s"}}}]}
                """.formatted(imageUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Key " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Clarifai returned HTTP {}: {}", response.statusCode(), response.body());
            return 0.0; // fail-open
        }

        return extractNsfwScore(response.body());
    }

    private double extractNsfwScore(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode concepts = root.path("outputs").path(0).path("data").path("concepts");
        for (JsonNode concept : concepts) {
            if ("nsfw".equals(concept.path("name").asText())) {
                return concept.path("value").asDouble(0.0);
            }
        }
        return 0.0;
    }

    private void updatePhotoStatus(UUID profileId, int photoIndex, String status) {
        profileRepository.findById(profileId).ifPresentOrElse(profile -> {
            int len = profile.getAvatarUrls() != null ? profile.getAvatarUrls().length : 0;
            String[] statuses = profile.getPhotoStatuses();
            if (statuses == null || statuses.length != len) {
                statuses = new String[len];
                Arrays.fill(statuses, "pending");
            }
            if (photoIndex >= 0 && photoIndex < statuses.length) {
                statuses[photoIndex] = status;
                profile.setPhotoStatuses(statuses);
                profileRepository.save(profile);
                logger.info("Profile {} photo[{}] -> {}", profileId, photoIndex, status);
            }
        }, () -> logger.warn("Profile {} not found when updating photo status", profileId));
    }
}
