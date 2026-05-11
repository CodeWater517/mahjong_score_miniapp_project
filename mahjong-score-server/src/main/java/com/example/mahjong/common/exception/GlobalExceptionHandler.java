package com.example.mahjong.common.exception;

import com.example.mahjong.common.api.ApiResponse;
import com.example.mahjong.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// 全局异常处理器：Controller 抛出的异常会在这里统一转换成 ApiResponse。
public class GlobalExceptionHandler {

    // 业务异常直接返回它自己的错误码和消息。
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    // 参数校验、JSON 格式错误等，都统一提示“请求参数不正确”。
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleValidation(Exception ex) {
        return ApiResponse.fail(ErrorCode.VALIDATION_ERROR, "请求参数不正确");
    }

    // 兜底异常，避免把后端堆栈信息暴露给前端用户。
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleAny(Exception ex) {
        return ApiResponse.fail(50000, "系统繁忙，请稍后再试");
    }
}
