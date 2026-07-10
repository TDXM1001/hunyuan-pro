package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmCandidateResolverTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SimpleModel 草稿校验器。
 */
@Component
public class SimpleModelValidator {

    private static final int MAX_COMPILED_NODE_KEY_LENGTH = 128;

    private static final String USER_TASK_TYPE = "userTask";

    private static final Pattern FLOWABLE_SAFE_NODE_KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final Pattern COMPILER_SEQUENCE_FLOW_ID_PATTERN = Pattern.compile("flow_\\d+");

    private static final Set<String> COMPILER_RESERVED_BPMN_IDS = Set.of(
            "startEvent",
            "endEvent",
            "flow_end"
    );

    /**
     * 校验设计器草稿是否满足当前审批约束。
     */
    public ResponseDTO<String> validate(String simpleModelJson, String startRuleJson) {
        JSONObject simpleModelObject = parseJson(simpleModelJson, "设计器草稿 JSON 不合法");
        if (simpleModelObject == null) {
            return ResponseDTO.userErrorParam("设计器草稿 JSON 不合法");
        }
        JSONObject startRuleObject = parseJson(startRuleJson, "发起规则 JSON 不合法");
        if (startRuleObject == null) {
            return ResponseDTO.userErrorParam("发起规则 JSON 不合法");
        }

        JSONArray nodes = simpleModelObject.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            return ResponseDTO.ok();
        }

        ResponseDTO<String> compiledNodeKeyResponse = validateCompiledNodeKeys(nodes);
        if (!Boolean.TRUE.equals(compiledNodeKeyResponse.getOk())) {
            return compiledNodeKeyResponse;
        }

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject nodeObject = nodes.getJSONObject(i);
            if (nodeObject == null) {
                continue;
            }
            if (!USER_TASK_TYPE.equals(nodeObject.getString("type"))) {
                continue;
            }

            String approvalMode = nodeObject.getString("approvalMode");
            if (StringUtils.isNotBlank(approvalMode)
                    && !"single".equalsIgnoreCase(approvalMode)
                    && !"singleOnly".equalsIgnoreCase(approvalMode)
                    && !"sequential".equalsIgnoreCase(approvalMode)) {
                return ResponseDTO.userErrorParam("当前只支持单人审批或顺序审批");
            }

