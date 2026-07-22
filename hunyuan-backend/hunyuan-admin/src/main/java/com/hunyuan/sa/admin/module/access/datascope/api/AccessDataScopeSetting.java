package com.hunyuan.sa.admin.module.access.datascope.api;

/**
 * 单项角色数据范围配置。
 */
public record AccessDataScopeSetting(
        Integer dataScopeType,
        Integer viewType
) {
}
