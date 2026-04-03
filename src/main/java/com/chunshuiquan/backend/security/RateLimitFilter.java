package com.chunshuiquan.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final Map<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String method = httpReq.getMethod();
        String path = httpReq.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/api/health")
                || path.startsWith("/ws")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpReq);
        long now = System.currentTimeMillis();

        // long[0]=count, long[1]=windowStart
        long[] bucket = requestCounts.compute(clientIp, (k, v) -> {
            if (v == null || now - v[1] > 60_000) return new long[]{1, now};
            v[0]++;
            return v;
        });

        if (bucket[0] > MAX_REQUESTS_PER_MINUTE) {
            httpRes.setStatus(429);
            httpRes.setContentType("application/json");
            httpRes.getWriter().write("{\"error\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
