package com.example.mahjong.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 认证模块 DTO 集合。DTO 是接口输入/输出对象，不直接等同于数据库表。
public final class AuthDtos {

    // 工具类写法：只作为内部类容器，不允许 new。
    private AuthDtos() {
    }

    @Data
    // 微信登录请求：前端通过 uni.login 拿到 code 后传给后端。
    public static class WechatLoginRequest {
        // 微信临时登录凭证，不能为空。
        @NotBlank
        private String code;
        // 可选昵称，开发或扩展时可用。
        private String nickname;
        // 可选头像地址。
        private String avatarUrl;
    }

    @Data
    // 微信登录响应：告诉前端 token、用户基本信息、是否已绑定手机号。
    public static class WechatLoginResponse {
        // 后端签发的 JWT，后续接口会放进 Authorization。
        private String token;
        // 当前用户 ID。
        private Long userId;
        // 是否已绑定手机号，前端据此决定是否跳绑定页。
        private Boolean hasBindPhone;
        // 用户昵称。
        private String nickname;
        // 用户头像。
        private String avatarUrl;
    }

    @Data
    // 绑定手机号请求：微信授权 code 或开发环境手机号。
    public static class BindPhoneRequest {
        @NotBlank
        private String phoneCode;
    }

    @Data
    // 绑定手机号响应：返回脱敏后的手机号。
    public static class BindPhoneResponse {
        private String phone;
    }
}
