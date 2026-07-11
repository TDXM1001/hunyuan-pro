package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建员工任务的服务端授权表单上下文。
 */
@Service
public class BpmTaskFormContextService {

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    public BpmTaskFormContextVO buildForEmployeeTask(BpmTaskEntity task, BpmInstanceEntity instance) {
        BpmDefinitionEntity definition = bpmDefinitionDao.selectById(task.getDefinitionId());
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectById(task.getDefinitionNodeId());
        if (definition == null || node == null || StringUtils.isBlank(definition.getFormSchemaSnapshotJson())) {
            return null;
        }

        Object schema;
        JSONObject compiledNode;
        JSONObject fullData;
        try {
            schema = JSON.parse(definition.getFormSchemaSnapshotJson());
            compiledNode = JSON.parseObject(node.getCompiledNodeSnapshotJson());
            fullData = JSON.parseObject(StringUtils.defaultIfBlank(instance.getCurrentFormDataSnapshotJson(), "{}"));
        } catch (RuntimeException ex) {
            return null;
        }
        if (schema == null || compiledNode == null || fullData == null) {
            return null;
        }

        Map<String, JSONObject> schemaFields = new LinkedHashMap<>();
        collectFields(schema, schemaFields);
        Map<String, BpmFieldPermissionVO> permissionMap = buildPermissionMap(compiledNode, schemaFields);
        if (permissionMap == null) {
            return null;
        }

        Object visibleSchema = filterSchema(JSON.parse(JSON.toJSONString(schema)), permissionMap);
        JSONObject visibleData = new JSONObject(true);
        List<BpmFieldPermissionVO> visiblePermissions = new ArrayList<>();
        for (Map.Entry<String, BpmFieldPermissionVO> entry : permissionMap.entrySet()) {
            if ("HIDDEN".equals(entry.getValue().getPermission())) {
                continue;
            }
            visiblePermissions.add(entry.getValue());
            if (fullData.containsKey(entry.getKey())) {
                visibleData.put(entry.getKey(), fullData.get(entry.getKey()));
            }
        }

        BpmTaskFormContextVO context = new BpmTaskFormContextVO();
        context.setDataVersion(instance.getFormDataVersion() == null ? 1L : instance.getFormDataVersion());
        context.setFormSchemaJson(JSON.toJSONString(visibleSchema));
        context.setFormDataJson(JSON.toJSONString(visibleData));
        context.setPermissions(visiblePermissions);
        return context;
    }

    public String getPublishedFormSchemaJson(BpmTaskEntity task) {
        BpmDefinitionEntity definition = bpmDefinitionDao.selectById(task.getDefinitionId());
        return definition == null ? null : definition.getFormSchemaSnapshotJson();
    }

    private Map<String, BpmFieldPermissionVO> buildPermissionMap(
            JSONObject compiledNode,
            Map<String, JSONObject> schemaFields
    ) {
        Map<String, BpmFieldPermissionVO> permissions = new LinkedHashMap<>();
        for (String fieldKey : schemaFields.keySet()) {
            permissions.put(fieldKey, permission(fieldKey, "READONLY", false));
        }
        JSONArray configured = compiledNode.getJSONArray("fieldPermissions");
        if (configured == null) {
            return permissions;
        }
        for (int index = 0; index < configured.size(); index++) {
            JSONObject item = configured.getJSONObject(index);
            if (item == null) {
                return null;
            }
            String fieldKey = item.getString("fieldKey");
            String mode = item.getString("permission");
            if (!permissions.containsKey(fieldKey)
                    || !("READONLY".equals(mode) || "EDITABLE".equals(mode) || "HIDDEN".equals(mode))) {
                return null;
            }
            permissions.put(fieldKey, permission(fieldKey, mode, item.getBooleanValue("required")));
        }
        return permissions;
    }

    private BpmFieldPermissionVO permission(String fieldKey, String mode, boolean required) {
        BpmFieldPermissionVO permission = new BpmFieldPermissionVO();
        permission.setFieldKey(fieldKey);
        permission.setPermission(mode);
        permission.setRequired(required);
        return permission;
    }

    private Object filterSchema(Object value, Map<String, BpmFieldPermissionVO> permissions) {
        if (value instanceof JSONArray array) {
            JSONArray filtered = new JSONArray();
            for (Object item : array) {
                Object filteredItem = filterSchema(item, permissions);
                if (filteredItem != null) {
                    filtered.add(filteredItem);
                }
            }
            return filtered;
        }
        if (!(value instanceof JSONObject object)) {
            return value;
        }
        String fieldKey = object.getString("field");
        if (StringUtils.isNotBlank(fieldKey)
                && (permissions.get(fieldKey) == null
                || "HIDDEN".equals(permissions.get(fieldKey).getPermission()))) {
            return null;
        }
        if (object.containsKey("fields")) {
            object.put("fields", filterSchema(object.get("fields"), permissions));
        }
        if (object.containsKey("children")) {
            object.put("children", filterSchema(object.get("children"), permissions));
        }
        return object;
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
}
