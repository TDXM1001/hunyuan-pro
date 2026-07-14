package com.hunyuan.sa.bpm.module.evolution.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;

@Service
public class MigrationDataMappingService {
    public Validation validate(String sourceDependenciesJson, String targetDependenciesJson, String mappingJson) {
        MappingPlan plan = parse(mappingJson);
        JSONObject sourceContract = contract(sourceDependenciesJson);
        JSONObject targetContract = contract(targetDependenciesJson);
        List<String> reasons = new ArrayList<>();
        if (sourceContract == null) reasons.add("源定义缺少冻结业务契约");
        if (targetContract == null) reasons.add("目标定义缺少冻结业务契约");
        if (sourceContract == null || targetContract == null) {
            return new Validation(false, List.copyOf(reasons), plan.fieldMappings(),
                    plan.workingDataMappings(), plan.variableMappings());
        }
        validateSchema("表单字段", schema(sourceContract, "fieldSchema"), schema(targetContract, "fieldSchema"),
                plan.fieldMappings(), reasons);
        validateSchema("工作数据", schema(sourceContract, "workingDataSchema"), schema(targetContract, "workingDataSchema"),
                plan.workingDataMappings(), reasons);
        validateUniqueTargets("表单字段", plan.fieldMappings(), reasons);
        validateUniqueTargets("工作数据", plan.workingDataMappings(), reasons);
        validateUniqueTargets("流程变量", plan.variableMappings(), reasons);
        return new Validation(reasons.isEmpty(), List.copyOf(reasons), plan.fieldMappings(),
                plan.workingDataMappings(), plan.variableMappings());
    }

    public Validation validateRuntime(String sourceDependenciesJson, String targetDependenciesJson, String mappingJson,
                                      String formDataJson, String workingDataJson, Map<String, Object> variables) {
        Validation schemaValidation = validate(sourceDependenciesJson, targetDependenciesJson, mappingJson);
        List<String> reasons = new ArrayList<>(schemaValidation.reasons());
        if (!schemaValidation.valid()) return schemaValidation;
        JSONObject sourceContract = contract(sourceDependenciesJson);
        JSONObject targetContract = contract(targetDependenciesJson);
        validatePayload("表单字段", formDataJson, schema(sourceContract, "fieldSchema"),
                schema(targetContract, "fieldSchema"), schemaValidation.fieldMappings(), reasons);
        validatePayload("工作数据", workingDataJson, schema(sourceContract, "workingDataSchema"),
                schema(targetContract, "workingDataSchema"), schemaValidation.workingDataMappings(), reasons);
        Map<String, Object> runtimeVariables = variables == null ? Collections.emptyMap() : variables;
        schemaValidation.variableMappings().forEach((source, target) -> {
            if (!runtimeVariables.containsKey(source)) reasons.add("流程变量 " + source + " 不存在，不能映射到 " + target);
            if (!source.equals(target) && runtimeVariables.containsKey(target)) {
                reasons.add("流程变量目标 " + target + " 已存在，不能被 " + source + " 覆盖");
            }
        });
        return new Validation(reasons.isEmpty(), List.copyOf(reasons), schemaValidation.fieldMappings(),
                schemaValidation.workingDataMappings(), schemaValidation.variableMappings());
    }

    public MappingPlan parse(String mappingJson) {
        JSONObject root = mappingJson == null || mappingJson.isBlank() ? new JSONObject() : JSON.parseObject(mappingJson);
        if (root == null) throw new IllegalArgumentException("数据映射必须是 JSON 对象");
        return new MappingPlan(stringMap(root.getJSONObject("fieldMappings")),
                stringMap(root.getJSONObject("workingDataMappings")),
                stringMap(root.getJSONObject("variableMappings")));
    }

    public String applyJson(String json, Map<String, String> mappings) {
        if (json == null || json.isBlank() || mappings.isEmpty()) return json;
        JSONObject source = JSON.parseObject(json);
        JSONObject target = new JSONObject(new LinkedHashMap<>());
        source.forEach((key, value) -> target.put(mappings.getOrDefault(key, key), value));
        return target.toJSONString();
    }

    private void validateSchema(String label, Map<String, Field> source, Map<String, Field> target,
                                Map<String, String> mappings, List<String> reasons) {
        Map<String, String> reverse = new LinkedHashMap<>();
        source.forEach((sourceKey, sourceField) -> {
            String targetKey = mappings.getOrDefault(sourceKey, sourceKey);
            Field targetField = target.get(targetKey);
            if (targetField == null) {
                reasons.add(label + " " + sourceKey + " 未映射到目标契约");
            } else if (!sourceField.type().equals(targetField.type())) {
                reasons.add(label + " " + sourceKey + " 与目标 " + targetKey + " 类型不兼容");
            } else {
                String previous = reverse.put(targetKey, sourceKey);
                if (previous != null && !previous.equals(sourceKey)) {
                    reasons.add(label + " " + previous + " 与 " + sourceKey + " 同时映射到目标 " + targetKey);
                }
            }
        });
        target.forEach((targetKey, targetField) -> {
            if (targetField.required() && !reverse.containsKey(targetKey)) {
                reasons.add(label + "目标必填字段 " + targetKey + " 没有可迁移来源");
            }
        });
    }

