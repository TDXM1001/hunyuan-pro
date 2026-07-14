package com.hunyuan.sa.bpm.module.evolution.service;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MigrationVariableEvidenceService {

    public BpmMigrationRuntimeGateway.MigrationRuntimeEvidence source(
            Map<String, Object> variables, Map<String, String> mappings) {
        return new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(normalizedMappings(mappings),
                digest(selected(variables, mappings.keySet())), null);
    }

    public BpmMigrationRuntimeGateway.MigrationRuntimeEvidence target(
            Map<String, Object> variables, Map<String, String> mappings) {
        List<String> missing = mappings.values().stream().distinct()
                .filter(name -> !variables.containsKey(name)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("迁移目标变量 " + String.join(",", missing) + " 缺失，需要人工对账");
        }
        return new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(normalizedMappings(mappings), null,
                digest(selected(variables, mappings.values())));
    }

    private Map<String, String> normalizedMappings(Map<String, String> mappings) {
        return Map.copyOf(new TreeMap<>(mappings));
    }

    private Map<String, Object> selected(Map<String, Object> variables, Collection<String> names) {
        Map<String, Object> result = new TreeMap<>();
        names.forEach(name -> {
            if (variables.containsKey(name)) result.put(name, variables.get(name));
        });
        return result;
    }

    private String digest(Map<String, Object> values) {
        return DigestUtils.sha256Hex(JSON.toJSONString(canonicalize(values)));
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new TreeMap<>();
            map.forEach((key, nested) -> result.put(String.valueOf(key), canonicalize(nested)));
            return result;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::canonicalize).toList();
        }
        if (value != null && value.getClass().isArray()) {
            ArrayList<Object> result = new ArrayList<>();
            for (int index = 0; index < java.lang.reflect.Array.getLength(value); index++) {
                result.add(canonicalize(java.lang.reflect.Array.get(value, index)));
            }
            return result;
        }
        return value;
    }
}
