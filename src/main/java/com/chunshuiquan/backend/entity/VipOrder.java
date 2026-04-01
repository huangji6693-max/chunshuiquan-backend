package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * VIP订阅订单
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "vip_orders")
public class VipOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 会员等级: gold / diamond */
    @Column(nullable = false)
    private String tier;

    /** 时长(天): 30 / 90 / 365 */
    @Column(nullable = false)
    private Integer days;

    /** 支付金额(分) */
    @Column(nullable = false)
    private Integer amount;

    /** 支付平台: apple / google / stripe */
    private String platform;

    /** 外部订单号 */
    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