    private JSONObject contract(String dependenciesJson) {
        if (dependenciesJson == null || dependenciesJson.isBlank()) return null;
        JSONObject dependencies = JSON.parseObject(dependenciesJson);
        if (dependencies == null) return null;
        JSONObject reference = dependencies.getJSONObject("businessContract");
        if (reference == null) return null;
        String payload = reference.getString("canonicalPayload");
        return payload == null || payload.isBlank() ? null : JSON.parseObject(payload);
    }

    private Map<String, Field> schema(JSONObject contract, String key) {
        JSONArray array = contract.getJSONArray(key);
        if (array == null) return Collections.emptyMap();
        Map<String, Field> result = new LinkedHashMap<>();
        for (Object value : array) {
            JSONObject field = (JSONObject) value;
            result.put(field.getString("key"), new Field(field.getString("type"), field.getBooleanValue("required")));
        }
        return result;
    }

    private Map<String, String> stringMap(JSONObject object) {
        if (object == null) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<>();
        object.forEach((key, value) -> {
            if (!(value instanceof String target) || target.isBlank()) {
                throw new IllegalArgumentException("数据映射目标必须是非空字符串: " + key);
            }
            result.put(key, target);
        });
        return Map.copyOf(result);
    }

    private void validateUniqueTargets(String label, Map<String, String> mappings, List<String> reasons) {
        Set<String> targets = new HashSet<>();
        mappings.forEach((source, target) -> {
            if (!targets.add(target)) reasons.add(label + "存在多个来源映射到同一目标 " + target);
        });
    }

    private void validatePayload(String label, String payloadJson, Map<String, Field> sourceSchema,
                                 Map<String, Field> targetSchema, Map<String, String> mappings, List<String> reasons) {
        JSONObject source = payloadJson == null || payloadJson.isBlank() ? new JSONObject() : JSON.parseObject(payloadJson);
        if (source == null) source = new JSONObject();
        for (String key : source.keySet()) {
            if (!sourceSchema.containsKey(key)) reasons.add(label + "来源包含契约未声明字段 " + key);
        }
        for (Map.Entry<String, Field> entry : sourceSchema.entrySet()) {
            Object value = source.get(entry.getKey());
            if (entry.getValue().required() && value == null) reasons.add(label + "必填来源 " + entry.getKey() + " 缺失");
            if (value != null && !compatible(value, entry.getValue().type())) {
                reasons.add(label + "来源 " + entry.getKey() + " 的值类型不符合 " + entry.getValue().type());
            }
        }
        JSONObject mapped = JSON.parseObject(applyJson(source.toJSONString(), mappings));
        for (String key : mapped.keySet()) {
            if (!targetSchema.containsKey(key)) reasons.add(label + "目标包含契约未声明字段 " + key);
        }
        for (Map.Entry<String, Field> entry : targetSchema.entrySet()) {
            Object value = mapped.get(entry.getKey());
            if (entry.getValue().required() && value == null) reasons.add(label + "目标必填字段 " + entry.getKey() + " 缺失");
            if (value != null && !compatible(value, entry.getValue().type())) {
                reasons.add(label + "目标 " + entry.getKey() + " 的值类型不符合 " + entry.getValue().type());
            }
        }
    }

    private boolean compatible(Object value, String type) {
        if (type == null) return false;
        return switch (type.toUpperCase()) {
            case "STRING", "TEXT", "DATE", "DATETIME" -> value instanceof String;
            case "BOOLEAN" -> value instanceof Boolean;
            case "INTEGER", "LONG", "EMPLOYEE_ID" -> value instanceof Number number
                    && Math.floor(number.doubleValue()) == number.doubleValue();
            case "NUMBER", "DECIMAL" -> value instanceof Number || canParseDecimal(value);
            case "OBJECT" -> value instanceof JSONObject || value instanceof Map;
            case "ARRAY" -> value instanceof JSONArray || value instanceof List;
            default -> false;
        };
    }

    private boolean canParseDecimal(Object value) {
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private record Field(String type, boolean required) {
    }

    public record MappingPlan(Map<String, String> fieldMappings, Map<String, String> workingDataMappings,
                              Map<String, String> variableMappings) {
    }

    public record Validation(boolean valid, List<String> reasons, Map<String, String> fieldMappings,
                             Map<String, String> workingDataMappings, Map<String, String> variableMappings) {
    }
}
