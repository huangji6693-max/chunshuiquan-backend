package com.chunshuiquan.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MessageResponseDto {

    private UUID id;
    private String content;
    private OffsetDateTime createdAt;
    private UUID senderId;
    private boolean isRead;

    public MessageResponseDto() {}

    public MessageResponseDto(UUID id, String content, OffsetDateTime createdAt,
                              UUID senderId, boolean isRead) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.senderId = senderId;
        this.isRead = isRead;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }
}
