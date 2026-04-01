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
        if (profileRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        Profile p = new Profile();
        p.setEmail(req.getEmail());
        p.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        p.setName(req.getName());
        p.setBirthDate(req.getBirthDate());
        p.setGender(req.getGender());
        p = profileRepository.save(p);
        String userId = p.getId().toString();
        return AuthResponse.of(
                jwtUtil.generateToken(userId),
                jwtUtil.generateRefreshToken(userId),
                p);
    }

    public AuthResponse login(LoginRequest req) {
        Profile p = profileRepository.findByEmail(req.getEmail())
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
