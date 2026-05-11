package com.example.mahjong.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_operation_log")
// 房间操作日志实体：记录房主或玩家对房间做过的重要操作。
public class RoomOperationLog {
    @TableId(type = IdType.AUTO)
    // 日志主键 ID。
    private Long id;
    // 所属房间 ID。
    private Long roomId;
    // 操作人用户 ID。
    private Long operatorUserId;
    // 操作类型，例如 CREATE_ROOM、KICK_USER。
    private String operationType;
    // 被操作的目标用户，可为空。
    private Long targetUserId;
    // 被操作的目标轮次，可为空。
    private Long targetRoundId;
    // 操作前数据 JSON 字符串。
    private String beforeData;
    // 操作后数据 JSON 字符串。
    private String afterData;
    // 简短备注，方便后台排查。
    private String remark;
    // 日志创建时间。
    private LocalDateTime createdAt;
}
