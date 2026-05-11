package com.example.mahjong.module.user.controller;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.security.UserContext;
import com.example.mahjong.module.user.dto.UserDtos;
import com.example.mahjong.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
// 用户接口控制器：当前只提供查看自己和修改资料。
public class UserController {

    private final UserService userService;

    // 获取当前登录用户资料。
    @GetMapping("/me")
    public ApiResponse<UserDtos.UserProfileResponse> me() {
        return ApiResponse.success(userService.getMe(UserContext.requireUserId()));
    }

    // 修改当前登录用户资料。
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        userService.updateProfile(UserContext.requireUserId(), request);
        return ApiResponse.success();
    }
}
