package com.example.mahjong.module.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@TableName("user_daily_stats")
@EqualsAndHashCode(callSuper = true)
// 用户每日统计实体，用于月榜、周榜等按日期聚合的统计。
public class UserDailyStats extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 每日统计主键 ID。
    private Long id;
    // 用户 ID。
    private Long userId;
    // 统计日期。
    private LocalDate statDate;
    // 当天总净分。
    private Integer totalScore;
    // 当天参与局数。
    private Integer totalRounds;
    // 当天净分为正的局数。
    private Integer winRounds;
    // 当天正分总和。
    private Integer positiveScoreSum;
    // 当天负分绝对值总和。
    private Integer negativeScoreSum;
}