            String resolverType = firstNonBlank(
                    nodeObject.getString("candidateResolverType"),
                    nodeObject.getString("resolverType")
            );
            if (StringUtils.isNotBlank(resolverType) && !isSupportedResolverType(resolverType)) {
                return ResponseDTO.userErrorParam("当前只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE、START_EMPLOYEE、START_DEPARTMENT_MANAGER、EMPLOYEE_SELECT_AT_START 六类候选人解析类型");
            }
            if ("sequential".equalsIgnoreCase(approvalMode)) {
                ResponseDTO<String> sequentialResponse = validateSequentialEmployeeApproval(nodeObject, resolverType);
                if (!Boolean.TRUE.equals(sequentialResponse.getOk())) {
                    return sequentialResponse;
                }
            }
            if ("EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)
                    && StringUtils.isBlank(firstNonBlank(
                    nodeObject.getString("employeeSelectFieldKey"),
                    nodeObject.getString("candidateFieldKey"),
                    nodeObject.getString("assigneeFieldKey")
            ))) {
                return ResponseDTO.userErrorParam("审批节点【" + firstNonBlank(nodeObject.getString("name"), nodeObject.getString("nodeKey"), nodeObject.getString("id")) + "】未配置发起时自选审批人字段");
            }
        }

        return ResponseDTO.ok();
    }

    private ResponseDTO<String> validateCompiledNodeKeys(JSONArray nodes) {
        Set<String> compiledNodeKeys = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject nodeObject = nodes.getJSONObject(i);
            if (nodeObject == null) {
                continue;
            }

            String nodeKey = firstNonBlank(
                    nodeObject.getString("nodeKey"),
                    nodeObject.getString("id"),
                    "task_" + (i + 1)
            );
            JSONArray employeeIds = nodeObject.getJSONArray("employeeIds");
            if (isSequentialEmployeeApproval(nodeObject, employeeIds)) {
                for (int sequentialIndex = 0; sequentialIndex < employeeIds.size(); sequentialIndex++) {
                    String expandedNodeKey = nodeKey + "_" + (sequentialIndex + 1);
                    ResponseDTO<String> response = addCompiledNodeKey(compiledNodeKeys, expandedNodeKey);
                    if (!Boolean.TRUE.equals(response.getOk())) {
                        return response;
                    }
                }
                continue;
            }

            ResponseDTO<String> response = addCompiledNodeKey(compiledNodeKeys, nodeKey);
            if (!Boolean.TRUE.equals(response.getOk())) {
                return response;
            }
        }
        return ResponseDTO.ok();
    }

    private boolean isSequentialEmployeeApproval(JSONObject nodeObject, JSONArray employeeIds) {
        return USER_TASK_TYPE.equals(nodeObject.getString("type"))
                && "sequential".equalsIgnoreCase(nodeObject.getString("approvalMode"))
                && "EMPLOYEE".equalsIgnoreCase(firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        ))
                && employeeIds != null
                && !employeeIds.isEmpty();
    }

    private ResponseDTO<String> addCompiledNodeKey(Set<String> compiledNodeKeys, String nodeKey) {
        if (!FLOWABLE_SAFE_NODE_KEY_PATTERN.matcher(nodeKey).matches()) {
            return ResponseDTO.userErrorParam(
                    "节点 key【" + nodeKey + "】格式非法，只允许字母、数字、下划线，且必须以字母或下划线开头"
            );
        }
        if (COMPILER_RESERVED_BPMN_IDS.contains(nodeKey)
                || COMPILER_SEQUENCE_FLOW_ID_PATTERN.matcher(nodeKey).matches()) {
            return ResponseDTO.userErrorParam(
                    "节点 key【" + nodeKey + "】属于编译器保留 BPMN ID，请更换节点 key"
            );
        }
        if (nodeKey.length() > MAX_COMPILED_NODE_KEY_LENGTH) {
            return ResponseDTO.userErrorParam(
                    "节点 key【" + nodeKey + "】超过数据库长度上限 128"
            );
        }
        if (!compiledNodeKeys.add(nodeKey)) {
            return ResponseDTO.userErrorParam(
                    "节点 key【" + nodeKey + "】在顺序审批展开后发生冲突"
            );
        }
        return ResponseDTO.ok();
    }

    /**
     * 模拟执行目前先复用校验结果，确保 P0 草稿至少满足规则约束。
     */
    public ResponseDTO<String> simulate(String simpleModelJson, String startRuleJson) {
        ResponseDTO<String> validateResponse = validate(simpleModelJson, startRuleJson);
        if (!Boolean.TRUE.equals(validateResponse.getOk())) {
            return validateResponse;
        }
        return ResponseDTO.okMsg("模拟通过");
    }

    private ResponseDTO<String> validateSequentialEmployeeApproval(JSONObject nodeObject, String resolverType) {
        String nodeName = firstNonBlank(
                nodeObject.getString("name"),
                nodeObject.getString("nodeKey"),
                nodeObject.getString("id"),
                "未命名审批节点"
        );
        if (!"EMPLOYEE".equalsIgnoreCase(resolverType)) {
            return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】顺序审批仅支持指定员工");
        }

        Object rawEmployeeIds = nodeObject.get("employeeIds");
        if (!(rawEmployeeIds instanceof JSONArray employeeIds) || employeeIds.size() < 2) {
            return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】顺序审批至少配置 2 名员工");
        }

        Set<Long> uniqueEmployeeIds = new HashSet<>();
        for (Object rawEmployeeId : employeeIds) {
            Long employeeId = parsePositiveLong(rawEmployeeId);
            if (employeeId == null) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】顺序审批员工 ID 无效");
            }
            if (!uniqueEmployeeIds.add(employeeId)) {
                return ResponseDTO.userErrorParam("审批节点【" + nodeName + "】顺序审批存在重复员工");
            }
        }
        return ResponseDTO.ok();
    }

    private JSONObject parseJson(String jsonText, String errorMessage) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        try {
            return JSON.parseObject(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isSupportedResolverType(String resolverType) {
        for (BpmCandidateResolverTypeEnum valueEnum : BpmCandidateResolverTypeEnum.values()) {
            if (valueEnum.equalsValue(resolverType)) {
                return true;
            }
        }
        return false;
    }

    private Long parsePositiveLong(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number numberValue) {
            Long value = parseExactLong(numberValue);
            if (value == null) {
                return null;
            }
            return value > 0 ? value : null;
        }
        try {
            long value = Long.parseLong(String.valueOf(rawValue).trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseExactLong(Number numberValue) {
        try {
            return new BigDecimal(String.valueOf(numberValue)).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
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
