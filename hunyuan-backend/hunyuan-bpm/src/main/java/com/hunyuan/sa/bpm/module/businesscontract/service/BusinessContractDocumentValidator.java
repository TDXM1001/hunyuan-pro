package com.hunyuan.sa.bpm.module.businesscontract.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class BusinessContractDocumentValidator {

    private static final Set<String> TYPES = Set.of("STRING", "DECIMAL", "INTEGER", "BOOLEAN", "EMPLOYEE_ID");
    private static final Set<String> SENSITIVITIES = Set.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED");
    private static final Set<String> CHANGE_MODES = Set.of("LOCKED", "VERSIONED", "RESTART_REQUIRED", "FIELD_CONTROLLED");

    void validate(Integer schemaVersion, String contractJson) {
        if (!Integer.valueOf(1).equals(schemaVersion)) {
            throw new IllegalArgumentException("当前只支持业务契约 schemaVersion=1");
        }
        JSONObject document;
        try {
            document = JSON.parseObject(contractJson);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("业务契约 JSON 不合法", ex);
        }
        if (document == null) {
            throw new IllegalArgumentException("业务契约 JSON 必须为对象");
        }
        requireText(document.getString("sourceSystem"), "业务契约 sourceSystem 不能为空");
        requireText(document.getString("businessType"), "业务契约 businessType 不能为空");
        validateBusinessKeyRule(document.getJSONObject("businessKeyRule"));
        Set<String> subjectFields = validateFields(document.getJSONArray("fieldSchema"), "fieldSchema");
        Set<String> routingFacts = validateFields(document.getJSONArray("routingFacts"), "routingFacts");
        Set<String> workingFields = validateFields(document.getJSONArray("workingDataSchema"), "workingDataSchema");
        if (!java.util.Collections.disjoint(subjectFields, routingFacts)
                || !java.util.Collections.disjoint(subjectFields, workingFields)
                || !java.util.Collections.disjoint(routingFacts, workingFields)) {
            throw new IllegalArgumentException("审批对象、路由事实和工作数据字段键必须相互隔离");
        }
        validateChangePolicy(document.getJSONObject("changePolicy"), workingFields);
        JSONObject attachmentRules = document.getJSONObject("attachmentRules");
        if (attachmentRules == null || attachmentRules.getInteger("maxCount") == null
                || attachmentRules.getIntValue("maxCount") < 0) {
            throw new IllegalArgumentException("attachmentRules.maxCount 必须为非负整数");
        }
        JSONObject detailLayout = document.getJSONObject("detailLayout");
        if (detailLayout == null || detailLayout.getJSONArray("sections") == null
                || detailLayout.getJSONArray("sections").isEmpty()) {
            throw new IllegalArgumentException("detailLayout.sections 不能为空");
        }
    }

    private void validateBusinessKeyRule(JSONObject rule) {
        if (rule == null || StringUtils.isBlank(rule.getString("pattern"))) {
            throw new IllegalArgumentException("businessKeyRule.pattern 不能为空");
        }
        try {
            Pattern.compile(rule.getString("pattern"));
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("businessKeyRule.pattern 不是合法正则表达式", ex);
        }
    }

    private Set<String> validateFields(JSONArray fields, String label) {
        if (fields == null) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        Set<String> keys = new HashSet<>();
        for (int index = 0; index < fields.size(); index++) {
            JSONObject field = fields.getJSONObject(index);
            String key = field == null ? null : field.getString("key");
            if (StringUtils.isBlank(key) || !keys.add(key)) {
                throw new IllegalArgumentException(label + " 包含空或重复字段键：" + key);
            }
            if (!TYPES.contains(field.getString("type"))) {
                throw new IllegalArgumentException(label + " 字段类型不受支持：" + key);
            }
            if (!SENSITIVITIES.contains(field.getString("sensitivity"))) {
                throw new IllegalArgumentException(label + " 敏感级别不受支持：" + key);
            }
        }
        return keys;
    }

    private void validateChangePolicy(JSONObject policy, Set<String> workingFields) {
        String mode = policy == null ? null : policy.getString("mode");
        if (!CHANGE_MODES.contains(mode)) {
            throw new IllegalArgumentException("changePolicy.mode 不受支持");
        }
        JSONArray editableFields = policy.getJSONArray("editableFields");
        if ("FIELD_CONTROLLED".equals(mode) && editableFields == null) {
            throw new IllegalArgumentException("FIELD_CONTROLLED 必须声明 editableFields");
        }
        if (editableFields != null) {
            for (Object rawField : editableFields) {
                String field = String.valueOf(rawField);
                if (!workingFields.contains(field)) {
                    throw new IllegalArgumentException("可编辑字段不属于工作数据 schema：" + field);
                }
            }
        }
    }

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
