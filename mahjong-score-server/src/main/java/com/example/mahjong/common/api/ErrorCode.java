package com.example.mahjong.common.api;

// 系统内部约定的业务错误码。前端可以根据这些码做统一处理，例如 40100 重新登录。
public final class ErrorCode {

    // 通用请求错误。
    public static final int BAD_REQUEST = 40000;
    // 未登录或 token 失效。
    public static final int UNAUTHORIZED = 40100;
    // 已登录但没有权限，例如未绑定手机号、不是房主。
    public static final int FORBIDDEN = 40300;
    // 通用资源不存在。
    public static final int NOT_FOUND = 40400;
    // 房间不存在。
    public static final int ROOM_NOT_FOUND = 40001;
    // 房间状态不允许当前操作。
    public static final int ROOM_STATE_ERROR = 40002;
    // 轮次状态不允许当前操作。
    public static final int ROUND_STATE_ERROR = 40003;
    // 参数校验失败。
    public static final int VALIDATION_ERROR = 42200;

    // 工具类不允许实例化。
    private ErrorCode() {
    }
}
