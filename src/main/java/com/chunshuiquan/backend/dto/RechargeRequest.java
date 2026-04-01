package com.chunshuiquan.backend.dto;

import lombok.Data;

/**
 * 金币充值请求
 */
@Data
public class RechargeRequest {
    /** 充值包ID: small / medium / large / mega */
    private String packageId;
    /** 支付平台收据/token（用于服务端校验） */
    private String receipt;
    /** 支付平台: apple / google / stripe */
    private String platform;
}
