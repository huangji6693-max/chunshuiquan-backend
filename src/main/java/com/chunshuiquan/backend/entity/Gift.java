package com.chunshuiquan.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟礼物实体
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "gifts")
public class Gift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 礼物名称：玫瑰/钻戒/跑车/城堡... */
    private String name;

    /** emoji或图标标识 */
    private String icon;

    /** 消耗金币数 */
    private Integer coins;

    /** 是否上架 */
    @Column(name = "is_active")
    private Boolean isActive;
}
