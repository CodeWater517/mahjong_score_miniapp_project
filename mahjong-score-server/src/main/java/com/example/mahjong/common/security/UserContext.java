package com.example.mahjong.common.security;

import com.example.mahjong.common.api.ErrorCode;
import com.example.mahjong.common.exception.BizException;

// 当前请求用户上下文。ThreadLocal 能让同一个请求链上的 Service/Controller 随时取到 userId。
public final class UserContext {

    // 每个请求线程都有自己的一份 userId，不会互相覆盖。
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    // JwtAuthFilter 解析 token 成功后调用。
    public static void setUserId(Long userId) {
        HOLDER.set(userId);
    }

    // 允许返回 null，适合拦截器里做宽松判断。
    public static Long getUserId() {
        return HOLDER.get();
    }

    // 必须登录的接口调用这个方法；没有用户就抛业务异常。
    public static Long requireUserId() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }

    // 请求结束时清理，避免线程池复用导致用户串号。
    public static void clear() {
        HOLDER.remove();
    }

    // 工具类不允许实例化。
    private UserContext() {
    }
}
