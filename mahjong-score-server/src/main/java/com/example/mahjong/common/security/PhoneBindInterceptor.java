package com.example.mahjong.common.security;

import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.exception.BizException;
import com.example.mahjong.module.user.entity.SysUser;
import com.example.mahjong.module.user.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
// 手机号绑定拦截器：进入核心业务接口前，要求用户已经绑定手机号。
public class PhoneBindInterceptor implements HandlerInterceptor {

    private final SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 未登录情况由 JwtAuthFilter 处理，这里不重复报错。
            return true;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getPhone())) {
            // 已登录但未绑定手机号，不允许创建/加入房间和计分。
            throw new BizException(ErrorCode.FORBIDDEN, "请先绑定手机号");
        }
        return true;
    }
}
