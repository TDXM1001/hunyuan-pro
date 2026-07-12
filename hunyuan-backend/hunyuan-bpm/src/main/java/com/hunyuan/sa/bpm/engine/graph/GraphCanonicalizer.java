package com.hunyuan.sa.bpm.engine.graph;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * 为发布版本提供忽略布局、字段稳定排序的 Graph 语义表示。
 */
public class GraphCanonicalizer {

    private static final Pattern STABLE_ID_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,127}");

    private static final Set<GraphNodeType> M1_NODE_TYPES = Set.of(
            GraphNodeType.START,
            GraphNodeType.END,
            GraphNodeType.APPROVAL,
            GraphNodeType.HANDLE,
            GraphNodeType.COPY,
            GraphNodeType.CONDITION,
            GraphNodeType.PARALLEL_GATEWAY,
            GraphNodeType.INCLUSIVE_GATEWAY
    );

    public String canonicalize(HunyuanProcessDefinitionGraph graph) {
        validate(graph);
        return JSON.toJSONString(toCanonicalMap(graph));
    }

    public String semanticHash(HunyuanProcessDefinitionGraph graph) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalize(graph).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }

    private Map<String, Object> toCanonicalMap(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> root = new TreeMap<>();
        root.put("schemaVersion", graph.schemaVersion());
        root.put("rootScopeId", graph.rootScopeId());
        root.put("policies", sortedMap(graph.policies()));
        root.put("scopes", graph.scopes().stream()
                .sorted(Comparator.comparing(GraphScope::scopeId))
                .map(scope -> Map.of(
                        "scopeId", scope.scopeId(),
                        "parentScopeId", scope.parentScopeId() == null ? "" : scope.parentScopeId(),
                        "name", scope.name() == null ? "" : scope.name()))
                .toList());
        root.put("nodes", graph.nodes().stream()
                .sorted(Comparator.comparing(GraphNode::nodeId))
                .map(node -> Map.of(
                        "nodeId", node.nodeId(),
                        "scopeId", node.scopeId(),
                        "type", node.type().name(),
                        "name", node.name() == null ? "" : node.name(),
                        "properties", sortedMap(node.properties())))
                .toList());
        root.put("edges", graph.edges().stream()
                .sorted(Comparator.comparing(GraphEdge::edgeId))
                .map(edge -> Map.of(
                        "edgeId", edge.edgeId(),
                        "scopeId", edge.scopeId(),
                        "sourceNodeId", edge.sourceNodeId(),
                        "targetNodeId", edge.targetNodeId(),
                        "sourcePort", edge.sourcePort() == null ? "" : edge.sourcePort(),
                        "properties", sortedMap(edge.properties())))
                .toList());
        return root;
    }

    private Map<String, Object> sortedMap(Map<String, Object> source) {
        Map<String, Object> result = new TreeMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), sortValue(entry.getValue()));
        }
        return result;
    }

    private Object sortValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new TreeMap<>();
            map.forEach((key, nestedValue) -> normalized.put(String.valueOf(key), sortValue(nestedValue)));
            return normalized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::sortValue).toList();
        }
        return value;
    }

    private void validate(HunyuanProcessDefinitionGraph graph) {
        if (graph == null || graph.schemaVersion() != 1) {
            throw new GraphValidationException("不支持的 Graph schemaVersion");
        }
        Set<String> scopeIds = uniqueIds(graph.scopes().stream().map(GraphScope::scopeId).toList(), "作用域");
        if (!scopeIds.contains(graph.rootScopeId())) {
            throw new GraphValidationException("根作用域不存在");
        }
        for (GraphScope scope : graph.scopes()) {
            if (StringUtils.isNotBlank(scope.parentScopeId()) && !scopeIds.contains(scope.parentScopeId())) {
                throw new GraphValidationException("父作用域不存在：" + scope.scopeId());
            }
        }
        Set<String> nodeIds = uniqueIds(graph.nodes().stream().map(GraphNode::nodeId).toList(), "节点");
        Map<String, GraphNode> nodesById = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            requireText(node.scopeId(), "节点作用域不能为空");
            if (!scopeIds.contains(node.scopeId())) {
                throw new GraphValidationException("节点作用域不存在：" + node.nodeId());
            }
            if (node.type() == null || !M1_NODE_TYPES.contains(node.type())) {
                throw new GraphValidationException("不支持的 M1 节点类型：" + node.nodeId());
            }
            nodesById.put(node.nodeId(), node);
        }
        uniqueIds(graph.edges().stream().map(GraphEdge::edgeId).toList(), "连线");
        for (GraphEdge edge : graph.edges()) {
            if (!scopeIds.contains(edge.scopeId())) {
                throw new GraphValidationException("连线作用域不存在：" + edge.edgeId());
            }
            GraphNode source = nodesById.get(edge.sourceNodeId());
            GraphNode target = nodesById.get(edge.targetNodeId());
            if (source == null || target == null) {
                throw new GraphValidationException("连线端点不存在：" + edge.edgeId());
            }
            if (!edge.scopeId().equals(source.scopeId()) || !edge.scopeId().equals(target.scopeId())) {
                throw new GraphValidationException("跨作用域连线：" + edge.edgeId());
            }
        }
    }

    private Set<String> uniqueIds(List<String> ids, String label) {
        Set<String> result = new HashSet<>();
        for (String id : ids) {
            requireText(id, label + " ID 不能为空");
            if (!STABLE_ID_PATTERN.matcher(id).matches()) {
                throw new GraphValidationException(label + " ID 非法：" + id);
            }
            if (!result.add(id)) {
                throw new GraphValidationException("重复" + label + " ID：" + id);
            }
        }
        return result;
    }

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new GraphValidationException(message);
        }
    }
}
