package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "swipes", uniqueConstraints = @UniqueConstraint(columnNames = {"swiper_id", "swiped_id"}))
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "swiper_id", nullable = false)
    private UUID swiperId;

    @Column(name = "swiped_id", nullable = false)
    private UUID swipedId;

    @Column(nullable = false)
    private String direction;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
