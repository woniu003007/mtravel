package com.mtravel.platform.common;

/**
 * 后端统一接口响应结构。
 *
 * @param code 响应状态码，0 表示成功
 * @param data 业务数据
 * @param error 错误详情
 * @param message 响应消息
 * @param <T> 业务数据类型
 */
public record ApiResponse<T>(
        int code,
        T data,
        Object error,
        String message
) {

    /**
     * 返回带业务数据的成功响应。
     *
     * @param data 业务数据
     * @return 成功响应
     * @param <T> 业务数据类型
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, data, null, "ok");
    }

    /**
     * 返回无业务数据的成功响应。
     *
     * @return 成功响应
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, null, null, "ok");
    }

    /**
     * 返回通用失败响应。
     *
     * @param message 失败原因
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(-1, null, message, message);
    }

    /**
     * 返回指定错误码的失败响应。
     *
     * @param code 错误码
     * @param message 失败原因
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, null, message, message);
    }
}
