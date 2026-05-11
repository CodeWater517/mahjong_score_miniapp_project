package com.example.mahjong.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.mahjong.common.api.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
// 用户表实体，对应 sys_user，一行就是一个微信用户。
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO)
    // 用户主键 ID。
    private Long id;
    // 微信 openid，用来识别同一个微信用户。
    private String openid;
    // 绑定手机号，第一版绑定后不支持换绑。
    private String phone;
    // 用户昵称。
    private String nickname;
    // 微信头像地址。
    private String avatarUrl;
    // 全局累计净分，属于冗余统计字段，重算统计时会刷新。
    private Integer totalScore;
    // 全局累计参与局数，属于冗余统计字段。
    private Integer totalRounds;
}
