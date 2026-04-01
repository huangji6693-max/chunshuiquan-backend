package com.chunshuiquan.backend.dto;

/**
 * 登出请求，可选携带refresh token一并吊销
 */
public class LogoutRequest {
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
