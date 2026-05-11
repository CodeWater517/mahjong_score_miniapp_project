package com.example.mahjong.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("room_seat_user_history")
@EqualsAndHashCode(callSuper = true)
// 座位用户历史表实体，用来记录玩家何时加入/离开某个座位。
public class RoomSeatUserHistory extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 历史记录主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 所属座位 ID。
    private Long seatId;
    // 玩家用户 ID。
    private Long userId;
    // 玩家在本房间内使用的昵称，可为空。
    private String roomNickname;
    // 从哪一局开始把该玩家计入个人统计。
    private Integer joinRoundNo;
    // 离开时最后参与到哪一局。
    private Integer leaveRoundNo;
    // 加入时间。
    private LocalDateTime joinTime;
    // 离开时间。
    private LocalDateTime leaveTime;
    // 离开原因，例如 QUIT、KICKED。
    private String leaveReason;
    // 是否仍然在座位上，1 表示当前有效。
    private Integer active;
}
