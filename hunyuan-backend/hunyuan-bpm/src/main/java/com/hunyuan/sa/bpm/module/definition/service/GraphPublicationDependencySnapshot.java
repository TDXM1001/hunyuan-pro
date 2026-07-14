package com.hunyuan.sa.bpm.module.definition.service;

import com.alibaba.fastjson.JSON;

import java.util.Collection;
import java.util.LinkedHashMap;
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

    public String canonicalPayload() {
        return JSON.toJSONString(values);
    }

    public Map<String, Object> businessMetadata() {
        return castMap(safeValue(values));
    }

    private Object safeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                String name = String.valueOf(key);
                if (!"canonicalPayload".equals(name) && !"digest".equals(name)) {
                    result.put(name, safeValue(nested));
                }
            });
            return result;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::safeValue).toList();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
