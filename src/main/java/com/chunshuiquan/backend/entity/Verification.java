package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 实名认证申请
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "verifications")
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 真实姓名 */
    @Column(name = "real_name", nullable = false)
    private String realName;

    /** 证件照URL */
    @Column(name = "id_photo_url", nullable = false)
    private String idPhotoUrl;

    /** 手持证件自拍URL */
    @Column(name = "selfie_url", nullable = false)
    private String selfieUrl;

    /** 状态: pending / approved / rejected */
    @Column(nullable = false)
    private String status = "pending";

    /** 拒绝原因 */
    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
