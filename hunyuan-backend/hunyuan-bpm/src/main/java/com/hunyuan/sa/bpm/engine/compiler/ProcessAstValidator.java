package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.ast.BranchNode;
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
import java.util.Set;
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
