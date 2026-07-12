package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.ast.BranchNode;
import com.hunyuan.sa.bpm.engine.ast.DelayNode;
import com.hunyuan.sa.bpm.engine.ast.ExternalTriggerNode;
import com.hunyuan.sa.bpm.engine.ast.HumanTaskNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessBranch;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;
import com.hunyuan.sa.bpm.engine.ast.RouteCondition;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

/**
 * 对整个 authored AST 执行结构和跨分支语义校验。
 */
@Component
public class ProcessAstValidator {

    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public List<ProcessValidationFinding> validate(
            ProcessAst ast,
            String formSchemaJson,
            BpmRouteExpressionRegistry expressionRegistry
    ) {
        ValidationState state = new ValidationState(
                ast.maxBranchDepth(),
                collectFormFields(formSchemaJson),
                StringUtils.isNotBlank(formSchemaJson)
        );
        validateNodes(ast.nodes(), 0, false, state, expressionRegistry);
        return List.copyOf(state.findings);
    }

    private void validateNodes(
            List<ProcessNode> nodes,
            int branchDepth,
            boolean concurrentContext,
            ValidationState state,
            BpmRouteExpressionRegistry expressionRegistry
    ) {
        for (ProcessNode node : nodes) {
            validateNodeKey(node, state);
            if (node instanceof HumanTaskNode humanTaskNode) {
                validateHumanTask(humanTaskNode, concurrentContext, state);
            }
            if (node instanceof DelayNode delayNode) {
                validateDelayNode(delayNode, state);
            }
            if (node instanceof ExternalTriggerNode externalTriggerNode) {
                validateExternalTriggerNode(externalTriggerNode, state);
            }
            if (node instanceof BranchNode branchNode) {
                validateBranchNode(branchNode, branchDepth, concurrentContext, state, expressionRegistry);
            }
        }
    }

    private void validateNodeKey(ProcessNode node, ValidationState state) {
        if (StringUtils.isBlank(node.nodeKey()) || !SAFE_KEY_PATTERN.matcher(node.nodeKey()).matches()) {
            state.add("NODE_KEY_INVALID", "节点 key 格式非法", node.nodeKey(), null, null, "使用字母或下划线开头的字母数字下划线组合");
            return;
        }
        if (node.nodeKey().length() > 128) {
            state.add("NODE_KEY_TOO_LONG", "节点 key 超过 128 字符", node.nodeKey(), null, null, "缩短节点 key");
        }
        if (!state.nodeKeys.add(node.nodeKey())) {
            state.add("NODE_KEY_DUPLICATE", "节点 key【" + node.nodeKey() + "】在流程中重复", node.nodeKey(), null, null, "为每个节点配置全局唯一 key");
        }
    }

    private void validateHumanTask(HumanTaskNode node, boolean concurrentContext, ValidationState state) {
        validateTaskSlaPolicy(node, state);
        Object rawPermissions = node.configuration().get("fieldPermissions");
        if (!(rawPermissions instanceof JSONArray permissions)) {
            return;
        }
        for (Object rawPermission : permissions) {
            if (!(rawPermission instanceof JSONObject permission)) {
                continue;
            }
            String fieldKey = permission.getString("fieldKey");
            if (state.validateFormReferences
                    && StringUtils.isNotBlank(fieldKey)
                    && !state.formFields.contains(fieldKey)) {
                state.add("FIELD_NOT_FOUND", "节点字段【" + fieldKey + "】不在发布表单中", node.nodeKey(), null, fieldKey, "从当前表单 schema 重新选择字段");
            }
            if (concurrentContext && "EDITABLE".equals(permission.getString("permission"))) {
                state.add(
                        "CONCURRENT_BRANCH_EDITABLE_FORBIDDEN",
                        "并行或包容分支中的人工节点不允许编辑表单字段",
                        node.nodeKey(),
                        null,
                        fieldKey,
                        "将字段改为只读或移到并发分支之外"
                );
            }
        }
    }

