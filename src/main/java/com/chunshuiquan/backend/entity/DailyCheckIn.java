package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 每日签到记录
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "daily_checkins", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "check_date"}))
public class DailyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    /** 当天奖励金币数 */
    @Column(nullable = false)
    private Integer reward;

    /** 连续签到天数（截至当天） */
    @Column(name = "streak_day", nullable = false)
    private Integer streakDay;
}
