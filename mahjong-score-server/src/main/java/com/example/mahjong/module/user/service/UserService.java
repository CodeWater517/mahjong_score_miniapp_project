package com.example.mahjong.module.user.service;

import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.exception.BizException;
import com.example.mahjong.module.user.dto.UserDtos;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// 用户业务服务：读取和修改用户资料。
public class UserService {

    private final SysUserMapper sysUserMapper;

    // 查询当前用户资料，手机号会脱敏后返回。
    public UserDtos.UserProfileResponse getMe(Long userId) {
        SysUser user = requireUser(userId);
        UserDtos.UserProfileResponse response = new UserDtos.UserProfileResponse();
        response.setUserId(user.getId());
        response.setPhone(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setTotalScore(user.getTotalScore());
        response.setTotalRounds(user.getTotalRounds());
        return response;
    }

    // 修改用户昵称。
    @Transactional
    public void updateProfile(Long userId, UserDtos.UpdateProfileRequest request) {
        SysUser user = requireUser(userId);
        user.setNickname(request.getNickname());
        sysUserMapper.updateById(user);
    }

    // 当前用户不存在时，视为登录态异常。
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    // 手机号脱敏，避免完整手机号直接展示到前端。
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