    private void validateTaskSlaPolicy(HumanTaskNode node, ValidationState state) {
        Object rawPolicy = node.configuration().get("taskSlaPolicy");
        if (!(rawPolicy instanceof JSONObject policy)) {
            return;
        }
        if (!isPositiveDuration(policy.getString("dueAfter"))) {
            state.add("SLA_DUE_AFTER_INVALID", "任务 SLA 到期时间必须是正数 ISO-8601 duration", node.nodeKey(), null, null, "例如使用 PT2H");
        }
        JSONArray reminders = policy.getJSONArray("reminderSchedule");
        if (reminders != null) {
            for (Object reminder : reminders) {
                if (!(reminder instanceof String value) || !isPositiveDuration(value)) {
                    state.add("SLA_REMINDER_INVALID", "提醒计划必须是正数 ISO-8601 duration", node.nodeKey(), null, null, "例如使用 PT1H");
                    break;
                }
            }
        }
        String action = policy.getString("timeoutAction");
        if (!Set.of("NONE", "REMIND_ONLY", "AUTO_APPROVE", "AUTO_REJECT", "ASSIGN_ADMIN").contains(action)) {
            state.add("SLA_TIMEOUT_ACTION_INVALID", "任务 SLA 超时动作不受支持", node.nodeKey(), null, null, "从受控超时动作中选择");
        }
        if (("AUTO_APPROVE".equals(action) || "AUTO_REJECT".equals(action))
                && StringUtils.isBlank(policy.getString("systemActionComment"))) {
            state.add("SLA_AUTO_TERMINAL_COMMENT_REQUIRED", "自动终态必须配置固定系统意见", node.nodeKey(), null, null, "填写可审计的系统动作意见");
        }
        if (("AUTO_APPROVE".equals(action) || "AUTO_REJECT".equals(action))
                && !"LOW".equals(policy.getString("riskLevel"))) {
            state.add("SLA_AUTO_TERMINAL_RISK_FORBIDDEN", "自动终态首期只允许明确标记为低风险的流程", node.nodeKey(), null, null, "高风险流程改为提醒或人工处理");
        }
        if ("ASSIGN_ADMIN".equals(action)
                && (policy.getLong("adminEmployeeId") == null || policy.getLongValue("adminEmployeeId") <= 0)) {
            state.add("SLA_ADMIN_EMPLOYEE_REQUIRED", "转管理员必须配置有效员工", node.nodeKey(), null, null, "从员工选择器选择管理员");
        }
    }

    private void validateDelayNode(DelayNode node, ValidationState state) {
        if (StringUtils.isNotBlank(node.timezone())) {
            try {
                ZoneId.of(node.timezone());
            } catch (DateTimeException ex) {
                state.add("DELAY_TIMEZONE_INVALID", "延迟节点时区不合法", node.nodeKey(), null, null, "使用 IANA 时区，例如 Asia/Shanghai");
            }
        }
        if ("DURATION".equals(node.mode())) {
            if (!isPositiveDuration(node.value())) {
                state.add("DELAY_DURATION_INVALID", "延迟时长必须是正数 ISO-8601 duration", node.nodeKey(), null, null, "例如使用 P1D 或 PT30M");
            }
            return;
        }
        if ("FIXED_DATETIME".equals(node.mode())) {
            try {
                OffsetDateTime.parse(node.value());
            } catch (DateTimeException | NullPointerException ex) {
                state.add("DELAY_DATETIME_INVALID", "固定延迟时间必须包含时区偏移", node.nodeKey(), null, null, "使用 ISO-8601 offset datetime");
            }
            return;
        }
        if ("FORM_DATETIME".equals(node.mode())) {
            if (StringUtils.isBlank(node.value()) || (state.validateFormReferences && !state.formFields.contains(node.value()))) {
                state.add("DELAY_FORM_FIELD_INVALID", "延迟日期字段不在发布表单中", node.nodeKey(), null, node.value(), "从冻结表单 schema 选择日期字段");
            }
            return;
        }
        state.add("DELAY_MODE_INVALID", "延迟节点模式不受支持", node.nodeKey(), null, null, "从 DURATION、FIXED_DATETIME、FORM_DATETIME 中选择");
    }

