package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.AuthResponse;
import com.chunshuiquan.backend.dto.LoginRequest;
import com.chunshuiquan.backend.dto.LogoutRequest;
import com.chunshuiquan.backend.dto.RefreshRequest;
import com.chunshuiquan.backend.dto.RegisterRequest;
import com.chunshuiquan.backend.security.JwtUtil;
import com.chunshuiquan.backend.service.AuthService;
import com.chunshuiquan.backend.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          TokenBlacklistService tokenBlacklistService,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    /**
     * 登出端点：将当前access token和可选的refresh token加入黑名单
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                       @RequestBody(required = false) LogoutRequest body) {
        // 从Authorization header提取access token并加入黑名单
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            tokenBlacklistService.blacklist(accessToken, jwtUtil.getRemainingMillis(accessToken));
        }
        // 如果请求体中包含refresh token，也加入黑名单
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            String refreshToken = body.getRefreshToken();
            if (jwtUtil.isValid(refreshToken)) {
                tokenBlacklistService.blacklist(refreshToken, jwtUtil.getRemainingMillis(refreshToken));
            }
        }
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBiz(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** [fix] 邮箱已注册 — 返回 409 让前端识别后跳转登录页 */
    @ExceptionHandler(AuthService.EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(AuthService.EmailAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage(), "code", "EMAIL_EXISTS"));
    }
}
