package com.hunyuan.sa.admin.module.access.menu.api;

/**
 * 菜单目录公开结果，不向消费者暴露旧系统响应类型。
 */
public record AccessMenuResult<T>(
        T data,
        AccessMenuFailure failure,
        String message
) {

    public static <T> AccessMenuResult<T> success(T data) {
        return new AccessMenuResult<>(data, null, null);
    }

    public static <T> AccessMenuResult<T> failure(AccessMenuFailure failure, String message) {
        return new AccessMenuResult<>(null, failure, message);
    }

    public boolean successful() {
        return failure == null;
    }
}
