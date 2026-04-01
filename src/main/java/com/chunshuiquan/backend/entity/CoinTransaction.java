package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 金币交易记录（充值/消费/赠送奖励等）
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "coin_transactions")
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 变动数量（正=收入，负=支出） */
    @Column(nullable = false)
    private Integer amount;

    /** 变动后余额 */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /** 交易类型: recharge / gift_sent / gift_received_bonus / daily_bonus / admin_grant */
    @Column(nullable = false)
    private String type;

    /** 备注（如充值包名称、礼物名称等） */
    private String note;

    /** 关联的外部订单号（充值时） */
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
