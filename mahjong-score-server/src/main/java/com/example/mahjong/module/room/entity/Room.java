package com.example.mahjong.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("room")
@EqualsAndHashCode(callSuper = true)
// 房间实体，对应 room 表，保存房间基本信息和当前状态。
public class Room extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 房间主键 ID。
    private Long id;
    // 6 位数字房间号，给用户输入和分享使用。
    private String roomCode;
    // 房间名称，默认由系统生成。
    private String roomName;
    // 房主用户 ID。
    private Long ownerUserId;
    // 房间人数，支持 2 到 4 人。
    private Integer playerCount;
    // 座位初始分，新房间的每个座位从这个分数开始。
    private Integer initialScore;
    // 房间状态：WAITING、PLAYING、CLOSED。
    private String status;
    // 已完成的最大局号。
    private Integer currentRoundNo;
    // 最近一次成功结算时间，用于自动关闭判断。
    private LocalDateTime lastScoreTime;
}
