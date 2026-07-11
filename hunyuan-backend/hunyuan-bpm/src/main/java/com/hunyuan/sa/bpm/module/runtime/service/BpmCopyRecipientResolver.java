package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 按发布时冻结的抄送节点快照解析接收人。
 */
@Service
public class BpmCopyRecipientResolver {

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public List<Long> resolve(BpmInstanceEntity instance, BpmDefinitionNodeEntity node) {
        JSONObject snapshot = parseSnapshot(node);
        String resolverType = firstNonBlank(
                snapshot.getString("candidateResolverType"),
                snapshot.getString("resolverType")
        );
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();

        if ("ROLE".equalsIgnoreCase(resolverType)) {
            for (Long roleId : readIds(snapshot, "roleIds", "roleId", "candidateParam")) {
                List<Long> roleMembers = bpmOrgIdentityGateway.listEmployeeIdsByRoleId(roleId);
                if (roleMembers != null) {
                    candidates.addAll(roleMembers);
                }
            }
        } else if ("DEPARTMENT_MANAGER".equalsIgnoreCase(resolverType)) {
            Long departmentId = firstId(snapshot, "departmentId");
            if (departmentId == null) {
                departmentId = instance.getStartDepartmentIdSnapshot();
            }
            addIfPresent(candidates, departmentId == null
                    ? null
                    : bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId));
        } else if ("START_EMPLOYEE".equalsIgnoreCase(resolverType)) {
            addIfPresent(candidates, instance.getStartEmployeeId());
        } else if ("START_DEPARTMENT_MANAGER".equalsIgnoreCase(resolverType)) {
            Long departmentId = instance.getStartDepartmentIdSnapshot();
            addIfPresent(candidates, departmentId == null
                    ? null
                    : bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId));
        } else if ("EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)) {
            String fieldKey = firstNonBlank(
                    snapshot.getString("employeeSelectFieldKey"),
                    snapshot.getString("candidateFieldKey"),
                    snapshot.getString("assigneeFieldKey")
            );
            JSONObject formData = parseObject(instance.getCurrentFormDataSnapshotJson());
            collectIds(formData.get(fieldKey), candidates);
        } else {
            candidates.addAll(readIds(
                    snapshot,
                    "employeeIds",
                    "employeeId",
                    "assigneeEmployeeIds",
                    "assigneeEmployeeId",
                    "candidateEmployeeIds",
                    "candidateEmployeeId",
                    "candidateParam"
            ));
        }

        LinkedHashSet<Long> available = new LinkedHashSet<>();
        for (Long employeeId : candidates) {
            if (employeeId == null || employeeId <= 0) {
                continue;
            }
            try {
                bpmOrgIdentityGateway.requireEmployee(employeeId);
                available.add(employeeId);
            } catch (IllegalArgumentException ignored) {
                // 角色等动态来源允许跳过已停用成员，最终空集合仍会失败关闭。
            }
        }
        if (available.isEmpty()) {
            throw new IllegalArgumentException("COPY_RECIPIENT_EMPTY：抄送节点【"
                    + firstNonBlank(node.getNodeNameSnapshot(), node.getNodeKey()) + "】未解析到可用接收人");
        }
        return List.copyOf(available);
    }

    private JSONObject parseSnapshot(BpmDefinitionNodeEntity node) {
        JSONObject snapshot = parseObject(node.getAuthoredRuleSnapshotJson());
        snapshot.putAll(parseObject(node.getCompiledNodeSnapshotJson()));
        return snapshot;
    }

    private JSONObject parseObject(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return new JSONObject();
        }
        try {
            JSONObject result = JSON.parseObject(jsonText);
            return result == null ? new JSONObject() : result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("COPY_SNAPSHOT_INVALID：抄送节点冻结快照不是合法 JSON", ex);
        }
    }

    private List<Long> readIds(JSONObject source, String... keys) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (String key : keys) {
            collectIds(source.get(key), result);
        }
        return List.copyOf(result);
    }

    private Long firstId(JSONObject source, String... keys) {
        List<Long> ids = readIds(source, keys);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void collectIds(Object rawValue, LinkedHashSet<Long> target) {
        if (rawValue == null) {
            return;
        }
        if (rawValue instanceof JSONArray array) {
            array.forEach(item -> collectIds(item, target));
            return;
        }
        if (rawValue instanceof Collection<?> collection) {
            collection.forEach(item -> collectIds(item, target));
            return;
        }
        for (String part : String.valueOf(rawValue).split(",")) {
            try {
                target.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
                // 非法候选值不进入运行事实，最终空集合会给出统一错误。
            }
        }
    }

    private void addIfPresent(LinkedHashSet<Long> target, Long value) {
        if (value != null) {
            target.add(value);
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
