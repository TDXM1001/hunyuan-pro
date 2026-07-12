package com.hunyuan.sa.bpm.engine.graph;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Graph 草稿与模板的完整导出文档，以及复制时的稳定 ID 重分配。
 */
public class GraphDocumentCodec {

    private final GraphCanonicalizer graphCanonicalizer = new GraphCanonicalizer();

    public String export(HunyuanProcessDefinitionGraph graph) {
        graphCanonicalizer.canonicalize(graph);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", graph.schemaVersion());
        document.put("rootScopeId", graph.rootScopeId());
        document.put("scopes", graph.scopes().stream().map(this::scopeDocument).toList());
        document.put("nodes", graph.nodes().stream().map(this::nodeDocument).toList());
        document.put("edges", graph.edges().stream().map(this::edgeDocument).toList());
        document.put("policies", graph.policies());
        return JSON.toJSONString(document);
    }

    public HunyuanProcessDefinitionGraph restore(String documentJson) {
        JSONObject document;
        try {
            document = JSON.parseObject(documentJson);
        } catch (Exception ex) {
            throw new GraphValidationException("Graph 导入文档不是合法 JSON");
        }
        if (document == null) {
            throw new GraphValidationException("Graph 导入文档不能为空");
        }
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                document.getIntValue("schemaVersion"),
                document.getString("rootScopeId"),
                parseScopes(document.getJSONArray("scopes")),
                parseNodes(document.getJSONArray("nodes")),
                parseEdges(document.getJSONArray("edges")),
                toMap(document.getJSONObject("policies"))
        );
        graphCanonicalizer.canonicalize(graph);
        return graph;
    }

    public HunyuanProcessDefinitionGraph importAsNewGraph(String documentJson) {
        return copyWithFreshIds(restore(documentJson));
    }

    /**
     * 恢复数据库中分离保存的 canonical Graph 与布局。
     */
    public HunyuanProcessDefinitionGraph restoreStored(String graphJson, String layoutJson) {
        JSONObject graph;
        JSONObject layouts;
        try {
            graph = JSON.parseObject(graphJson);
            layouts = JSON.parseObject(layoutJson);
        } catch (Exception ex) {
            throw new GraphValidationException("Graph 草稿存储 JSON 不合法");
        }
        if (graph == null) {
            throw new GraphValidationException("Graph 草稿不能为空");
        }
        JSONArray nodes = graph.getJSONArray("nodes");
        if (nodes != null) {
            for (int index = 0; index < nodes.size(); index++) {
                JSONObject node = nodes.getJSONObject(index);
                if (node != null) {
                    JSONObject layout = layouts == null ? null : layouts.getJSONObject(node.getString("nodeId"));
                    node.put("layout", layout == null ? new JSONObject() : layout);
                }
            }
        }
        return restore(graph.toJSONString());
    }

    public HunyuanProcessDefinitionGraph copyWithFreshIds(HunyuanProcessDefinitionGraph source) {
        graphCanonicalizer.canonicalize(source);
        Map<String, String> scopeIds = new HashMap<>();
        for (GraphScope scope : source.scopes()) {
            scopeIds.put(scope.scopeId(), nextId("scope"));
        }
        Map<String, String> nodeIds = new HashMap<>();
        for (GraphNode node : source.nodes()) {
            nodeIds.put(node.nodeId(), nextId("node"));
        }

        List<GraphScope> scopes = source.scopes().stream()
                .map(scope -> new GraphScope(
                        scopeIds.get(scope.scopeId()),
                        scope.parentScopeId() == null ? null : scopeIds.get(scope.parentScopeId()),
                        scope.name()
                ))
                .toList();
        List<GraphNode> nodes = source.nodes().stream()
                .map(node -> new GraphNode(
                        nodeIds.get(node.nodeId()),
                        scopeIds.get(node.scopeId()),
                        node.type(),
                        node.name(),
                        node.properties(),
                        node.layout()
                ))
                .toList();
        List<GraphEdge> edges = source.edges().stream()
                .map(edge -> new GraphEdge(
                        nextId("edge"),
                        scopeIds.get(edge.scopeId()),
                        nodeIds.get(edge.sourceNodeId()),
                        nodeIds.get(edge.targetNodeId()),
                        edge.sourcePort(),
                        edge.properties()
                ))
                .toList();
        HunyuanProcessDefinitionGraph copied = new HunyuanProcessDefinitionGraph(
                source.schemaVersion(),
                scopeIds.get(source.rootScopeId()),
                scopes,
                nodes,
                edges,
                source.policies()
        );
        graphCanonicalizer.canonicalize(copied);
        return copied;
    }

    private List<GraphScope> parseScopes(JSONArray scopes) {
        List<GraphScope> result = new ArrayList<>();
        if (scopes == null) {
            return result;
        }
        for (int index = 0; index < scopes.size(); index++) {
            JSONObject scope = scopes.getJSONObject(index);
            if (scope != null) {
                result.add(new GraphScope(
                        scope.getString("scopeId"),
                        scope.getString("parentScopeId"),
                        scope.getString("name")
                ));
            }
        }
        return result;
    }

    private List<GraphNode> parseNodes(JSONArray nodes) {
        List<GraphNode> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            if (node != null) {
                GraphNodeType type;
                try {
                    type = GraphNodeType.valueOf(node.getString("type"));
                } catch (Exception ex) {
                    throw new GraphValidationException("不支持的 M1 节点类型：" + node.getString("nodeId"));
                }
                result.add(new GraphNode(
                        node.getString("nodeId"),
                        node.getString("scopeId"),
                        type,
                        node.getString("name"),
                        toMap(node.getJSONObject("properties")),
                        toMap(node.getJSONObject("layout"))
                ));
            }
        }
        return result;
    }

    private List<GraphEdge> parseEdges(JSONArray edges) {
        List<GraphEdge> result = new ArrayList<>();
        if (edges == null) {
            return result;
        }
        for (int index = 0; index < edges.size(); index++) {
            JSONObject edge = edges.getJSONObject(index);
            if (edge != null) {
                result.add(new GraphEdge(
                        edge.getString("edgeId"),
                        edge.getString("scopeId"),
                        edge.getString("sourceNodeId"),
                        edge.getString("targetNodeId"),
                        edge.getString("sourcePort"),
                        toMap(edge.getJSONObject("properties"))
                ));
            }
        }
        return result;
    }

    private Map<String, Object> toMap(JSONObject object) {
        return object == null ? Map.of() : new LinkedHashMap<>(object);
    }

    private Map<String, Object> scopeDocument(GraphScope scope) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scopeId", scope.scopeId());
        result.put("parentScopeId", scope.parentScopeId());
        result.put("name", scope.name());
        return result;
    }

    private Map<String, Object> nodeDocument(GraphNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeId", node.nodeId());
        result.put("scopeId", node.scopeId());
        result.put("type", node.type().name());
        result.put("name", node.name());
        result.put("properties", node.properties());
        result.put("layout", node.layout());
        return result;
    }

    private Map<String, Object> edgeDocument(GraphEdge edge) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("edgeId", edge.edgeId());
        result.put("scopeId", edge.scopeId());
        result.put("sourceNodeId", edge.sourceNodeId());
        result.put("targetNodeId", edge.targetNodeId());
        result.put("sourcePort", edge.sourcePort());
        result.put("properties", edge.properties());
        return result;
    }

    private String nextId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
