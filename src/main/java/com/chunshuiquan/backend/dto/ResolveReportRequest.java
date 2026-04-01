package com.chunshuiquan.backend.dto;

import lombok.Data;

/**
 * 处理举报请求
 */
@Data
public class ResolveReportRequest {
    /** warn: 警告, ban: 封禁, dismiss: 驳回 */
    private String action;
}
