package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.ast.BranchNode;
import com.hunyuan.sa.bpm.engine.ast.CopyTaskNode;
import com.hunyuan.sa.bpm.engine.ast.HumanTaskNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessBranch;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;
import com.hunyuan.sa.bpm.engine.ast.RouteCondition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 v1 线性模型和 v2 树形模型解析为统一 AST。
 */
public class ProcessAstParser {

    private static final int CURRENT_SCHEMA_VERSION = 2;

    private static final int DEFAULT_MAX_BRANCH_DEPTH = 3;

    public ProcessAst parse(String modelJson) {
        JSONObject root;
        try {
            root = JSON.parseObject(modelJson);
        } catch (Exception ex) {
            throw new ProcessModelParseException("MODEL_JSON_INVALID", "设计器草稿 JSON 不合法");
        }
        if (root == null) {
            throw new ProcessModelParseException("MODEL_JSON_INVALID", "设计器草稿 JSON 不合法");
        }

        Integer declaredVersion = root.getInteger("schemaVersion");
        int schemaVersion = declaredVersion == null ? 1 : declaredVersion;
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new ProcessModelParseException(
                    "SCHEMA_VERSION_UNSUPPORTED",
                    "不支持的流程模型版本：" + schemaVersion
            );
        }
        JSONObject settings = root.getJSONObject("settings");
        Integer configuredMaxBranchDepth = settings == null ? null : settings.getInteger("maxBranchDepth");
        int maxBranchDepth = configuredMaxBranchDepth == null
                ? DEFAULT_MAX_BRANCH_DEPTH
                : configuredMaxBranchDepth;
        return new ProcessAst(schemaVersion, maxBranchDepth, parseNodes(root.getJSONArray("nodes"), schemaVersion));
    }

    private List<ProcessNode> parseNodes(JSONArray nodes, int schemaVersion) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<ProcessNode> result = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject rawNode = nodes.getJSONObject(index);
            if (rawNode != null) {
                result.add(parseNode(rawNode, schemaVersion, index));
            }
        }
        return result;
    }

    private ProcessNode parseNode(JSONObject rawNode, int schemaVersion, int index) {
        String rawType = StringUtils.defaultIfBlank(rawNode.getString("type"), "userTask");
        ProcessNodeType nodeType = parseNodeType(rawType, schemaVersion);
        String nodeKey = firstNonBlank(rawNode.getString("nodeKey"), rawNode.getString("id"), "task_" + (index + 1));
        String name = firstNonBlank(rawNode.getString("name"), nodeKey);
        Map<String, Object> configuration = toConfiguration(rawNode);

        return switch (nodeType) {
            case USER_TASK, HANDLE_TASK -> new HumanTaskNode(
                    nodeKey,
                    name,
                    nodeType,
                    rawNode.getString("approvalMode"),
                    firstNonBlank(rawNode.getString("candidateResolverType"), rawNode.getString("resolverType")),
                    configuration
            );
            case COPY_TASK -> new CopyTaskNode(
                    nodeKey,
                    name,
                    firstNonBlank(rawNode.getString("candidateResolverType"), rawNode.getString("resolverType")),
                    configuration
            );
            case EXCLUSIVE_BRANCH, PARALLEL_BRANCH, INCLUSIVE_BRANCH -> new BranchNode(
                    nodeKey,
                    name,
                    nodeType,
                    parseBranches(rawNode.getJSONArray("branches"), schemaVersion)
            );
        };
    }

    private List<ProcessBranch> parseBranches(JSONArray branches, int schemaVersion) {
        if (branches == null || branches.isEmpty()) {
            return List.of();
        }
        List<ProcessBranch> result = new ArrayList<>();
        for (int index = 0; index < branches.size(); index++) {
            JSONObject branch = branches.getJSONObject(index);
            if (branch == null) {
                continue;
            }
            result.add(new ProcessBranch(
                    firstNonBlank(branch.getString("branchKey"), "branch_" + (index + 1)),
                    firstNonBlank(branch.getString("name"), "分支" + (index + 1)),
                    branch.getBooleanValue("isDefault"),
                    parseCondition(branch.getJSONObject("condition")),
                    parseNodes(branch.getJSONArray("nodes"), schemaVersion)
            ));
        }
        return result;
    }

    private RouteCondition parseCondition(JSONObject condition) {
        if (condition == null) {
            return null;
        }
        JSONObject parameters = condition.getJSONObject("parameters");
        return new RouteCondition(
                condition.getString("sourceType"),
                condition.getString("fieldKey"),
                condition.getString("valueType"),
                condition.getString("operator"),
                condition.get("compareValue"),
                condition.getString("expressionKey"),
                condition.getInteger("version"),
                parameters == null ? Map.of() : new LinkedHashMap<>(parameters)
        );
    }

    private ProcessNodeType parseNodeType(String rawType, int schemaVersion) {
        if (schemaVersion == 1 && "userTask".equals(rawType)) {
            return ProcessNodeType.USER_TASK;
        }
        try {
            return ProcessNodeType.valueOf(rawType);
        } catch (IllegalArgumentException ex) {
            throw new ProcessModelParseException("NODE_TYPE_UNSUPPORTED", "不支持的流程节点类型：" + rawType);
        }
    }

    private Map<String, Object> toConfiguration(JSONObject node) {
        Map<String, Object> configuration = new LinkedHashMap<>();
        node.forEach((key, value) -> configuration.put(key, value));
        return configuration;
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
