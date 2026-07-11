package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发布表单数据的运行时结构校验器。
 */
@Component
public class BpmRuntimeFormDataValidator {

    public ResponseDTO<String> validateFullData(String schemaJson, String dataJson) {
        Object schema = parseJson(schemaJson);
        JSONObject data = parseObject(dataJson);
        if (schema == null || data == null) {
            return ResponseDTO.userErrorParam("运行表单数据或 Schema JSON 不合法");
        }
        Map<String, JSONObject> fields = new LinkedHashMap<>();
        collectFields(schema, fields);
        for (String key : data.keySet()) {
            JSONObject field = fields.get(key);
            if (field == null) {
                return ResponseDTO.userErrorParam("字段【" + key + "】不属于当前发布表单");
            }
            ResponseDTO<String> typeResponse = validateBasicType(key, field, data.get(key));
            if (!Boolean.TRUE.equals(typeResponse.getOk())) {
                return typeResponse;
            }
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> validateTaskData(
            String schemaJson,
            String dataJson,
            List<BpmFieldPermissionVO> permissions
    ) {
        ResponseDTO<String> fullResponse = validateFullData(schemaJson, dataJson);
        if (!Boolean.TRUE.equals(fullResponse.getOk())) {
            return fullResponse;
        }
        JSONObject data = parseObject(dataJson);
        if (permissions == null) {
            return ResponseDTO.ok();
        }
        for (BpmFieldPermissionVO permission : permissions) {
            if (!Boolean.TRUE.equals(permission.getRequired())) {
                continue;
            }
            Object value = data.get(permission.getFieldKey());
            if (value == null || value instanceof String text && StringUtils.isBlank(text)) {
                return ResponseDTO.userErrorParam("字段【" + permission.getFieldKey() + "】为当前审批节点必填项");
            }
        }
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> validateBasicType(String fieldKey, JSONObject field, Object value) {
        if (value == null) {
            return ResponseDTO.ok();
        }
        String type = StringUtils.defaultString(field.getString("type")).toLowerCase();
        if ((type.contains("number") || type.contains("inputnumber")) && !(value instanceof Number)) {
            return ResponseDTO.userErrorParam("字段【" + fieldKey + "】必须是数值");
        }
        if (type.contains("employee") && (!(value instanceof Number number) || number.longValue() <= 0)) {
            return ResponseDTO.userErrorParam("字段【" + fieldKey + "】必须是有效员工 ID");
        }
        if ((type.contains("checkbox") || type.contains("multiple")) && !(value instanceof JSONArray)) {
            return ResponseDTO.userErrorParam("字段【" + fieldKey + "】必须是数组");
        }
        return ResponseDTO.ok();
    }

    private void collectFields(Object value, Map<String, JSONObject> fields) {
        if (value instanceof JSONArray array) {
            array.forEach(item -> collectFields(item, fields));
            return;
        }
        if (!(value instanceof JSONObject object)) {
            return;
        }
        String fieldKey = object.getString("field");
        if (StringUtils.isNotBlank(fieldKey)) {
            fields.put(fieldKey, object);
        }
        collectFields(object.get("fields"), fields);
        collectFields(object.get("children"), fields);
    }

    private Object parseJson(String value) {
        try {
            return StringUtils.isBlank(value) ? null : JSON.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private JSONObject parseObject(String value) {
        try {
            return StringUtils.isBlank(value) ? null : JSON.parseObject(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
