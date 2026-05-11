package com.example.mahjong.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("room_session")
@EqualsAndHashCode(callSuper = true)
// 房间开启段实体：房间关闭后再重开，会产生新的 session。
public class RoomSession extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 开启段主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 第几次开启，从 1 递增。
    private Integer sessionNo;
    // 本次开启时间。
    private LocalDateTime startTime;
    // 本次关闭时间，未关闭时为空。
    private LocalDateTime endTime;
    // 开启时已经完成的最大局号。
    private Integer startRoundNo;
    // 关闭时已经完成的最大局号。
    private Integer endRoundNo;
    // 关闭原因：OWNER_CLOSE 或 AUTO_CLOSE。
    private String closeReason;
}
