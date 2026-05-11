package com.example.mahjong.common.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
// 数据库实体的公共字段。继承它的表都会有创建时间和更新时间。
public abstract class BaseEntity {
    // 记录创建时间，由数据库默认值维护。
    private LocalDateTime createdAt;
    // 记录更新时间，由数据库 ON UPDATE 自动维护。
    private LocalDateTime updatedAt;
}
