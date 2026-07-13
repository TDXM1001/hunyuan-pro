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

class GraphPublicationPrecheckTest {

    private final GraphPublicationPrecheck precheck = new GraphPublicationPrecheck();

    @Test
    void validGraphShouldPassPrecheck() {
        GraphPublicationPrecheckResult result = precheck.check(validGraph());

        assertThat(result.pass()).isTrue();
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void unreachableNodeShouldBlockPublishWithAuthoredNodeId() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        start(), approval("node_review"), end(), approval("node_orphan")
                ),
                List.of(
                        edge("edge_start_review", "node_start", "node_review"),
                        edge("edge_review_end", "node_review", "node_end")
                ), Map.of()
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.pass()).isFalse();
        assertThat(result.findings()).anySatisfy(finding -> {
            assertThat(finding.code()).isEqualTo("NODE_UNREACHABLE");
            assertThat(finding.authoredElementId()).isEqualTo("node_orphan");
        });
    }

    @Test
    void cycleShouldBlockPublishBeforeCompilerRuns() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(start(), approval("node_review"), end()),
                List.of(
                        edge("edge_start_review", "node_start", "node_review"),
                        edge("edge_review_start", "node_review", "node_start"),
                        edge("edge_review_end", "node_review", "node_end")
                ), Map.of()
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.pass()).isFalse();
        assertThat(result.findings()).extracting(GraphPublicationFinding::code)
                .contains("GRAPH_CYCLE_FORBIDDEN");
    }

    @Test
    void conditionalSplitShouldRequireDefaultRouteMetadataAndPairedJoin() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        start(),
                        gateway("route_split", GraphNodeType.CONDITION, "SPLIT", "route_join"),
                        approval("small_review"),
                        approval("large_review"),
                        gateway("route_join", GraphNodeType.CONDITION, "JOIN", "route_split"),
                        end()
                ),
                List.of(
                        edge("edge_start_route", "node_start", "route_split"),
                        new GraphEdge("edge_small", "scope_root", "route_split", "small_review", "small_amount", Map.of()),
                        new GraphEdge("edge_large", "scope_root", "route_split", "large_review", "large_amount", Map.of()),
                        edge("edge_small_join", "small_review", "route_join"),
                        edge("edge_large_join", "large_review", "route_join"),
                        edge("edge_join_end", "route_join", "node_end")
                ),
                Map.of()
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.pass()).isFalse();
        assertThat(result.findings()).extracting(GraphPublicationFinding::code)
                .contains("GATEWAY_DEFAULT_ROUTE_MISSING", "GATEWAY_ROUTE_CONDITION_MISSING");
        assertThat(result.findings()).extracting(GraphPublicationFinding::authoredElementId)
                .contains("route_split", "edge_small", "edge_large");
    }

    @Test
    void pairedGatewayShouldRejectWrongModeAndPortShape() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        start(),
                        gateway("parallel_split", GraphNodeType.PARALLEL_GATEWAY, "SPLIT", "parallel_join"),
                        approval("left_review"),
                        approval("right_review"),
                        gateway("parallel_join", GraphNodeType.PARALLEL_GATEWAY, "SPLIT", "parallel_split"),
                        end()
                ),
                List.of(
                        edge("edge_start_parallel", "node_start", "parallel_split"),
                        new GraphEdge("edge_left", "scope_root", "parallel_split", "left_review", "branch", Map.of()),
                        new GraphEdge("edge_right", "scope_root", "parallel_split", "right_review", "branch", Map.of()),
                        edge("edge_left_join", "left_review", "parallel_join"),
                        edge("edge_right_join", "right_review", "parallel_join"),
                        edge("edge_join_end", "parallel_join", "node_end")
                ),
                Map.of()
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.pass()).isFalse();
        assertThat(result.findings()).extracting(GraphPublicationFinding::code)
                .contains("GATEWAY_PAIR_MODE_INVALID", "GATEWAY_BRANCH_PORT_DUPLICATE");
        assertThat(result.findings()).extracting(GraphPublicationFinding::authoredElementId)
                .contains("parallel_split", "parallel_join", "edge_right");
    }

    @Test
    void advancedNodesShouldRejectUnsafeOrUnfrozenConfiguration() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        start(),
                        new GraphNode("delay", "scope_root", GraphNodeType.DELAY, "延迟",
                                Map.of("mode", "DURATION", "value", "tomorrow"), Map.of()),
                        new GraphNode("external", "scope_root", GraphNodeType.EXTERNAL_TRIGGER, "外部调用",
                                Map.of(
                                        "connectorKey", "finance",
                                        "operationKey", "createExpense",
                                        "waitMode", "WAIT_CALLBACK",
                                        "url", "http://127.0.0.1/admin",
                                        "credential", "secret"
                                ), Map.of()),
                        new GraphNode("child", "scope_root", GraphNodeType.SUB_PROCESS, "子流程",
                                Map.of("calledProcessKey", "expense_archive"), Map.of()),
                        end()
                ),
                List.of(
                        edge("start_delay", "node_start", "delay"),
                        edge("delay_external", "delay", "external"),
                        edge("external_child", "external", "child"),
                        edge("child_end", "child", "node_end")
                ), Map.of()
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.pass()).isFalse();
        assertThat(result.findings()).extracting(GraphPublicationFinding::code).contains(
                "DELAY_VALUE_INVALID",
                "EXTERNAL_CONNECTOR_VERSION_REQUIRED",
                "EXTERNAL_ENDPOINT_FORBIDDEN",
                "EXTERNAL_CREDENTIAL_FORBIDDEN",
                "EXTERNAL_TIMEOUT_POLICY_REQUIRED",
                "SUB_PROCESS_VERSION_REQUIRED",
                "SUB_PROCESS_FAILURE_POLICY_REQUIRED",
                "SUB_PROCESS_CANCEL_POLICY_REQUIRED"
        );
    }

    @Test
    void slaAutoTerminalShouldRequireLowRiskAndValidDuration() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        start(),
                        new GraphNode("review", "scope_root", GraphNodeType.APPROVAL, "审批",
                                Map.of("taskSlaPolicy", Map.of("dueAfter", "tomorrow", "timeoutAction", "AUTO_APPROVE")), Map.of()),
                        end()
                ),
                List.of(edge("start_review", "node_start", "review"), edge("review_end", "review", "node_end")),
                Map.of("riskLevel", "HIGH")
        );

        GraphPublicationPrecheckResult result = precheck.check(graph);

        assertThat(result.findings()).extracting(GraphPublicationFinding::code)
                .contains("SLA_DURATION_INVALID", "SLA_AUTO_TERMINAL_RISK_FORBIDDEN");
    }

    private HunyuanProcessDefinitionGraph validGraph() {
        return new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(start(), approval("node_review"), end()),
                List.of(
                        edge("edge_start_review", "node_start", "node_review"),
                        edge("edge_review_end", "node_review", "node_end")
                ), Map.of()
        );
    }

    private GraphNode start() {
        return new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of());
    }

    private GraphNode approval(String nodeId) {
        return new GraphNode(nodeId, "scope_root", GraphNodeType.APPROVAL, "审批", Map.of(), Map.of());
    }

    private GraphNode gateway(String nodeId, GraphNodeType type, String mode, String pairedGatewayId) {
        return new GraphNode(
                nodeId,
                "scope_root",
                type,
                "网关",
                Map.of("gatewayMode", mode, "pairedGatewayId", pairedGatewayId),
                Map.of()
        );
    }

    private GraphNode end() {
        return new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of());
    }

    private GraphEdge edge(String edgeId, String source, String target) {
        return new GraphEdge(edgeId, "scope_root", source, target, "default", Map.of());
    }
}
