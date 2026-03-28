package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private String content;

    @Column(name = "msg_type")
    private String msgType = "text";

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
