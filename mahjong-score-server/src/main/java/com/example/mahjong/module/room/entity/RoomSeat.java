package com.example.mahjong.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("room_seat")
@EqualsAndHashCode(callSuper = true)
// 房间座位实体，对应 room_seat。座位分会被继承，玩家离开不清零。
public class RoomSeat extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 座位主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 座位序号，1 到 4。
    private Integer seatNo;
    // 座位编码：EAST/SOUTH/WEST/NORTH。
    private String seatName;
    // 当前坐在该座位上的用户；允许更新为 null，所以设置 ALWAYS。
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long currentUserId;
    // 当前座位分数，新玩家加入空座后会继承这个分数。
    private Integer currentScore;
    // 当前用户加入该座位的时间；离开时允许清空。
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime joinedAt;
}
