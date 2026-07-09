package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.common.enumeration.BpmCandidateResolverTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * SimpleModel 草稿校验器。
 */
@Component
public class SimpleModelValidator {

    private static final String USER_TASK_TYPE = "userTask";

    /**
     * 校验设计器草稿是否满足 P0 约束。
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
                    && !"singleOnly".equalsIgnoreCase(approvalMode)) {
                return ResponseDTO.userErrorParam("P0 只支持单人审批");
            }

            String resolverType = firstNonBlank(
                    nodeObject.getString("candidateResolverType"),
                    nodeObject.getString("resolverType")
            );
            if (StringUtils.isNotBlank(resolverType) && !isSupportedResolverType(resolverType)) {
                return ResponseDTO.userErrorParam("当前只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE、START_EMPLOYEE、START_DEPARTMENT_MANAGER、EMPLOYEE_SELECT_AT_START 六类候选人解析类型");
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
