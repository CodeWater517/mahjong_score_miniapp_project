package com.example.mahjong.module.round.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("round_participant")
@EqualsAndHashCode(callSuper = true)
// 单局参与者实体：每一局中每个座位上的玩家都有一条记录。
public class RoundParticipant extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 参与记录主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 所属局 ID。
    private Long roundId;
    // 本局对应座位 ID。
    private Long seatId;
    // 本局对应用户 ID，玩家离开后历史记录仍可保留。
    private Long userId;
    // 提交状态：PENDING、SUBMITTED、OWNER_SUBMITTED、FORCED_SUBMITTED。
    private String submitStatus;
    // 实际提交人，房主代交时是房主 ID。
    private Long submittedBy;
    // 提交时间。
    private LocalDateTime submittedAt;
    // 本局净分，结算时由输分流水计算得出。
    private Integer netScore;
    // 是否计入个人统计，换座/离座场景可用它控制统计边界。
    private Integer activeForStats;
}
