package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将已发布流程节点快照解析为 Flowable 启动变量。
 */
@Service
public class BpmTaskAssignmentResolver {

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes, BpmEmployeeSnapshot startEmployeeSnapshot) {
        Map<String, Object> variables = new HashMap<>();
        if (definitionNodes == null || definitionNodes.isEmpty()) {
            return variables;
        }

        definitionNodes.stream()
                .filter(node -> "userTask".equals(node.getNodeType()))
                .sorted(Comparator.comparing(BpmDefinitionNodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .forEach(node -> {
                    Long assigneeEmployeeId = resolveNodeAssignee(node, startEmployeeSnapshot);
                    variables.put("assignee_" + node.getNodeKey(), String.valueOf(assigneeEmployeeId));
                });
        return variables;
    }

    private Long resolveNodeAssignee(BpmDefinitionNodeEntity node, BpmEmployeeSnapshot startEmployeeSnapshot) {
        JSONObject nodeObject = parseNodeObject(node);
        String resolverType = firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        );
        String nodeName = firstNonBlank(nodeObject.getString("name"), node.getNodeNameSnapshot(), node.getNodeKey());

        if ("START_EMPLOYEE".equalsIgnoreCase(resolverType)) {
            Long employeeId = startEmployeeSnapshot == null ? null : startEmployeeSnapshot.employeeId();
            if (employeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人");
            }
            return employeeId;
        }

        if ("START_DEPARTMENT_MANAGER".equalsIgnoreCase(resolverType)) {
            Long departmentId = startEmployeeSnapshot == null ? null : startEmployeeSnapshot.departmentId();
            if (departmentId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人部门");
            }
            Long managerEmployeeId = bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId);
            if (managerEmployeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人部门主管");
            }
            return managerEmployeeId;
        }

        if ("DEPARTMENT_MANAGER".equalsIgnoreCase(resolverType)) {
            Long departmentId = firstNonNull(
                    readLong(nodeObject, "departmentId"),
                    startEmployeeSnapshot == null ? null : startEmployeeSnapshot.departmentId()
            );
            Long managerEmployeeId = departmentId == null ? null : bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId);
            if (managerEmployeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到部门主管");
            }
            return managerEmployeeId;
        }

        if ("ROLE".equalsIgnoreCase(resolverType)) {
            Long roleId = firstNonNull(
                    readLong(nodeObject, "roleId"),
                    readFirstLong(nodeObject, "roleIds"),
                    readFirstLongCandidateParam(nodeObject)
            );
            if (roleId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未配置角色");
            }
            return bpmOrgIdentityGateway.listEmployeeIdsByRoleId(roleId)
                    .stream()
                    .sorted()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("审批节点【" + nodeName + "】未找到角色成员"));
        }

        Long employeeId = firstNonNull(
                readLong(nodeObject, "employeeId"),
                readLong(nodeObject, "assigneeEmployeeId"),
                readLong(nodeObject, "candidateEmployeeId"),
                readLong(nodeObject, "userId"),
                readFirstLong(nodeObject, "employeeIds"),
                readFirstLong(nodeObject, "assigneeEmployeeIds"),
                readFirstLong(nodeObject, "candidateEmployeeIds"),
                readFirstLongCandidateParam(nodeObject)
        );
        if (employeeId == null) {
            throw new IllegalArgumentException("审批节点【" + nodeName + "】未配置指定员工");
        }
        return employeeId;
    }

    private JSONObject parseNodeObject(BpmDefinitionNodeEntity node) {
        String jsonText = firstNonBlank(node.getAuthoredRuleSnapshotJson(), node.getCompiledNodeSnapshotJson());
        if (StringUtils.isBlank(jsonText)) {
            return new JSONObject();
        }
        try {
            JSONObject nodeObject = JSON.parseObject(jsonText);
            return nodeObject == null ? new JSONObject() : nodeObject;
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private Long readFirstLongCandidateParam(JSONObject nodeObject) {
        Object candidateParam = nodeObject.get("candidateParam");
        if (candidateParam == null) {
            return null;
        }
        if (candidateParam instanceof Number numberValue) {
            return numberValue.longValue();
        }
        String text = String.valueOf(candidateParam).trim();
        if (text.isEmpty()) {
            return null;
        }
        int delimiterIndex = text.indexOf(',');
        if (delimiterIndex >= 0) {
            text = text.substring(0, delimiterIndex);
        }
        return parseLong(text);
    }

    private Long readFirstLong(JSONObject nodeObject, String fieldName) {
        Object rawValue = nodeObject.get(fieldName);
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof JSONArray jsonArray) {
            if (jsonArray.isEmpty()) {
                return null;
            }
            return parseLong(String.valueOf(jsonArray.get(0)));
        }
        if (rawValue instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                return parseLong(String.valueOf(item));
            }
            return null;
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return null;
        }
        int delimiterIndex = text.indexOf(',');
        if (delimiterIndex >= 0) {
            text = text.substring(0, delimiterIndex);
        }
        return parseLong(text);
    }

    private Long readLong(JSONObject nodeObject, String fieldName) {
        Object rawValue = nodeObject.get(fieldName);
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return parseLong(String.valueOf(rawValue));
    }

    private Long parseLong(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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
