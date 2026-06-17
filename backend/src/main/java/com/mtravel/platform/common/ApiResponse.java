package com.mtravel.platform.common;

public record ApiResponse<T>(int code, T data, Object error, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, data, null, "ok");
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, null, null, "ok");
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(-1, null, message, message);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, null, message, message);
    }
}
