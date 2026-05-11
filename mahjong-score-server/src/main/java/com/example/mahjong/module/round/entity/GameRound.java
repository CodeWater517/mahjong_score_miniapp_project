package com.example.mahjong.module.round.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("game_round")
@EqualsAndHashCode(callSuper = true)
// 单局实体，对应 game_round，一行表示房间中的一局计分。
public class GameRound extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 局主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 所属房间开启段 ID。
    private Long sessionId;
    // 第几局，从 1 递增。
    private Integer roundNo;
    // 局状态：SUBMITTING、SETTLED、DELETED。
    private String status;
    // 结算时间。
    private LocalDateTime settledAt;
    // 逻辑删除标记，1 表示已删除。
    private Integer deleted;
}
