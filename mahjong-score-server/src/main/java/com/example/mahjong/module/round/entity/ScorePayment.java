package com.example.mahjong.module.round.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("score_payment")
@EqualsAndHashCode(callSuper = true)
// 输分流水实体：一条记录表示 fromUser 输给 toUser 多少分。
public class ScorePayment extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 流水主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 所属局 ID。
    private Long roundId;
    // 输分玩家 ID。
    private Long fromUserId;
    // 输分玩家当时所在座位 ID。
    private Long fromSeatId;
    // 收分玩家 ID。
    private Long toUserId;
    // 收分玩家当时所在座位 ID。
    private Long toSeatId;
    // 输分分值，必须是正整数。
    private Integer score;
    // 可选备注。
    private String remark;
    // 创建人，自己提交时是本人，房主代交时是房主。
    private Long createdBy;
    // 是否房主代提交，1 表示是。
    private Integer isOwnerSubmit;
}
