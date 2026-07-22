package com.hunyuan.sa.admin.module.access.datascope.api;

/**
 * 数据范围可见级别选项。
 */
public record AccessDataScopeViewOption(
        Integer viewType,
        Integer level,
        String name
) {
}
