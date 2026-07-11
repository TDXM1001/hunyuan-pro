package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 发布期校验 simple model 中的发起自选审批人与表单字段是否一致。
 */
@Component
public class BpmSimpleModelPublishValidator {

    private static final String USER_TASK_TYPE = "userTask";

    public ResponseDTO<String> validate(String simpleModelJson, String formSchemaJson) {
        JSONObject simpleModelObject = parseObject(simpleModelJson);
        if (simpleModelObject == null) {
            return ResponseDTO.userErrorParam("设计器草稿 JSON 不合法");
        }

        Object formSchemaObject = parseJson(formSchemaJson);
        if (formSchemaObject == null) {
            return ResponseDTO.userErrorParam("表单 Schema JSON 不合法");
        }

        Map<String, JSONObject> formFields = new HashMap<>();
        collectFields(formSchemaObject, formFields);

        JSONArray nodes = simpleModelObject.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            return ResponseDTO.ok();
        }

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject nodeObject = nodes.getJSONObject(i);
            if (nodeObject == null || !USER_TASK_TYPE.equals(nodeObject.getString("type"))) {
                continue;
            }
            ResponseDTO<String> fieldPermissionResponse = validateFieldPermissions(nodeObject, formFields);
            if (!Boolean.TRUE.equals(fieldPermissionResponse.getOk())) {
                return fieldPermissionResponse;
            }
            String resolverType = firstNonBlank(
                    nodeObject.getString("candidateResolverType"),
                    nodeObject.getString("resolverType")
            );
            if (!"EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)) {
                continue;
            }

            String fieldKey = firstNonBlank(
                    nodeObject.getString("employeeSelectFieldKey"),
                    nodeObject.getString("candidateFieldKey"),
                    nodeObject.getString("assigneeFieldKey")
            );
            String nodeName = firstNonBlank(nodeObject.getString("name"), nodeObject.getString("nodeKey"), nodeObject.getString("id"));
            if (StringUtils.isBlank(fieldKey)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】未配置发起时自选审批人字段");
            }

            JSONObject fieldObject = formFields.get(fieldKey);
            if (fieldObject == null) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】发起时自选审批人字段【" + fieldKey + "】不存在");
            }
            if (!isEmployeeSelectField(fieldObject)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】发起时自选审批人字段【" + fieldKey + "】必须是员工单选字段");
            }
        }

        return ResponseDTO.ok();
    }

    private ResponseDTO<String> validateFieldPermissions(
            JSONObject nodeObject,
            Map<String, JSONObject> formFields
    ) {
        JSONArray permissions = nodeObject.getJSONArray("fieldPermissions");
        if (permissions == null || permissions.isEmpty()) {
            return ResponseDTO.ok();
        }

        String nodeName = firstNonBlank(
                nodeObject.getString("name"),
                nodeObject.getString("nodeKey"),
                nodeObject.getString("id")
        );
        boolean parallelAll = "parallelAll".equalsIgnoreCase(nodeObject.getString("approvalMode"));
        Set<String> configuredFields = new HashSet<>();
        for (int index = 0; index < permissions.size(); index++) {
            JSONObject permissionObject = permissions.getJSONObject(index);
            if (permissionObject == null) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】字段权限配置不合法");
            }
            String fieldKey = permissionObject.getString("fieldKey");
            if (StringUtils.isBlank(fieldKey)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】字段权限缺少 fieldKey");
            }
            if (!configuredFields.add(fieldKey)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】字段【" + fieldKey + "】权限配置重复");
            }
            if (!formFields.containsKey(fieldKey)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】字段权限引用的字段【" + fieldKey + "】不存在");
            }

            String permission = permissionObject.getString("permission");
            if (!Set.of("READONLY", "EDITABLE", "HIDDEN").contains(permission)) {
                return ResponseDTO.userErrorParam(
                        "审批节点【" + nodeName + "】字段【" + fieldKey
                                + "】权限只允许 READONLY、EDITABLE 或 HIDDEN"
                );
            }
            if (permissionObject.getBooleanValue("required") && !"EDITABLE".equals(permission)) {
                return ResponseDTO.userErrorParam(
                        "审批节点【" + nodeName + "】字段【" + fieldKey + "】设为节点必填时必须可编辑"
                );
            }
            if (parallelAll && "EDITABLE".equals(permission)) {
                return ResponseDTO.userErrorParam(
                        "审批节点【" + nodeName + "】并行全员会签字段【" + fieldKey + "】不允许配置为可编辑"
                );
            }
        }
        return ResponseDTO.ok();
    }

    private void collectFields(Object value, Map<String, JSONObject> formFields) {
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                collectFields(item, formFields);
            }
            return;
        }
        if (!(value instanceof JSONObject object)) {
            return;
        }

        String fieldKey = object.getString("field");
        if (StringUtils.isNotBlank(fieldKey)) {
            formFields.put(fieldKey, object);
        }

        collectFields(object.get("children"), formFields);
        collectFields(object.get("fields"), formFields);
    }

    private boolean isEmployeeSelectField(JSONObject fieldObject) {
        JSONObject props = fieldObject.getJSONObject("props");
        String type = firstNonBlank(
                fieldObject.getString("type"),
                fieldObject.getString("component"),
                props == null ? null : props.getString("type"),
                props == null ? null : props.getString("component")
        );
        return "employee".equalsIgnoreCase(type) || "employeeSelect".equalsIgnoreCase(type);
    }

    private JSONObject parseObject(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        try {
            return JSON.parseObject(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object parseJson(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        try {
            return JSON.parse(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
