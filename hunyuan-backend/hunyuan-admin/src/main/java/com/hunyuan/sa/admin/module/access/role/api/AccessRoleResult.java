package com.hunyuan.sa.admin.module.access.role.api;

/**
 * 角色生命周期公开结果，不向消费者暴露旧系统响应类型。
 */
public record AccessRoleResult<T>(
        T data,
        AccessRoleFailure failure,
        String message
) {

    public static <T> AccessRoleResult<T> success(T data) {
        return new AccessRoleResult<>(data, null, null);
    }

    public static <T> AccessRoleResult<T> failure(AccessRoleFailure failure, String message) {
        return new AccessRoleResult<>(null, failure, message);
    }

    public boolean successful() {
        return failure == null;
    }
}
