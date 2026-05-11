package com.example.mahjong.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 后端统一响应结构。前端 request.js 会根据 code 是否为 0 判断成功或失败。
public class ApiResponse<T> {

    // 业务状态码：0 表示成功，其它值表示错误。
    private int code;
    // 给前端展示或调试用的提示信息。
    private String message;
    // 真正的业务数据，泛型 T 代表不同接口可以返回不同类型。
    private T data;

    // 成功且带数据的返回。
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    // 成功但不需要返回数据的返回。
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    // 失败返回，统一放错误码和错误信息。
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