    private void validateExternalTriggerNode(ExternalTriggerNode node, ValidationState state) {
        if (StringUtils.isBlank(node.connectorKey()) || !SAFE_KEY_PATTERN.matcher(node.connectorKey()).matches()
                || StringUtils.isBlank(node.operationKey()) || !SAFE_KEY_PATTERN.matcher(node.operationKey()).matches()) {
            state.add("EXTERNAL_REGISTRY_KEY_INVALID", "连接器和操作必须使用已登记的安全 key", node.nodeKey(), null, null, "从连接器目录选择操作");
        }
        if (!Set.of("NO_WAIT", "WAIT_CALLBACK").contains(node.waitMode())) {
            state.add("EXTERNAL_WAIT_MODE_INVALID", "外部触发等待模式不受支持", node.nodeKey(), null, null, "从 NO_WAIT 或 WAIT_CALLBACK 中选择");
        }
        if (containsAnyKey(node.configuration(), Set.of("url", "endpoint", "baseUrl", "baseEndpoint"))) {
            state.add("EXTERNAL_INLINE_ENDPOINT_FORBIDDEN", "流程模型禁止保存外部地址", node.nodeKey(), null, null, "使用登记连接器 key");
        }
        if (containsKeyFragment(node.configuration(), "credential") || containsKeyFragment(node.configuration(), "secret")) {
            state.add("EXTERNAL_INLINE_CREDENTIAL_FORBIDDEN", "流程模型禁止保存凭据或秘密引用", node.nodeKey(), null, null, "由连接器目录绑定安全引用");
        }
        Object timeoutAfter = node.timeoutPolicy().get("timeoutAfter");
        if ("WAIT_CALLBACK".equals(node.waitMode())
                && (!(timeoutAfter instanceof String value) || !isPositiveDuration(value))) {
            state.add("EXTERNAL_TIMEOUT_INVALID", "等待回调必须配置正数 ISO-8601 超时时长", node.nodeKey(), null, null, "例如使用 PT30M");
        }
    }

    private boolean isPositiveDuration(String value) {
        try {
            Duration duration = Duration.parse(value);
            return !duration.isZero() && !duration.isNegative();
        } catch (DateTimeException | NullPointerException ex) {
            return false;
        }
    }

    private boolean containsAnyKey(Map<String, Object> values, Set<String> forbiddenKeys) {
        return values.keySet().stream().anyMatch(forbiddenKeys::contains);
    }

    private boolean containsKeyFragment(Map<String, Object> values, String fragment) {
        return values.keySet().stream().anyMatch(key -> key.toLowerCase().contains(fragment.toLowerCase()));
    }

    private void validateBranchNode(
            BranchNode node,
            int branchDepth,
            boolean concurrentContext,
            ValidationState state,
            BpmRouteExpressionRegistry expressionRegistry
    ) {
        int nextDepth = branchDepth + 1;
        if (nextDepth > state.maxBranchDepth) {
            state.add("BRANCH_DEPTH_EXCEEDED", "分支嵌套深度超过 " + state.maxBranchDepth, node.nodeKey(), null, null, "减少分支嵌套层级");
        }
        if (node.branches().size() < 2) {
            state.add("BRANCH_COUNT_INVALID", "分支节点至少需要两条分支", node.nodeKey(), null, null, "增加至少两条分支");
        }

        long defaultCount = node.branches().stream().filter(ProcessBranch::defaultBranch).count();
        if ((node.type() == ProcessNodeType.EXCLUSIVE_BRANCH || node.type() == ProcessNodeType.INCLUSIVE_BRANCH)
                && defaultCount != 1) {
            state.add(
                    defaultCount == 0 ? "ROUTE_DEFAULT_BRANCH_MISSING" : "ROUTE_DEFAULT_BRANCH_DUPLICATE",
                    defaultCount == 0 ? "路由节点缺少默认分支" : "路由节点只能有一条默认分支",
                    node.nodeKey(), null, null, "保留且只保留一条默认分支"
            );
        }

        boolean childConcurrent = concurrentContext
                || node.type() == ProcessNodeType.PARALLEL_BRANCH
                || node.type() == ProcessNodeType.INCLUSIVE_BRANCH;
        for (ProcessBranch branch : node.branches()) {
            validateBranch(node, branch, state, expressionRegistry);
            validateNodes(branch.nodes(), nextDepth, childConcurrent, state, expressionRegistry);
        }
    }

