package com.chunshuiquan.backend.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class MomentDto {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String authorVipTier;
    private String content;
    private List<String> imageUrls;
    private String location;
    private int likeCount;
    private int commentCount;
    private boolean likedByMe;
    private OffsetDateTime createdAt;
}
