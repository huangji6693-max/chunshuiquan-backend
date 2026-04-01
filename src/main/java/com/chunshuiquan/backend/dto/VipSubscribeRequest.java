package com.chunshuiquan.backend.dto;

import lombok.Data;

/**
 * VIP订阅请求
 */
@Data
public class VipSubscribeRequest {
    /** 套餐ID: gold_monthly / gold_quarterly / gold_yearly / diamond_monthly / diamond_quarterly / diamond_yearly */
    private String planId;
    /** 支付凭证 */
    private String receipt;
    /** 支付平台 */
    private String platform;
}
