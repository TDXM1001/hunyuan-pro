package com.hunyuan.sa.admin.module.access.datascope.api;

import java.util.List;

/**
 * 可配置的数据范围定义。
 */
public record AccessDataScopeDefinition(
        Integer dataScopeType,
        String dataScopeTypeName,
        String dataScopeTypeDescription,
        Integer sort,
        List<AccessDataScopeViewOption> viewOptions
) {
}
