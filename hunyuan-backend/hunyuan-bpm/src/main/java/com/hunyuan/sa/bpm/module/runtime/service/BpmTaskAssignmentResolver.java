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

import java.math.BigDecimal;
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
        return resolve(definitionNodes, new BpmTaskAssignmentContext(startEmployeeSnapshot, "{}"));
    }

    public Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes, BpmTaskAssignmentContext context) {
        Map<String, Object> variables = new HashMap<>();
        if (definitionNodes == null || definitionNodes.isEmpty()) {
            return variables;
        }

        BpmTaskAssignmentContext safeContext = context == null
                ? new BpmTaskAssignmentContext(null, "{}")
                : context;
        definitionNodes.stream()
                .filter(node -> "userTask".equals(node.getNodeType()))
                .sorted(Comparator.comparing(BpmDefinitionNodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .forEach(node -> {
                    Long assigneeEmployeeId = resolveNodeAssignee(node, safeContext);
                    variables.put("assignee_" + node.getNodeKey(), String.valueOf(assigneeEmployeeId));
                });
        return variables;
    }

    private Long resolveNodeAssignee(BpmDefinitionNodeEntity node, BpmTaskAssignmentContext context) {
        JSONObject nodeObject = parseNodeObject(node);
        BpmEmployeeSnapshot startEmployeeSnapshot = context.startEmployeeSnapshot();
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
            bpmOrgIdentityGateway.requireEmployee(managerEmployeeId);
            return managerEmployeeId;
        }

        if ("EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)) {
            String fieldKey = firstNonBlank(
                    nodeObject.getString("employeeSelectFieldKey"),
                    nodeObject.getString("candidateFieldKey"),
                    nodeObject.getString("assigneeFieldKey")
            );
            if (StringUtils.isBlank(fieldKey)) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未配置发起时自选审批人字段");
            }
            Long employeeId = readEmployeeIdFromFormData(context.formDataJson(), fieldKey);
            if (employeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起时自选审批人");
            }
            if (employeeId <= 0) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】发起时自选审批人无效");
            }
            bpmOrgIdentityGateway.requireEmployee(employeeId);
            return employeeId;
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
            bpmOrgIdentityGateway.requireEmployee(managerEmployeeId);
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
            List<Long> employeeIds = bpmOrgIdentityGateway.listEmployeeIdsByRoleId(roleId);
            if (employeeIds != null) {
                for (Long employeeId : employeeIds.stream().filter(item -> item != null).sorted().toList()) {
                    try {
                        bpmOrgIdentityGateway.requireEmployee(employeeId);
                        return employeeId;
                    } catch (IllegalArgumentException ignored) {
                        // 角色成员可能已禁用或删除，继续尝试下一个可用员工。
                    }
                }
            }
            throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到角色成员");
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
        if (employeeId <= 0) {
            throw new IllegalArgumentException("审批节点【" + nodeName + "】指定员工无效");
        }
        bpmOrgIdentityGateway.requireEmployee(employeeId);
        return employeeId;
    }

    private JSONObject parseNodeObject(BpmDefinitionNodeEntity node) {
        JSONObject nodeObject = parseJsonObject(node.getAuthoredRuleSnapshotJson());
        nodeObject.putAll(parseJsonObject(node.getCompiledNodeSnapshotJson()));
        return nodeObject;
    }

    private JSONObject parseJsonObject(String jsonText) {
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

    private Long readEmployeeIdFromFormData(String formDataJson, String fieldKey) {
        JSONObject formDataObject = parseFormDataObject(formDataJson);
        Object rawValue = formDataObject.get(fieldKey);
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number numberValue) {
            Long value = parseExactLong(numberValue);
            return value == null ? -1L : value;
        }
        if (rawValue instanceof JSONArray || rawValue instanceof Iterable<?>) {
            return -1L;
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.contains(",")) {
            return -1L;
        }
        Long value = parseLong(text);
        return value == null ? -1L : value;
    }

    private JSONObject parseFormDataObject(String formDataJson) {
        if (StringUtils.isBlank(formDataJson)) {
            return new JSONObject();
        }
        try {
            JSONObject formDataObject = JSON.parseObject(formDataJson);
            return formDataObject == null ? new JSONObject() : formDataObject;
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
            return parseExactLong(numberValue);
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
            return parseExactLong(numberValue);
        }
        return parseLong(String.valueOf(rawValue));
    }

    private Long parseExactLong(Number numberValue) {
        try {
            return new BigDecimal(String.valueOf(numberValue)).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return null;
        }
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
