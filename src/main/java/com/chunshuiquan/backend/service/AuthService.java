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

@Service
public class AuthService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
        return AuthResponse.of(jwtUtil.generateToken(p.getId().toString()), p);
    }

    public AuthResponse login(LoginRequest req) {
        Profile p = profileRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), p.getPasswordHash())) {
            throw new IllegalArgumentException("用户不存在或密码错误");
        }
        p.setLastActive(OffsetDateTime.now());
        profileRepository.save(p);
        return AuthResponse.of(jwtUtil.generateToken(p.getId().toString()), p);
    }
}
