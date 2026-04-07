package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.AuthResponse;
import com.chunshuiquan.backend.dto.LoginRequest;
import com.chunshuiquan.backend.dto.RegisterRequest;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       TokenBlacklistService tokenBlacklistService) {
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse register(RegisterRequest req) {
        // [fix] email 强制 toLowerCase + trim 防止大小写/空格导致重复
        final String email = normalizeEmail(req.getEmail());
        if (profileRepository.existsByEmail(email)) {
            // [fix] 抛 EmailAlreadyExists 让 controller 返回 409 + 引导前端跳转登录
            throw new EmailAlreadyExistsException("该邮箱已注册，请直接登录");
        }
        Profile p = new Profile();
        p.setEmail(email);
        p.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        p.setName(req.getName() != null && !req.getName().isBlank()
                ? req.getName() : email.split("@")[0]);
        // [fix] birthDate 默认值 — Profile.birth_date 是 NOT NULL 列, 不能 null
        // 用户在 Onboarding 阶段会更新真实生日。原 commit 56f26a8 只改了 DTO 漏了这里, 导致 500
        p.setBirthDate(req.getBirthDate() != null
                ? req.getBirthDate()
                : java.time.LocalDate.of(2000, 1, 1));
        p.setGender(req.getGender());
        p = profileRepository.save(p);
        String userId = p.getId().toString();
        return AuthResponse.of(
                jwtUtil.generateToken(userId),
                jwtUtil.generateRefreshToken(userId),
                p);
    }

    public AuthResponse login(LoginRequest req) {
        // [fix] email 同步 normalize 保证一致性
        final String email = normalizeEmail(req.getEmail());
        Profile p = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), p.getPasswordHash())) {
            throw new IllegalArgumentException("用户不存在或密码错误");
        }
        p.setLastActive(OffsetDateTime.now());
        profileRepository.save(p);
        String userId = p.getId().toString();
        return AuthResponse.of(
                jwtUtil.generateToken(userId),
                jwtUtil.generateRefreshToken(userId),
                p);
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    /** 邮箱已注册异常 — Controller 捕获后返回 409 Conflict */
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String msg) { super(msg); }
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("无效的 refresh token");
        }
        // 检查refresh token是否已被吊销（防止重放攻击）
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("该 refresh token 已被吊销");
        }
        String userId = jwtUtil.extractUserId(refreshToken);
        Profile p = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        // 将旧的refresh token加入黑名单，防止重放
        tokenBlacklistService.blacklist(refreshToken, jwtUtil.getRemainingMillis(refreshToken));
        return AuthResponse.of(
                jwtUtil.generateToken(userId),
                jwtUtil.generateRefreshToken(userId), // rotation
                p);
    }
}
