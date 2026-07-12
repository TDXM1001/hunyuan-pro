package com.hunyuan.sa.bpm.module.definition.service;

import java.util.Map;

/**
 * 发布后保存在定义版本上的跨模块不可变引用快照。
 */
public record GraphPublicationDependencySnapshot(Map<String, Object> values) {

    public GraphPublicationDependencySnapshot {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public Map<String, Object> toSnapshotMap() {
        return values;
    }
}
