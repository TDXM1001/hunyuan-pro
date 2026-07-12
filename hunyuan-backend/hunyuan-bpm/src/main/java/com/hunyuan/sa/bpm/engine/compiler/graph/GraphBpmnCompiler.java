package com.hunyuan.sa.bpm.engine.compiler.graph;

import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将经过预检的正式 Graph 编译为受控 BPMN，条件表达式只引用编译器生成的变量名。
 */
public class GraphBpmnCompiler {

    private static final String DEFAULT_PORT = "default";

    private final GraphPublicationPrecheck precheck = new GraphPublicationPrecheck();

    public GraphCompiledArtifact compile(String processKey, String processName, HunyuanProcessDefinitionGraph graph) {
        GraphPublicationPrecheckResult result = precheck.check(graph);
        if (!result.pass()) {
            throw new GraphCompilationException(result.findings().get(0).message());
        }

        Map<String, GraphNode> nodesById = new HashMap<>();
        Map<String, List<GraphEdge>> outgoingByNodeId = new HashMap<>();
        Map<String, String> compiledNodeIds = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            nodesById.put(node.nodeId(), node);
            outgoingByNodeId.put(node.nodeId(), new ArrayList<>());
            compiledNodeIds.put(node.nodeId(), compiledNodeId(node));
        }
        for (GraphEdge edge : graph.edges()) {
            outgoingByNodeId.get(edge.sourceNodeId()).add(edge);
        }

        StringBuilder xml = new StringBuilder();
        List<GraphCompiledElementMapping> mappings = new ArrayList<>();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"http://hunyuan.sa/bpm\">");
        xml.append("<process id=\"").append(escapeXml(processKey)).append("\" name=\"")
                .append(escapeXml(processName)).append("\" isExecutable=\"true\">");
        for (GraphNode node : graph.nodes()) {
            String compiledId = compiledNodeIds.get(node.nodeId());
            String compiledType = appendNode(xml, node, compiledId, outgoingByNodeId.get(node.nodeId()));
            mappings.add(new GraphCompiledElementMapping(node.nodeId(), "NODE", compiledId, compiledType));
        }
        for (GraphEdge edge : graph.edges()) {
            String compiledId = edgeId(edge.edgeId());
            appendEdge(xml, edge, compiledId, nodesById.get(edge.sourceNodeId()), compiledNodeIds);
            mappings.add(new GraphCompiledElementMapping(edge.edgeId(), "EDGE", compiledId, "sequenceFlow"));
        }
        xml.append("</process></definitions>");
        return new GraphCompiledArtifact(xml.toString(), List.copyOf(mappings));
    }

    private String appendNode(StringBuilder xml, GraphNode node, String compiledId, List<GraphEdge> outgoing) {
        String tag = switch (node.type()) {
            case START -> "startEvent";
            case END -> "endEvent";
            case APPROVAL -> "receiveTask";
            case HANDLE -> "userTask";
            case COPY -> "serviceTask";
            case CONDITION -> "exclusiveGateway";
            case PARALLEL_GATEWAY -> "parallelGateway";
            case INCLUSIVE_GATEWAY -> "inclusiveGateway";
            default -> throw new GraphCompilationException("不支持的 M1 节点类型：" + node.nodeId());
        };
        xml.append("<").append(tag).append(" id=\"").append(escapeXml(compiledId))
                .append("\" name=\"").append(escapeXml(node.name())).append("\"");
        GraphEdge defaultEdge = defaultEdge(node, outgoing);
        if (defaultEdge != null) {
            xml.append(" default=\"").append(escapeXml(edgeId(defaultEdge.edgeId()))).append("\"");
        }
        if (node.type() == GraphNodeType.COPY) {
            xml.append(" flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\"");
        }
        if (node.type() == GraphNodeType.APPROVAL) {
            xml.append("><extensionElements><flowable:executionListener event=\"start\" ")
                    .append("delegateExpression=\"${hunyuanApprovalStageControl}\"/>")
                    .append("</extensionElements></receiveTask>");
            return tag;
        }
        xml.append("/>");
        return tag;
    }

    private void appendEdge(
            StringBuilder xml,
            GraphEdge edge,
            String compiledId,
            GraphNode sourceNode,
            Map<String, String> compiledNodeIds
    ) {
        xml.append("<sequenceFlow id=\"").append(escapeXml(compiledId))
                .append("\" sourceRef=\"").append(escapeXml(compiledNodeIds.get(edge.sourceNodeId())))
                .append("\" targetRef=\"").append(escapeXml(compiledNodeIds.get(edge.targetNodeId())))
                .append("\"");
        String conditionExpression = generatedConditionExpression(sourceNode, edge);
        if (conditionExpression == null) {
            xml.append("/>");
            return;
        }
        xml.append("><conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[")
                .append(conditionExpression)
                .append("]]></conditionExpression></sequenceFlow>");
    }

    private GraphEdge defaultEdge(GraphNode node, List<GraphEdge> outgoing) {
        if (!isConditionalSplit(node)) {
            return null;
        }
        return outgoing.stream().filter(edge -> DEFAULT_PORT.equals(edge.sourcePort())).findFirst().orElse(null);
    }

    private String generatedConditionExpression(GraphNode sourceNode, GraphEdge edge) {
        if (!isConditionalSplit(sourceNode) || DEFAULT_PORT.equals(edge.sourcePort())) {
            return null;
        }
        String variableName = "hunyuan_graph_route_" + safeId(sourceNode.nodeId()) + "_" + safeId(edge.sourcePort());
        return "${execution.getVariable('" + variableName + "') == true}";
    }

    private boolean isConditionalSplit(GraphNode node) {
        Object gatewayMode = node.properties().get("gatewayMode");
        return "SPLIT".equals(gatewayMode)
                && (node.type() == GraphNodeType.CONDITION || node.type() == GraphNodeType.INCLUSIVE_GATEWAY);
    }

    private String compiledNodeId(GraphNode node) {
        return node.type() == GraphNodeType.APPROVAL
                ? "graph_stage_" + safeId(node.nodeId())
                : "graph_node_" + safeId(node.nodeId());
    }

    private String edgeId(String authoredId) {
        return "graph_edge_" + safeId(authoredId);
    }

    private String safeId(String value) {
        return value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
