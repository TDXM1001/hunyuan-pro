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
        Map<String, String> entryNodeIds = new HashMap<>();
        Map<String, String> exitNodeIds = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            nodesById.put(node.nodeId(), node);
            outgoingByNodeId.put(node.nodeId(), new ArrayList<>());
            String compiledId = compiledNodeId(node);
            compiledNodeIds.put(node.nodeId(), mappingNodeId(node, compiledId));
            entryNodeIds.put(node.nodeId(), entryNodeId(node, compiledId));
            exitNodeIds.put(node.nodeId(), exitNodeId(node, compiledId));
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
            String compiledId = baseNodeId(node);
            String compiledType = appendNode(xml, node, compiledId, outgoingByNodeId.get(node.nodeId()));
            mappings.add(new GraphCompiledElementMapping(node.nodeId(), "NODE", compiledNodeIds.get(node.nodeId()), compiledType));
        }
        for (GraphEdge edge : graph.edges()) {
            String compiledId = edgeId(edge.edgeId());
            appendEdge(xml, edge, compiledId, nodesById.get(edge.sourceNodeId()), entryNodeIds, exitNodeIds);
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
            case DELAY -> "intermediateCatchEvent";
            case EXTERNAL_TRIGGER -> "WAIT_CALLBACK".equals(node.properties().get("waitMode")) ? "receiveTask" : "serviceTask";
            case SUB_PROCESS -> "callActivity";
        };
        if (node.type() == GraphNodeType.EXTERNAL_TRIGGER) {
            appendExternalTrigger(xml, node, compiledId);
            return tag;
        }
        xml.append("<").append(tag).append(" id=\"").append(escapeXml(compiledId))
                .append("\" name=\"").append(escapeXml(node.name())).append("\"");
        GraphEdge defaultEdge = defaultEdge(node, outgoing);
        if (defaultEdge != null) {
            xml.append(" default=\"").append(escapeXml(edgeId(defaultEdge.edgeId()))).append("\"");
        }
        if (node.type() == GraphNodeType.COPY) {
            xml.append(" flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\"");
        }
        if (node.type() == GraphNodeType.START) {
            xml.append("><extensionElements><flowable:executionListener event=\"end\" ")
                    .append("delegateExpression=\"${hunyuanSubProcessInstanceStartListener}\"/>")
                    .append("</extensionElements></startEvent>");
            return tag;
        }
        if (node.type() == GraphNodeType.APPROVAL) {
            xml.append("><extensionElements><flowable:executionListener event=\"start\" ")
                    .append("delegateExpression=\"${hunyuanApprovalStageControl}\"/>")
                    .append("</extensionElements></receiveTask>");
            return tag;
        }
        if (node.type() == GraphNodeType.DELAY) {
            appendDelay(xml, node, tag);
            return tag;
        }
        if (node.type() == GraphNodeType.SUB_PROCESS) {
            appendSubProcess(xml, node, tag);
            return tag;
        }
        if (isConditionalSplit(node)) {
            xml.append("><extensionElements><flowable:executionListener event=\"start\" ")
                    .append("delegateExpression=\"${hunyuanGraphRouteDecisionListener}\">")
                    .append("<flowable:field name=\"routeNodeKey\"><flowable:string>")
                    .append(escapeXml(node.nodeId()))
                    .append("</flowable:string></flowable:field></flowable:executionListener>")
                    .append("</extensionElements></").append(tag).append(">");
            return tag;
        }
        xml.append("/>");
        return tag;
    }

    private void appendDelay(StringBuilder xml, GraphNode node, String tag) {
        xml.append("><extensionElements>")
                .append("<flowable:executionListener event=\"start\" delegateExpression=\"${hunyuanDelayStartListener}\">")
                .append(flowableField("authoredNodeId", node.nodeId()))
                .append("</flowable:executionListener>")
                .append("<flowable:executionListener event=\"end\" delegateExpression=\"${hunyuanDelayEndListener}\"/>")
                .append("</extensionElements><timerEventDefinition>");
        String mode = String.valueOf(node.properties().get("mode"));
        String value = String.valueOf(node.properties().get("value"));
        if ("DURATION".equals(mode)) {
            xml.append("<timeDuration>").append(escapeXml(value)).append("</timeDuration>");
        } else if ("FIXED_DATETIME".equals(mode)) {
            xml.append("<timeDate>").append(escapeXml(value)).append("</timeDate>");
        } else {
            xml.append("<timeDate>${delay_").append(safeId(node.nodeId())).append("}</timeDate>");
        }
        xml.append("</timerEventDefinition></").append(tag).append(">");
    }

    private void appendExternalTrigger(StringBuilder xml, GraphNode node, String compiledId) {
        String waitMode = String.valueOf(node.properties().get("waitMode"));
        String invokeId = "WAIT_CALLBACK".equals(waitMode) ? compiledId + "_invoke" : compiledId;
        xml.append("<serviceTask id=\"").append(escapeXml(invokeId)).append("\" name=\"").append(escapeXml(node.name()))
                .append("\" flowable:delegateExpression=\"${hunyuanExternalTriggerDelegate}\">")
                .append("<extensionElements>")
                .append(flowableField("externalNodeKey", node.nodeId()))
                .append(flowableField("connectorKey", node.properties().get("connectorKey")))
                .append(flowableField("operationKey", node.properties().get("operationKey")))
                .append(flowableField("waitMode", waitMode))
                .append("</extensionElements></serviceTask>");
        if ("WAIT_CALLBACK".equals(waitMode)) {
            String timeoutAfter = String.valueOf(((Map<?, ?>) node.properties().get("timeoutPolicy")).get("timeoutAfter"));
            xml.append("<receiveTask id=\"").append(escapeXml(compiledId)).append("_wait\" name=\"")
                    .append(escapeXml(node.name())).append("回调等待\"><extensionElements>")
                    .append("<flowable:executionListener event=\"start\" delegateExpression=\"${hunyuanExternalWaitListener}\">")
                    .append(flowableField("authoredNodeId", node.nodeId()))
                    .append("</flowable:executionListener>")
                    .append("</extensionElements></receiveTask>")
                    .append("<sequenceFlow id=\"graph_internal_").append(safeId(node.nodeId()))
                    .append("_invoke_wait\" sourceRef=\"").append(escapeXml(invokeId))
                    .append("\" targetRef=\"").append(escapeXml(compiledId)).append("_wait\"/>")
                    .append("<boundaryEvent id=\"").append(escapeXml(compiledId)).append("_timeout\" attachedToRef=\"")
                    .append(escapeXml(compiledId)).append("_wait\" cancelActivity=\"true\"><timerEventDefinition><timeDuration>")
                    .append(escapeXml(timeoutAfter)).append("</timeDuration></timerEventDefinition></boundaryEvent>")
                    .append("<serviceTask id=\"").append(escapeXml(compiledId)).append("_timeout_action\" name=\"外部等待超时处理\" ")
                    .append("flowable:delegateExpression=\"${hunyuanTimeEventDelegate}\"><extensionElements>")
                    .append(flowableField("timeEventKind", "EXTERNAL_TIMEOUT"))
                    .append(flowableField("authoredNodeKey", node.nodeId()))
                    .append("</extensionElements></serviceTask>")
                    .append("<endEvent id=\"").append(escapeXml(compiledId)).append("_timeout_end\" name=\"外部等待超时\"/>")
                    .append("<sequenceFlow id=\"graph_internal_").append(safeId(node.nodeId()))
                    .append("_timeout_action\" sourceRef=\"").append(escapeXml(compiledId)).append("_timeout\" targetRef=\"")
                    .append(escapeXml(compiledId)).append("_timeout_action\"/>")
                    .append("<sequenceFlow id=\"graph_internal_").append(safeId(node.nodeId()))
                    .append("_timeout_end\" sourceRef=\"").append(escapeXml(compiledId)).append("_timeout_action\" targetRef=\"")
                    .append(escapeXml(compiledId)).append("_timeout_end\"/>");
        }
    }

    private void appendSubProcess(StringBuilder xml, GraphNode node, String tag) {
        xml.append(" calledElement=\"").append(escapeXml(String.valueOf(node.properties().get("calledEngineProcessDefinitionId"))))
                .append("\" flowable:calledElementType=\"id\"><extensionElements><flowable:executionListener event=\"start\" ")
                .append("delegateExpression=\"${hunyuanSubProcessLifecycleListener}\">")
                .append(flowableField("authoredNodeId", node.nodeId()))
                .append("</flowable:executionListener>")
                .append("<flowable:executionListener event=\"end\" ")
                .append("delegateExpression=\"${hunyuanSubProcessLifecycleListener}\">")
                .append(flowableField("authoredNodeId", node.nodeId()))
                .append("</flowable:executionListener>")
                ;
        xml.append("<flowable:in source=\"hunyuanInstanceId\" target=\"hunyuanParentInstanceId\"/>");
        xml.append("<flowable:in source=\"hunyuanChildInstanceId\" target=\"hunyuanInstanceId\"/>");
        appendVariableMappings(xml, node.properties().get("inputMapping"), "in");
        appendVariableMappings(xml, node.properties().get("outputMapping"), "out");
        xml.append("</extensionElements></").append(tag).append(">");
    }

    private void appendVariableMappings(StringBuilder xml, Object rawMapping, String element) {
        if (!(rawMapping instanceof Map<?, ?> mapping)) return;
        mapping.entrySet().stream().sorted(Map.Entry.comparingByKey((left, right) -> String.valueOf(left).compareTo(String.valueOf(right))))
                .forEach(entry -> xml.append("<flowable:").append(element)
                        .append(" source=\"").append(escapeXml(String.valueOf(entry.getValue())))
                        .append("\" target=\"").append(escapeXml(String.valueOf(entry.getKey())))
                        .append("\"/>"));
    }

    private String flowableField(String name, Object value) {
        return "<flowable:field name=\"" + escapeXml(name) + "\"><flowable:string>"
                + escapeXml(String.valueOf(value)) + "</flowable:string></flowable:field>";
    }

    private void appendEdge(
            StringBuilder xml,
            GraphEdge edge,
            String compiledId,
            GraphNode sourceNode,
            Map<String, String> entryNodeIds,
            Map<String, String> exitNodeIds
    ) {
        xml.append("<sequenceFlow id=\"").append(escapeXml(compiledId))
                .append("\" sourceRef=\"").append(escapeXml(exitNodeIds.get(edge.sourceNodeId())))
                .append("\" targetRef=\"").append(escapeXml(entryNodeIds.get(edge.targetNodeId())))
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

    private String baseNodeId(GraphNode node) {
        return node.type() == GraphNodeType.APPROVAL
                ? "graph_stage_" + safeId(node.nodeId())
                : "graph_node_" + safeId(node.nodeId());
    }

    private String entryNodeId(GraphNode node, String compiledId) {
        return node.type() == GraphNodeType.EXTERNAL_TRIGGER
                && "WAIT_CALLBACK".equals(node.properties().get("waitMode"))
                ? compiledId + "_invoke" : compiledId;
    }

    private String exitNodeId(GraphNode node, String compiledId) {
        return node.type() == GraphNodeType.EXTERNAL_TRIGGER
                && "WAIT_CALLBACK".equals(node.properties().get("waitMode"))
                ? compiledId + "_wait" : compiledId;
    }

    private String mappingNodeId(GraphNode node, String compiledId) {
        return exitNodeId(node, compiledId);
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
