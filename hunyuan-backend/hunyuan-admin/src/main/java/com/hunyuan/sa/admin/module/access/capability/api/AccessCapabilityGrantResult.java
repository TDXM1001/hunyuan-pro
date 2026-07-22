package com.hunyuan.sa.admin.module.access.capability.api;

/**
 * 角色能力授权结果，不向消费者暴露旧系统响应类型。
 */
public record AccessCapabilityGrantResult<T>(
        T data,
        AccessCapabilityGrantFailure failure
) {

    public static <T> AccessCapabilityGrantResult<T> success(T data) {
        return new AccessCapabilityGrantResult<>(data, null);
    }

    public static <T> AccessCapabilityGrantResult<T> failure(AccessCapabilityGrantFailure failure) {
        return new AccessCapabilityGrantResult<>(null, failure);
    }

    public boolean successful() {
        return failure == null;
    }
}
