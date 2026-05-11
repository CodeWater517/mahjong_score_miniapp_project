package com.example.mahjong.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 用户模块 DTO 集合。
public final class UserDtos {

    // 只作为静态内部类容器，不允许实例化。
    private UserDtos() {
    }

    @Data
    // 当前用户资料响应，对应 /api/user/me。
    public static class UserProfileResponse {
        // 用户 ID。
        private Long userId;
        // 脱敏手机号。
        private String phone;
        // 昵称。
        private String nickname;
        // 头像地址。
        private String avatarUrl;
        // 全局总净分。
        private Integer totalScore;
        // 全局总局数。
        private Integer totalRounds;
    }

    @Data
    // 修改资料请求，目前只允许改昵称。
    public static class UpdateProfileRequest {
        // 新昵称，不能为空。
        @NotBlank
        private String nickname;
    }
}
