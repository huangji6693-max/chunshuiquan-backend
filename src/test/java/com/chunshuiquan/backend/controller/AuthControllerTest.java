package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.AuthResponse;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.security.JwtAuthFilter;
import com.chunshuiquan.backend.security.JwtUtil;
import com.chunshuiquan.backend.service.AuthService;
import com.chunshuiquan.backend.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private AuthResponse buildMockAuthResponse() {
        Profile p = new Profile();
        p.setId(UUID.randomUUID());
        p.setName("TestUser");
        p.setEmail("test@example.com");
        p.setBirthDate(LocalDate.of(2000, 1, 1));
        p.setGender("male");
        p.setCoins(100);
        p.setVipTier("none");
        p.setAvatarUrls(new String[0]);
        p.setTags(new String[0]);
        return AuthResponse.of("mock-access-token", "mock-refresh-token", p);
    }

    @Test
    @DisplayName("POST /api/auth/register - 成功注册")
    void register_success() throws Exception {
        AuthResponse mockResponse = buildMockAuthResponse();
        when(authService.register(any())).thenReturn(mockResponse);

        Map<String, Object> body = Map.of(
                "email", "newuser@example.com",
                "password", "123456",
                "name", "NewUser",
                "birthDate", "2000-01-15",
                "gender", "female"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.name").value("TestUser"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 成功登录")
    void login_success() throws Exception {
        AuthResponse mockResponse = buildMockAuthResponse();
        when(authService.login(any())).thenReturn(mockResponse);

        Map<String, String> body = Map.of(
                "email", "test@example.com",
                "password", "123456"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-access-token"))
                .andExpect(jsonPath("$.name").value("TestUser"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 密码错误返回400")
    void login_wrongPassword_returns400() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("用户不存在或密码错误"));

        Map<String, String> body = Map.of(
                "email", "test@example.com",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户不存在或密码错误"));
    }
}
