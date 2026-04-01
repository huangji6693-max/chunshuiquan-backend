package com.chunshuiquan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CheckInResultDto {
    private Boolean checkedInToday;
    private Integer streakDays;
    private Integer todayReward;
    private Integer totalCoins;
    /** 最近7天签到状态 [true, false, true, ...] 从周一到周日 */
    private List<Boolean> weekStatus;
}
