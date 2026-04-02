package com.chunshuiquan.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("正常请求通过，返回限流头")
    void normalRequest_passes() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
        assertEquals("59", response.getHeader("X-RateLimit-Remaining"));
        assertNotEquals(429, response.getStatus());
    }

    @Test
    @DisplayName("超过60次请求返回429")
    void exceedingLimit_returns429() throws IOException, ServletException {
        String clientIp = "10.0.0.1";

        // 先发送60个正常请求
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilter(request, response, filterChain);
            assertNotEquals(429, response.getStatus(), "第" + (i + 1) + "次请求不应被限流");
        }

        // 第61次请求应该被限流
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("请求过于频繁"));
        // filterChain不应被调用（第61次）
        verify(filterChain, times(60)).doFilter(any(), any());
    }

    @Test
    @DisplayName("不同IP互不影响")
    void differentIps_areIndependent() throws IOException, ServletException {
        // IP1发送60次请求
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
            req.setRemoteAddr("10.0.0.1");
            rateLimitFilter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // IP2的第一次请求应该通过
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        assertNotEquals(429, response.getStatus());
        assertEquals("59", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("健康检查不限流")
    void healthCheck_notRateLimited() throws IOException, ServletException {
        // 即使超过限制，健康检查也应该通过
        String clientIp = "10.0.0.100";

        // 先用普通请求耗尽限额
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
            req.setRemoteAddr(clientIp);
            rateLimitFilter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // 健康检查请求仍应通过
        MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/api/health");
        healthReq.setRemoteAddr(clientIp);
        MockHttpServletResponse healthRes = new MockHttpServletResponse();

        rateLimitFilter.doFilter(healthReq, healthRes, filterChain);

        assertNotEquals(429, healthRes.getStatus());
        // 健康检查不设置限流头
        assertNull(healthRes.getHeader("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("X-Forwarded-For头正确解析客户端IP")
    void xForwardedFor_extractsCorrectIp() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18, 150.172.238.178");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        // 应该使用X-Forwarded-For的第一个IP
        assertNotEquals(429, response.getStatus());
        assertEquals("59", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Swagger路径不限流")
    void swaggerPath_notRateLimited() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(response.getHeader("X-RateLimit-Limit"));
    }
}
