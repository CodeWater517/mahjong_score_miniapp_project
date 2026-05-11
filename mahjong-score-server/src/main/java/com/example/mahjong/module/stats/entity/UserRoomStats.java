package com.example.mahjong.module.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("user_room_stats")
@EqualsAndHashCode(callSuper = true)
// 用户在单个房间内的统计实体，用于房间排行和个人最近房间。
public class UserRoomStats extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 房间统计主键 ID。
    private Long id;
    // 房间 ID。
    private Long roomId;
    // 用户 ID。
    private Long userId;
    // 用户在该房间的总净分。
    private Integer totalScore;
    // 用户在该房间实际参与局数。
    private Integer totalRounds;
    // 净分为正的局数。
    private Integer winRounds;
    // 净分为负的局数。
    private Integer loseRounds;
    // 净分为 0 的局数。
    private Integer drawRounds;
    // 正分总和。
    private Integer positiveScoreSum;
    // 负分绝对值总和。
    private Integer negativeScoreSum;
    // 单局最高净分。
    private Integer highestRoundScore;
    // 单局最低净分。
    private Integer lowestRoundScore;
    // 首次参与该房间计分的时间。
    private LocalDateTime firstJoinTime;
    // 最近一次参与该房间计分的时间。
    private LocalDateTime lastPlayTime;
}
