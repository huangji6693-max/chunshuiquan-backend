package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    private String gender;

    @Column(name = "looking_for")
    private String lookingFor = "everyone";

    private String bio = "";

    @Column(name = "job_title")
    private String jobTitle = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "avatar_urls", columnDefinition = "text[]")
    private String[] avatarUrls = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] tags = new String[0];

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_active")
    private OffsetDateTime lastActive;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "fcm_token")
    private String fcmToken;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (lastActive == null) lastActive = OffsetDateTime.now();
    }
}
