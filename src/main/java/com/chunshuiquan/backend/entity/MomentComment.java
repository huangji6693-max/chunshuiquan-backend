package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "moment_comments")
public class MomentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "moment_id", nullable = false)
    private UUID momentId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    /** 回复某条评论的ID（null表示顶级评论） */
    @Column(name = "reply_to_id")
    private UUID replyToId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