    private void validateBranch(
            BranchNode node,
            ProcessBranch branch,
            ValidationState state,
            BpmRouteExpressionRegistry expressionRegistry
    ) {
        String branchIdentity = node.nodeKey() + ":" + branch.branchKey();
        if (StringUtils.isBlank(branch.branchKey()) || !SAFE_KEY_PATTERN.matcher(branch.branchKey()).matches()) {
            state.add("BRANCH_KEY_INVALID", "分支 key 格式非法", node.nodeKey(), branch.branchKey(), null, "使用字母或下划线开头的字母数字下划线组合");
        } else if (branch.branchKey().length() > 64) {
            state.add("BRANCH_KEY_TOO_LONG", "分支 key 超过 64 字符", node.nodeKey(), branch.branchKey(), null, "缩短分支 key");
        }
        if (!state.branchKeys.add(branchIdentity)) {
            state.add("BRANCH_KEY_DUPLICATE", "同一路由节点的分支 key 重复", node.nodeKey(), branch.branchKey(), null, "为分支配置唯一 key");
        }
        if (branch.defaultBranch() || node.type() == ProcessNodeType.PARALLEL_BRANCH) {
            return;
        }
        RouteCondition condition = branch.condition();
        if (condition == null) {
            state.add("ROUTE_CONDITION_MISSING", "非默认路由分支缺少条件", node.nodeKey(), branch.branchKey(), null, "配置类型化条件或登记表达式");
            return;
        }
        if ("REGISTERED_EXPRESSION".equals(condition.sourceType())) {
            if (!expressionRegistry.contains(condition.expressionKey(), condition.expressionVersion())) {
                state.add(
                        "ROUTE_EXPRESSION_NOT_REGISTERED",
                        "路由表达式【" + condition.expressionKey() + "@" + condition.expressionVersion() + "】未登记",
                        node.nodeKey(), branch.branchKey(), null, "选择后端已登记的表达式版本"
                );
            }
            return;
        }
        if (state.validateFormReferences
                && (StringUtils.isBlank(condition.fieldKey()) || !state.formFields.contains(condition.fieldKey()))) {
            state.add("ROUTE_FIELD_NOT_FOUND", "路由字段不存在", node.nodeKey(), branch.branchKey(), condition.fieldKey(), "从发布表单 schema 选择字段");
        }
    }

    private Set<String> collectFormFields(String formSchemaJson) {
        Set<String> fields = new HashSet<>();
        try {
            collectFields(JSON.parse(formSchemaJson), fields);
        } catch (Exception ignored) {
            return fields;
        }
        return fields;
    }

    private void collectFields(Object value, Set<String> fields) {
        if (value instanceof JSONArray array) {
            array.forEach(item -> collectFields(item, fields));
            return;
        }
        if (!(value instanceof JSONObject object)) {
            return;
        }
        if (StringUtils.isNotBlank(object.getString("field"))) {
            fields.add(object.getString("field"));
        }
        collectFields(object.get("fields"), fields);
        collectFields(object.get("children"), fields);
    }

    private static final class ValidationState {
        private final int maxBranchDepth;
        private final Set<String> formFields;
        private final boolean validateFormReferences;
        private final Set<String> nodeKeys = new HashSet<>();
        private final Set<String> branchKeys = new HashSet<>();
        private final List<ProcessValidationFinding> findings = new ArrayList<>();

        private ValidationState(int maxBranchDepth, Set<String> formFields, boolean validateFormReferences) {
            this.maxBranchDepth = maxBranchDepth;
            this.formFields = formFields;
            this.validateFormReferences = validateFormReferences;
        }

        private void add(String code, String message, String nodeKey, String branchKey, String fieldKey, String fixHint) {
            findings.add(ProcessValidationFinding.blocking(code, message, nodeKey, branchKey, fieldKey, fixHint));
        }
    }
}
