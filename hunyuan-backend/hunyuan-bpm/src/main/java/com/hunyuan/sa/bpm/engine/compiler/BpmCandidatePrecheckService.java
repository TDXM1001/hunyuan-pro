package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCandidateResolverTypeEnum;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 在发布前给出节点级候选策略摘要，帮助管理员理解审批来源。
 */
@Component
public class BpmCandidatePrecheckService {

    private static final String STATUS_BLOCKING = "BLOCKING";

    private static final String STATUS_READY = "READY";

    private static final String STATUS_RUNTIME_REQUIRED = "RUNTIME_REQUIRED";

    private static final String USER_TASK_TYPE = "userTask";

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public List<BpmDefinitionValidationReportVO.CandidateCheck> precheck(String simpleModelJson, String formSchemaJson) {
        return precheck(simpleModelJson, formSchemaJson, BpmCandidatePrecheckContext.empty());
    }

    public List<BpmDefinitionValidationReportVO.CandidateCheck> precheck(
            String simpleModelJson,
            String formSchemaJson,
            BpmCandidatePrecheckContext context
    ) {
        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = new ArrayList<>();
        JSONObject simpleModelObject = parseObject(simpleModelJson);
        if (simpleModelObject == null) {
            return checks;
        }

        FormSchemaAnalysis formSchemaAnalysis = analyzeFormSchema(formSchemaJson);
        BpmCandidatePrecheckContext safeContext = context == null ? BpmCandidatePrecheckContext.empty() : context;
        FormDataAnalysis formDataAnalysis = analyzeFormData(safeContext.formDataJson());

        JSONArray nodes = simpleModelObject.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            return checks;
        }

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject nodeObject = nodes.getJSONObject(i);
            if (nodeObject == null || !USER_TASK_TYPE.equals(nodeObject.getString("type"))) {
                continue;
            }
            checks.add(buildCandidateCheck(nodeObject, formSchemaAnalysis, safeContext, formDataAnalysis));
        }
        return checks;
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildCandidateCheck(
            JSONObject nodeObject,
            FormSchemaAnalysis formSchemaAnalysis,
            BpmCandidatePrecheckContext context,
            FormDataAnalysis formDataAnalysis
    ) {
        BpmDefinitionValidationReportVO.CandidateCheck check = new BpmDefinitionValidationReportVO.CandidateCheck();
        String nodeKey = firstNonBlank(nodeObject.getString("nodeKey"), nodeObject.getString("id"));
        String nodeName = firstNonBlank(nodeObject.getString("name"), nodeKey, "未命名审批节点");
        String resolverType = firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        );
        check.setNodeKey(nodeKey);
        check.setNodeName(nodeName);
        check.setCandidateResolverType(resolverType);
        check.setCandidateResolverLabel(resolveResolverLabel(resolverType));
        check.setCanResolveNow(Boolean.FALSE);
        check.setRequiresRuntimeFormData(Boolean.FALSE);

        if (StringUtils.isBlank(resolverType)) {
            return block(check, "USER_TASK_CANDIDATE_EMPTY", "candidateResolverType", "审批节点缺少处理人规则");
        }
        if (!isSupportedResolverType(resolverType)) {
            return block(check, "CANDIDATE_RESOLVER_UNSUPPORTED", "candidateResolverType", "当前只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE、START_EMPLOYEE、START_DEPARTMENT_MANAGER、EMPLOYEE_SELECT_AT_START 六类候选人解析类型");
        }

        return switch (resolverType.toUpperCase(Locale.ROOT)) {
            case "EMPLOYEE" -> buildEmployeeCheck(check, nodeObject);
            case "ROLE" -> buildRoleCheck(check, nodeObject);
            case "DEPARTMENT_MANAGER" -> buildDepartmentManagerCheck(check, nodeObject, context);
            case "START_EMPLOYEE" -> buildStartEmployeeCheck(check, context);
            case "START_DEPARTMENT_MANAGER" -> buildStartDepartmentManagerCheck(check, context);
            case "EMPLOYEE_SELECT_AT_START" -> buildEmployeeSelectAtStartCheck(
                    check,
                    nodeObject,
                    formSchemaAnalysis,
                    formDataAnalysis
            );
            default -> block(check, "CANDIDATE_RESOLVER_UNSUPPORTED", "candidateResolverType", "当前候选策略暂不支持预检");
        };
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildEmployeeCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            JSONObject nodeObject
    ) {
        if ("sequential".equalsIgnoreCase(nodeObject.getString("approvalMode"))) {
            return buildMultipleEmployeeCheck(check, nodeObject, "顺序审批", false);
        }
        if ("parallelAll".equalsIgnoreCase(nodeObject.getString("approvalMode"))) {
            return buildMultipleEmployeeCheck(check, nodeObject, "并行全员会签", true);
        }

        Long employeeId = firstNonNull(
                readPositiveLong(nodeObject, "employeeId"),
                readPositiveLong(nodeObject, "assigneeEmployeeId"),
                readPositiveLong(nodeObject, "candidateEmployeeId"),
                readPositiveLong(nodeObject, "userId"),
                readFirstPositiveLong(nodeObject, "employeeIds"),
                readFirstPositiveLong(nodeObject, "assigneeEmployeeIds"),
                readFirstPositiveLong(nodeObject, "candidateEmployeeIds"),
                readFirstPositiveLongCandidateParam(nodeObject)
        );
        if (employeeId == null) {
            return block(check, "EMPLOYEE_ID_MISSING", "employeeId", "审批节点【" + check.getNodeName() + "】未配置指定员工");
        }

        check.setRequiredConfig("employeeId=" + employeeId);
        try {
            bpmOrgIdentityGateway.requireEmployee(employeeId);
        } catch (IllegalArgumentException ex) {
            return block(
                    check,
                    "EMPLOYEE_NOT_FOUND",
                    "employeeId",
                    firstNonBlank(ex.getMessage(), "审批节点【" + check.getNodeName() + "】指定员工不存在")
            );
        }
        return ready(check, "固定审批人为员工 ID " + employeeId);
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildMultipleEmployeeCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            JSONObject nodeObject,
            String modeLabel,
            boolean parallel
    ) {
        MultipleEmployeeAnalysis analysis = analyzeMultipleEmployeeIds(nodeObject, modeLabel);
        if (!analysis.valid()) {
            return block(
                    check,
                    "SEQUENTIAL_EMPLOYEE_IDS_INVALID",
                    "employeeIds",
                    "审批节点【" + check.getNodeName() + "】" + analysis.message()
            );
        }

        for (Long employeeId : analysis.employeeIds()) {
            try {
                bpmOrgIdentityGateway.requireEmployee(employeeId);
            } catch (IllegalArgumentException ex) {
                return block(
                        check,
                        "EMPLOYEE_NOT_FOUND",
                        "employeeIds",
                        firstNonBlank(
                                ex.getMessage(),
                                "审批节点【" + check.getNodeName() + "】顺序审批员工不存在，employeeId=" + employeeId
                        )
                );
            }
        }

        check.setRequiredConfig("employeeIds=" + analysis.employeeIds());
        return ready(
                check,
                parallel
                        ? "指定 " + analysis.employeeIds().size() + " 名并行会签员工，全部通过后继续"
                        : "顺序审批将按配置依次由 " + analysis.employeeIds().size() + " 名员工处理"
        );
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildRoleCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            JSONObject nodeObject
    ) {
        Long roleId = firstNonNull(
                readPositiveLong(nodeObject, "roleId"),
                readFirstPositiveLong(nodeObject, "roleIds"),
                readFirstPositiveLongCandidateParam(nodeObject)
        );
        if (roleId == null) {
            return block(check, "ROLE_ID_MISSING", "roleId", "审批节点【" + check.getNodeName() + "】未配置角色");
        }

        check.setRequiredConfig("roleId=" + roleId);
        List<Long> employeeIds = bpmOrgIdentityGateway.listEmployeeIdsByRoleId(roleId);
        if (employeeIds == null || employeeIds.isEmpty()) {
            return block(
                    check,
                    "ROLE_EMPLOYEE_EMPTY",
                    "roleId",
                    "审批节点【" + check.getNodeName() + "】配置的角色当前没有可用员工"
            );
        }
        int availableEmployeeCount = 0;
        for (Long employeeId : employeeIds) {
            if (employeeId == null) {
                continue;
            }
            try {
                bpmOrgIdentityGateway.requireEmployee(employeeId);
                availableEmployeeCount++;
            } catch (IllegalArgumentException ignored) {
                // 预检只统计当前仍可参与审批的角色成员。
            }
        }
        if (availableEmployeeCount == 0) {
            return block(
                    check,
                    "ROLE_EMPLOYEE_EMPTY",
                    "roleId",
                    "审批节点【" + check.getNodeName() + "】配置的角色当前没有可用员工"
            );
        }
        return ready(check, "审批人将按角色成员解析，当前可解析 " + availableEmployeeCount + " 名员工");
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildDepartmentManagerCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            JSONObject nodeObject,
            BpmCandidatePrecheckContext context
    ) {
        Long departmentId = readPositiveLong(nodeObject, "departmentId");
        if (departmentId != null) {
            return resolveDepartmentManagerCheck(
                    check,
                    departmentId,
                    "departmentId=" + departmentId,
                    "审批人将按指定部门主管解析"
            );
        }
        if (context.startDepartmentId() != null && context.startDepartmentId() > 0) {
            return resolveDepartmentManagerCheck(
                    check,
                    context.startDepartmentId(),
                    "使用模拟发起人部门 departmentId=" + context.startDepartmentId(),
                    "审批人将按模拟发起人部门主管解析"
            );
        }

        check.setRequiredConfig("departmentId 未配置，运行时回退到发起人部门");
        return runtimeRequired(check, "需在发起时结合发起人部门解析主管");
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildStartEmployeeCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            BpmCandidatePrecheckContext context
    ) {
        if (context.startEmployeeId() != null && context.startEmployeeId() > 0) {
            check.setRequiredConfig("模拟发起人 employeeId=" + context.startEmployeeId());
            try {
                bpmOrgIdentityGateway.requireEmployee(context.startEmployeeId());
            } catch (IllegalArgumentException ex) {
                return block(
                        check,
                        "EMPLOYEE_NOT_FOUND",
                        "startEmployeeId",
                        firstNonBlank(ex.getMessage(), "审批节点【" + check.getNodeName() + "】模拟发起人不可用")
                );
            }
            return ready(check, "审批人为模拟发起人本人");
        }

        check.setRequiredConfig("依赖发起人上下文");
        return runtimeRequired(check, "需在发起时按发起人本人解析");
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildStartDepartmentManagerCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            BpmCandidatePrecheckContext context
    ) {
        if (context.startDepartmentId() != null && context.startDepartmentId() > 0) {
            return resolveDepartmentManagerCheck(
                    check,
                    context.startDepartmentId(),
                    "模拟发起人部门 departmentId=" + context.startDepartmentId(),
                    "审批人将按模拟发起人部门主管解析"
            );
        }

        check.setRequiredConfig("依赖发起人部门上下文");
        return runtimeRequired(check, "需在发起时按发起人部门主管解析");
    }

    private BpmDefinitionValidationReportVO.CandidateCheck resolveDepartmentManagerCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            Long departmentId,
            String requiredConfig,
            String readyMessage
    ) {
        check.setRequiredConfig(requiredConfig);
        Long managerEmployeeId = bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId);
        if (managerEmployeeId == null) {
            return block(
                    check,
                    "DEPARTMENT_MANAGER_EMPTY",
                    "departmentId",
                    "审批节点【" + check.getNodeName() + "】未找到部门主管"
            );
        }
        try {
            bpmOrgIdentityGateway.requireEmployee(managerEmployeeId);
        } catch (IllegalArgumentException ex) {
            return block(
                    check,
                    "DEPARTMENT_MANAGER_INVALID",
                    "departmentId",
                    firstNonBlank(ex.getMessage(), "审批节点【" + check.getNodeName() + "】部门主管不可用")
            );
        }
        return ready(check, readyMessage + "，当前解析到员工 ID " + managerEmployeeId);
    }

    private BpmDefinitionValidationReportVO.CandidateCheck buildEmployeeSelectAtStartCheck(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            JSONObject nodeObject,
            FormSchemaAnalysis formSchemaAnalysis,
            FormDataAnalysis formDataAnalysis
    ) {
        String fieldKey = firstNonBlank(
                nodeObject.getString("employeeSelectFieldKey"),
                nodeObject.getString("candidateFieldKey"),
                nodeObject.getString("assigneeFieldKey")
        );
        if (StringUtils.isBlank(fieldKey)) {
            return block(check, "EMPLOYEE_SELECT_FIELD_EMPTY", "employeeSelectFieldKey", "审批节点【" + check.getNodeName() + "】未配置发起时自选审批人字段");
        }

        check.setField(fieldKey);
        check.setRequiredConfig("employeeSelectFieldKey=" + fieldKey);
        check.setRequiresRuntimeFormData(Boolean.TRUE);

        if (!formSchemaAnalysis.valid()) {
            return block(check, "EMPLOYEE_SELECT_FORM_SCHEMA_INVALID", fieldKey, "审批节点【" + check.getNodeName() + "】表单字段定义无法解析");
        }

        JSONObject fieldObject = formSchemaAnalysis.formFields().get(fieldKey);
        if (fieldObject == null) {
            return block(check, "EMPLOYEE_SELECT_FIELD_MISSING", fieldKey, "审批节点【" + check.getNodeName() + "】发起时自选审批人字段【" + fieldKey + "】不存在");
        }
        if (!isEmployeeSelectField(fieldObject)) {
            return block(check, "EMPLOYEE_SELECT_FIELD_TYPE_MISMATCH", fieldKey, "审批节点【" + check.getNodeName() + "】发起时自选审批人字段【" + fieldKey + "】必须是员工单选字段");
        }
        if (formDataAnalysis.provided() && !formDataAnalysis.valid()) {
            return block(check, "EMPLOYEE_SELECT_FORM_DATA_INVALID", fieldKey, "审批节点【" + check.getNodeName() + "】模拟表单 JSON 不合法");
        }

        Long simulatedEmployeeId = readEmployeeIdFromFormData(formDataAnalysis.formData(), fieldKey);
        if (simulatedEmployeeId == null) {
            return runtimeRequired(check, "等待发起表单字段【" + fieldKey + "】提供审批人");
        }
        if (simulatedEmployeeId <= 0) {
            return block(check, "EMPLOYEE_SELECT_FORM_DATA_INVALID", fieldKey, "审批节点【" + check.getNodeName() + "】模拟表单中的审批员工无效");
        }
        try {
            bpmOrgIdentityGateway.requireEmployee(simulatedEmployeeId);
        } catch (IllegalArgumentException ex) {
            return block(
                    check,
                    "EMPLOYEE_NOT_FOUND",
                    fieldKey,
                    firstNonBlank(ex.getMessage(), "审批节点【" + check.getNodeName() + "】模拟表单中的审批员工不可用")
            );
        }

        return ready(check, "模拟表单已解析出审批员工 ID " + simulatedEmployeeId);
    }

    private BpmDefinitionValidationReportVO.CandidateCheck ready(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            String message
    ) {
        check.setStatus(STATUS_READY);
        check.setCanResolveNow(Boolean.TRUE);
        check.setCode(null);
        check.setMessage(message);
        return check;
    }

    private BpmDefinitionValidationReportVO.CandidateCheck runtimeRequired(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            String message
    ) {
        check.setStatus(STATUS_RUNTIME_REQUIRED);
        check.setCanResolveNow(Boolean.FALSE);
        check.setCode(null);
        check.setMessage(message);
        return check;
    }

    private BpmDefinitionValidationReportVO.CandidateCheck block(
            BpmDefinitionValidationReportVO.CandidateCheck check,
            String code,
            String field,
            String message
    ) {
        check.setStatus(STATUS_BLOCKING);
        check.setCanResolveNow(Boolean.FALSE);
        check.setCode(code);
        check.setField(field);
        check.setMessage(message);
        return check;
    }

    private FormDataAnalysis analyzeFormData(String formDataJson) {
        if (StringUtils.isBlank(formDataJson)) {
            return new FormDataAnalysis(false, true, new JSONObject());
        }
        JSONObject formDataObject = parseObject(formDataJson);
        return new FormDataAnalysis(true, formDataObject != null, formDataObject);
    }

    private FormSchemaAnalysis analyzeFormSchema(String formSchemaJson) {
        if (StringUtils.isBlank(formSchemaJson)) {
            return new FormSchemaAnalysis(false, new HashMap<>());
        }

        Object formSchemaObject = parseJson(formSchemaJson);
        if (formSchemaObject == null) {
            return new FormSchemaAnalysis(false, new HashMap<>());
        }

        Map<String, JSONObject> formFields = new HashMap<>();
        collectFields(formSchemaObject, formFields);
        return new FormSchemaAnalysis(true, formFields);
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

    private String resolveResolverLabel(String resolverType) {
        if (StringUtils.isBlank(resolverType)) {
            return "未配置";
        }
        for (BpmCandidateResolverTypeEnum valueEnum : BpmCandidateResolverTypeEnum.values()) {
            if (valueEnum.equalsValue(resolverType)) {
                return valueEnum.getDesc();
            }
        }
        return resolverType;
    }

    private boolean isSupportedResolverType(String resolverType) {
        for (BpmCandidateResolverTypeEnum valueEnum : BpmCandidateResolverTypeEnum.values()) {
            if (valueEnum.equalsValue(resolverType)) {
                return true;
            }
        }
        return false;
    }

    private Long readEmployeeIdFromFormData(JSONObject formDataObject, String fieldKey) {
        if (formDataObject == null) {
            return null;
        }
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

    private MultipleEmployeeAnalysis analyzeMultipleEmployeeIds(JSONObject nodeObject, String modeLabel) {
        Object rawEmployeeIds = nodeObject.get("employeeIds");
        if (!(rawEmployeeIds instanceof JSONArray employeeIds) || employeeIds.size() < 2) {
            return new MultipleEmployeeAnalysis(false, List.of(), modeLabel + "至少配置 2 名员工");
        }

        List<Long> normalizedEmployeeIds = new ArrayList<>();
        Set<Long> uniqueEmployeeIds = new HashSet<>();
        for (Object rawEmployeeId : employeeIds) {
            Long employeeId = readPositiveLongValue(rawEmployeeId);
            if (employeeId == null) {
                return new MultipleEmployeeAnalysis(false, List.of(), modeLabel + "员工 ID 无效");
            }
            if (!uniqueEmployeeIds.add(employeeId)) {
                return new MultipleEmployeeAnalysis(false, List.of(), modeLabel + "存在重复员工");
            }
            normalizedEmployeeIds.add(employeeId);
        }
        return new MultipleEmployeeAnalysis(true, normalizedEmployeeIds, null);
    }

    private Long readPositiveLongValue(Object rawValue) {
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
        Long value = parseLong(String.valueOf(rawValue));
        return value != null && value > 0 ? value : null;
    }

    private Long readPositiveLong(JSONObject nodeObject, String fieldName) {
        if (nodeObject == null || !nodeObject.containsKey(fieldName)) {
            return null;
        }
        Long value = readLong(nodeObject, fieldName);
        return value != null && value > 0 ? value : null;
    }

    private Long readFirstPositiveLong(JSONObject nodeObject, String fieldName) {
        if (nodeObject == null || !nodeObject.containsKey(fieldName)) {
            return null;
        }
        Long value = readFirstLong(nodeObject, fieldName);
        return value != null && value > 0 ? value : null;
    }

    private Long readFirstPositiveLongCandidateParam(JSONObject nodeObject) {
        if (nodeObject == null || !nodeObject.containsKey("candidateParam")) {
            return null;
        }
        Long value = readFirstLongCandidateParam(nodeObject);
        return value != null && value > 0 ? value : null;
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

    private JSONObject parseObject(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        try {
            JSONObject object = JSON.parseObject(jsonText);
            return object == null ? null : object;
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

    private record FormSchemaAnalysis(boolean valid, Map<String, JSONObject> formFields) {
    }

    private record FormDataAnalysis(boolean provided, boolean valid, JSONObject formData) {
    }

    private record MultipleEmployeeAnalysis(boolean valid, List<Long> employeeIds, String message) {
    }
}
