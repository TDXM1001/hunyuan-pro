package com.hunyuan.sa.bpm.engine.compiler.graph;

import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphScope;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBpmnCompilerTest {

    private final GraphBpmnCompiler compiler = new GraphBpmnCompiler();

    @Test
    void compileShouldProduceBpmnAndMappingForEveryAuthoredElement() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of()),
                        new GraphNode("node_review", "scope_root", GraphNodeType.APPROVAL, "主管审批", Map.of(), Map.of()),
                        new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of())
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "node_start", "node_review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "node_review", "node_end", "default", Map.of())
                ), Map.of()
        );

        GraphCompiledArtifact artifact = compiler.compile("expense_apply", "报销申请", graph);

        assertThat(artifact.compiledBpmnXml()).contains(
                "<startEvent id=\"graph_node_node_start\"",
                "<userTask id=\"graph_node_node_review\"",
                "<endEvent id=\"graph_node_node_end\"",
                "<sequenceFlow id=\"graph_edge_edge_start_review\""
        );
        assertThat(artifact.mappings()).hasSize(5);
        assertThat(artifact.mappings()).extracting(GraphCompiledElementMapping::authoredElementId)
                .containsExactlyInAnyOrder(
                        "node_start", "node_review", "node_end", "edge_start_review", "edge_review_end"
                );
    }

    @Test
    void compileShouldRenderControlledConditionalFlowsAndGatewayPairs() {
        HunyuanProcessDefinitionGraph graph = fullM1Graph();

        GraphCompiledArtifact artifact = compiler.compile("expense_apply", "报销申请", graph);

        assertThat(artifact.compiledBpmnXml()).contains(
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "<exclusiveGateway id=\"graph_node_route_split\" name=\"金额路由\" default=\"graph_edge_route_default\"/>",
                "<parallelGateway id=\"graph_node_parallel_split\"",
                "<parallelGateway id=\"graph_node_parallel_join\"",
                "flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\"",
                "${execution.getVariable('hunyuan_graph_route_route_split_large_amount') == true}",
                "<sequenceFlow id=\"graph_edge_route_default\" sourceRef=\"graph_node_route_split\" targetRef=\"graph_node_route_join\"/>"
        );
        assertThat(artifact.mappings()).extracting(GraphCompiledElementMapping::authoredElementId)
                .containsExactlyInAnyOrderElementsOf(
                        java.util.stream.Stream.concat(
                                graph.nodes().stream().map(GraphNode::nodeId),
                                graph.edges().stream().map(GraphEdge::edgeId)
                        ).toList()
                );
    }

    static HunyuanProcessDefinitionGraph fullM1Graph() {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        node("start", GraphNodeType.START, "开始"),
                        gateway("route_split", GraphNodeType.CONDITION, "金额路由", "SPLIT", "route_join"),
                        node("large_review", GraphNodeType.APPROVAL, "大额审批"),
                        gateway("route_join", GraphNodeType.CONDITION, "金额汇聚", "JOIN", "route_split"),
                        gateway("parallel_split", GraphNodeType.PARALLEL_GATEWAY, "并行分叉", "SPLIT", "parallel_join"),
                        node("finance_copy", GraphNodeType.COPY, "财务抄送"),
                        node("archive_review", GraphNodeType.HANDLE, "归档办理"),
                        gateway("parallel_join", GraphNodeType.PARALLEL_GATEWAY, "并行汇聚", "JOIN", "parallel_split"),
                        node("end", GraphNodeType.END, "结束")
                ),
                List.of(
                        edge("start_route", "start", "route_split", "default", Map.of()),
                        edge(
                                "route_large",
                                "route_split",
                                "large_review",
                                "large_amount",
                                Map.of("routeCondition", Map.of(
                                        "sourceType", "FORM_FIELD",
                                        "fieldKey", "amount",
                                        "valueType", "NUMBER",
                                        "operator", "GT",
                                        "compareValue", 5000
                                ))
                        ),
                        edge("large_join", "large_review", "route_join", "default", Map.of()),
                        edge("route_default", "route_split", "route_join", "default", Map.of()),
                        edge("route_join_parallel", "route_join", "parallel_split", "default", Map.of()),
                        edge("parallel_copy", "parallel_split", "finance_copy", "copy_branch", Map.of()),
                        edge("parallel_archive", "parallel_split", "archive_review", "archive_branch", Map.of()),
                        edge("copy_join", "finance_copy", "parallel_join", "default", Map.of()),
                        edge("archive_join", "archive_review", "parallel_join", "default", Map.of()),
                        edge("parallel_end", "parallel_join", "end", "default", Map.of())
                ),
                Map.of()
        );
    }

    private static GraphNode node(String nodeId, GraphNodeType type, String name) {
        return new GraphNode(nodeId, "scope_root", type, name, Map.of(), Map.of());
    }

    private static GraphNode gateway(
            String nodeId,
            GraphNodeType type,
            String name,
            String gatewayMode,
            String pairedGatewayId
    ) {
        return new GraphNode(
                nodeId,
                "scope_root",
                type,
                name,
                Map.of("gatewayMode", gatewayMode, "pairedGatewayId", pairedGatewayId),
                Map.of()
        );
    }

    private static GraphEdge edge(
            String edgeId,
            String sourceNodeId,
            String targetNodeId,
            String sourcePort,
            Map<String, Object> properties
    ) {
        return new GraphEdge(edgeId, "scope_root", sourceNodeId, targetNodeId, sourcePort, properties);
    }
}
