package com.example.mahjong.common.exception;

import com.example.mahjong.common.api.ErrorCode;
import lombok.Getter;

@Getter
// 业务异常：用于表示“代码正常运行，但用户操作不符合业务规则”。
public class BizException extends RuntimeException {

    // 返回给前端的业务错误码。
    private final int code;

    // 默认使用 BAD_REQUEST 错误码。
    public BizException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    // 指定错误码和错误信息。
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
