package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.UpdateProfileRequest;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.*;
import com.chunshuiquan.backend.service.ClarifaiService;
import com.chunshuiquan.backend.service.OnlineStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final ProfileRepository profileRepository;
    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final ReportRepository reportRepository;
    private final GiftRecordRepository giftRecordRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final VipOrderRepository vipOrderRepository;
    private final ClarifaiService clarifaiService;
    private final OnlineStatusService onlineStatusService;

    public UserController(ProfileRepository profileRepository,
                          MessageRepository messageRepository,
                          MatchRepository matchRepository,
                          SwipeRepository swipeRepository,
                          BlockedUserRepository blockedUserRepository,
                          ReportRepository reportRepository,
                          GiftRecordRepository giftRecordRepository,
                          CoinTransactionRepository coinTransactionRepository,
                          VipOrderRepository vipOrderRepository,
                          ClarifaiService clarifaiService,
                          OnlineStatusService onlineStatusService) {
        this.profileRepository = profileRepository;
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
        this.swipeRepository = swipeRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.reportRepository = reportRepository;
        this.giftRecordRepository = giftRecordRepository;
        this.coinTransactionRepository = coinTransactionRepository;
        this.vipOrderRepository = vipOrderRepository;
        this.clarifaiService = clarifaiService;
        this.onlineStatusService = onlineStatusService;
    }

    // GET /api/users/me — 当前用户信息
    @GetMapping("/me")
    public ResponseEntity<Profile> me(@AuthenticationPrincipal String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(p -> { return ResponseEntity.ok(p); })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/profile — 更新资料
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal String userId,
            @jakarta.validation.Valid @RequestBody UpdateProfileRequest req) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    if (req.getName() != null && !req.getName().isBlank())
                        profile.setName(req.getName());
                    if (req.getBio() != null)
                        profile.setBio(req.getBio());
                    if (req.getJobTitle() != null)
                        profile.setJobTitle(req.getJobTitle());
                    if (req.getLookingFor() != null)
                        profile.setLookingFor(req.getLookingFor());
                    if (req.getHeight() != null)
                        profile.setHeight(req.getHeight());
                    if (req.getEducation() != null)
                        profile.setEducation(req.getEducation());
                    if (req.getZodiac() != null)
                        profile.setZodiac(req.getZodiac());
                    if (req.getCity() != null)
                        profile.setCity(req.getCity());
                    if (req.getSmoking() != null)
                        profile.setSmoking(req.getSmoking());
                    if (req.getDrinking() != null)
                        profile.setDrinking(req.getDrinking());
                    if (req.getLatitude() != null)
                        profile.setLatitude(req.getLatitude());
                    if (req.getLongitude() != null)
                        profile.setLongitude(req.getLongitude());
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/users/avatar — 上传头像 URL（前端上传到 CDN 后传 URL 过来）
    @PostMapping("/avatar")
    public ResponseEntity<?> addAvatar(
            @AuthenticationPrincipal String userId,
            @RequestBody java.util.Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "url 不能为空"));
        }

        // Clarifai 内容预检：NSFW score > 0.85 直接拒绝
        try {
            boolean safe = clarifaiService.isImageSafe(url).get(4, TimeUnit.SECONDS);
            if (!safe) {
                return ResponseEntity.unprocessableEntity()
                        .body(java.util.Map.of("error", "图片包含不适宜内容，请更换"));
            }
        } catch (TimeoutException e) {
            logger.warn("Clarifai pre-check timeout for {}, rejecting (fail-close)", url);
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "图片审核超时，请重试"));
        } catch (Exception e) {
            logger.error("Clarifai pre-check error for {}, rejecting (fail-close)", url, e);
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "图片审核失败，请重试"));
        }

        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    List<String> urls = new ArrayList<>(Arrays.asList(
                            profile.getAvatarUrls() != null ? profile.getAvatarUrls() : new String[0]
                    ));
                    urls.add(url);
                    profile.setAvatarUrls(urls.toArray(new String[0]));

                    // 预检已通过，直接标记 approved
                    List<String> statuses = new ArrayList<>(Arrays.asList(
                            profile.getPhotoStatuses() != null ? profile.getPhotoStatuses() : new String[0]
                    ));
                    while (statuses.size() < urls.size() - 1) statuses.add("approved");
                    statuses.add("approved");
                    profile.setPhotoStatuses(statuses.toArray(new String[0]));

                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/avatar/reorder — 重排照片顺序
    @PutMapping("/avatar/reorder")
    public ResponseEntity<?> reorderAvatars(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, List<String>> body) {
        List<String> newOrder = body.get("avatarUrls");
        if (newOrder == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "avatarUrls 不能为空"));
        }

        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    List<String> current = new ArrayList<>(Arrays.asList(
                            profile.getAvatarUrls() != null ? profile.getAvatarUrls() : new String[0]
                    ));

                    // 验证新列表与原列表包含相同的URL（防止篡改）
                    List<String> sortedCurrent = new ArrayList<>(current);
                    List<String> sortedNew = new ArrayList<>(newOrder);
                    java.util.Collections.sort(sortedCurrent);
                    java.util.Collections.sort(sortedNew);
                    if (!sortedCurrent.equals(sortedNew)) {
                        return ResponseEntity.badRequest().body(
                                (Object) Map.of("error", "照片列表不匹配，不能添加或删除照片"));
                    }

                    // 同步更新 photoStatuses 顺序
                    List<String> statuses = new ArrayList<>(Arrays.asList(
                            profile.getPhotoStatuses() != null ? profile.getPhotoStatuses() : new String[0]
                    ));
                    // 建立 url -> status 映射
                    Map<String, String> urlStatusMap = new HashMap<>();
                    for (int i = 0; i < current.size(); i++) {
                        String status = i < statuses.size() ? statuses.get(i) : "approved";
                        urlStatusMap.put(current.get(i), status);
                    }
                    // 按新顺序重建 statuses
                    List<String> newStatuses = new ArrayList<>();
                    for (String url : newOrder) {
                        newStatuses.add(urlStatusMap.getOrDefault(url, "approved"));
                    }

                    profile.setAvatarUrls(newOrder.toArray(new String[0]));
                    profile.setPhotoStatuses(newStatuses.toArray(new String[0]));
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok((Object) profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/avatar/{index} — 删除头像（按 index）
    @DeleteMapping("/avatar/{index}")
    public ResponseEntity<?> deleteAvatar(
            @AuthenticationPrincipal String userId,
            @PathVariable int index) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    List<String> urls = new ArrayList<>(Arrays.asList(
                            profile.getAvatarUrls() != null ? profile.getAvatarUrls() : new String[0]
                    ));
                    if (index < 0 || index >= urls.size()) {
                        return ResponseEntity.badRequest().body(
                                java.util.Map.of("error", "index 超出范围"));
                    }
                    urls.remove(index);
                    profile.setAvatarUrls(urls.toArray(new String[0]));

                    // Keep photoStatuses in sync
                    List<String> statuses = new ArrayList<>(Arrays.asList(
                            profile.getPhotoStatuses() != null ? profile.getPhotoStatuses() : new String[0]
                    ));
                    if (index < statuses.size()) statuses.remove(index);
                    profile.setPhotoStatuses(statuses.toArray(new String[0]));

                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok((Object) profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/users/feed — 推荐卡片列表（支持年龄、性别、距离筛选 + 分页）
    @GetMapping("/feed")
    public ResponseEntity<List<Profile>> feed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Double maxDistance) {
        UUID myId = UUID.fromString(userId);

        // 获取当前用户的经纬度，用于距离筛选
        Double myLat = null;
        Double myLon = null;
        if (maxDistance != null) {
            Profile me = profileRepository.findById(myId).orElse(null);
            if (me != null && me.getLatitude() != null && me.getLongitude() != null) {
                myLat = me.getLatitude();
                myLon = me.getLongitude();
            }
        }

        List<Profile> feed = profileRepository.findFeed(
                myId, minAge, maxAge, gender, myLat, myLon, maxDistance,
                PageRequest.of(page, size));
        feed.forEach(p -> p.setPasswordHash(null));
        return ResponseEntity.ok(feed);
    }

    // GET /api/users/nearby — 搜索附近的人
    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "50") double radiusKm,
            @RequestParam(defaultValue = "50") int size) {
        UUID myId = UUID.fromString(userId);

        // 如果前端没传经纬度，从当前用户资料获取
        Double lat = latitude;
        Double lon = longitude;
        if (lat == null || lon == null) {
            Profile me = profileRepository.findById(myId).orElse(null);
            if (me != null && me.getLatitude() != null && me.getLongitude() != null) {
                lat = me.getLatitude();
                lon = me.getLongitude();
            }
        }
        if (lat == null || lon == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "缺少经纬度信息，请先更新位置或传入 latitude/longitude 参数"));
        }

        List<Profile> nearby = profileRepository.findNearby(
                myId, lat, lon, radiusKm, PageRequest.of(0, size));
        nearby.forEach(p -> p.setPasswordHash(null));
        return ResponseEntity.ok(nearby);
    }

    // PUT /api/users/onboarding-complete — 标记完成新手引导
    @PutMapping("/onboarding-complete")
    public ResponseEntity<?> completeOnboarding(@AuthenticationPrincipal String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    profile.setOnboardingCompleted(true);
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/fcm-token — 更新 FCM 推送 token
    @PutMapping("/fcm-token")
    public ResponseEntity<?> updateFcmToken(
            @AuthenticationPrincipal String userId,
            @RequestBody java.util.Map<String, String> body) {
        String token = body.getOrDefault("fcmToken", body.get("token"));
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    profile.setFcmToken(token);
                    profileRepository.save(profile);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/me — 注销账号（级联删除：messages → matches → swipes → blocks/reports → profile）
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal String userId) {
        UUID myId = UUID.fromString(userId);

        // 1. 获取涉及该用户的所有 match ID
        List<UUID> matchIds = matchRepository.findByUser1IdOrUser2Id(myId, myId)
                .stream().map(Match::getId).toList();

        // 2. 删除这些 match 下的所有消息
        if (!matchIds.isEmpty()) {
            messageRepository.deleteByMatchIdIn(matchIds);
        }

        // 3. 删除 matches
        matchRepository.deleteByUser1IdOrUser2Id(myId, myId);

        // 4. 删除 swipes
        swipeRepository.deleteBySwipedIdOrSwiperId(myId, myId);

        // 5. 删除 blocks
        blockedUserRepository.deleteByBlockerIdOrBlockedId(myId, myId);

        // 6. 删除 reports
        reportRepository.deleteByReporterIdOrReportedId(myId, myId);

        // 6.5 删除礼物记录
        giftRecordRepository.deleteBySenderIdOrReceiverId(myId, myId);

        // 6.6 删除金币流水
        coinTransactionRepository.deleteByUserId(myId);

        // 6.7 删除VIP订单
        vipOrderRepository.deleteByUserId(myId);

        // 7. 删除 profile
        profileRepository.deleteById(myId);

        return ResponseEntity.noContent().build();
    }

    // GET /api/users/{userId}/online — 查询用户在线状态
    @GetMapping("/{userId}/online")
    public ResponseEntity<Map<String, Object>> getOnlineStatus(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("online", onlineStatusService.isOnline(userId));
        result.put("lastSeen", onlineStatusService.getLastSeen(userId));
        return ResponseEntity.ok(result);
    }

    // POST /api/users/heartbeat — 前端定期调用刷新在线状态（30秒一次）
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal String userId) {
        onlineStatusService.markOnline(userId);
        return ResponseEntity.ok().build();
    }
}
