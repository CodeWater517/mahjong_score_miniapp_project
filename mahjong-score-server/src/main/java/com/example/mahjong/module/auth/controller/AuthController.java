package com.example.mahjong.module.auth.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.security.UserContext;
import com.example.mahjong.module.auth.dto.AuthDtos;
import com.example.mahjong.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
// 认证接口控制器：负责微信登录和手机号绑定两个入口。
public class AuthController {

    private final AuthService authService;

    // 微信登录：不需要已有 token，前端传 code，后端返回本系统 token。
    @PostMapping("/wechat-login")
    public ApiResponse<AuthDtos.WechatLoginResponse> wechatLogin(@Valid @RequestBody AuthDtos.WechatLoginRequest request) {
        return ApiResponse.success(authService.wechatLogin(request));
    }

    // 绑定手机号：需要已登录，所以从 UserContext 取当前用户 ID。
    @PostMapping("/bind-phone")
    public ApiResponse<AuthDtos.BindPhoneResponse> bindPhone(@Valid @RequestBody AuthDtos.BindPhoneRequest request) {
        return ApiResponse.success(authService.bindPhone(UserContext.requireUserId(), request));
    }
}
