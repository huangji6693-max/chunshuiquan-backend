package com.chunshuiquan.backend.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CommentDto {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String replyToId;
    private String content;
    private OffsetDateTime createdAt;
}
