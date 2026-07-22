package com.hunyuan.sa.admin.module.access.datascope.api;

/**
 * 数据范围管理用例结果。
 */
public record AccessDataScopeManagementResult<T>(
        T data,
        AccessDataScopeManagementFailure failure,
        String message
) {

    public static <T> AccessDataScopeManagementResult<T> success(T data) {
        return new AccessDataScopeManagementResult<>(data, null, null);
    }

    public static <T> AccessDataScopeManagementResult<T> failure(
            AccessDataScopeManagementFailure failure,
            String message) {
        return new AccessDataScopeManagementResult<>(null, failure, message);
    }

    public boolean successful() {
        return failure == null;
    }
}
