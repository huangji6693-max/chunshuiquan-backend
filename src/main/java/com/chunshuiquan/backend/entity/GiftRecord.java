package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 礼物赠送记录
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "gift_records")
public class GiftRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 送礼者ID */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** 收礼者ID */
    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    /** 礼物ID */
    @Column(name = "gift_id", nullable = false)
    private Long giftId;

    /** 在哪个匹配中送的 */
    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
