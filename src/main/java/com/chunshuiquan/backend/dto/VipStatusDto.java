package com.chunshuiquan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class VipStatusDto {
    private String tier;           // none / gold / diamond
    private OffsetDateTime expiresAt;
    private Integer daysLeft;
    private Boolean isActive;
}
