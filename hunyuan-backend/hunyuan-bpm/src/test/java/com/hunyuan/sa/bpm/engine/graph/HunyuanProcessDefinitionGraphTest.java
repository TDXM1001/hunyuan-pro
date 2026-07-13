package com.hunyuan.sa.bpm.engine.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HunyuanProcessDefinitionGraphTest {

    private final GraphCanonicalizer canonicalizer = new GraphCanonicalizer();

    @Test
    void layoutOnlyChangeShouldKeepSemanticHash() {
        HunyuanProcessDefinitionGraph original = graph(Map.of("x", 120, "y", 80));
        HunyuanProcessDefinitionGraph moved = graph(Map.of("x", 500, "y", 320));

        assertThat(canonicalizer.semanticHash(original))
                .isEqualTo(canonicalizer.semanticHash(moved));
    }

    @Test
    void duplicateNodeIdShouldBeRejected() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of()),
                        new GraphNode("node_start", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of())
                ),
                List.of(),
                Map.of()
        );

        assertThatThrownBy(() -> canonicalizer.canonicalize(graph))
                .isInstanceOf(GraphValidationException.class)
                .hasMessageContaining("重复节点 ID");
    }

    @Test
    void crossScopeEdgeShouldBeRejected() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(
                        new GraphScope("scope_root", null, "主流程"),
                        new GraphScope("scope_child", "scope_root", "子作用域")
                ),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of()),
                        new GraphNode("node_review", "scope_child", GraphNodeType.APPROVAL, "审批", Map.of(), Map.of())
                ),
                List.of(new GraphEdge("edge_1", "scope_root", "node_start", "node_review", "default", Map.of())),
                Map.of()
        );

        assertThatThrownBy(() -> canonicalizer.canonicalize(graph))
                .isInstanceOf(GraphValidationException.class)
                .hasMessageContaining("跨作用域连线");
    }

    @Test
    void m5NodeShouldParticipateInCanonicalGraphSemantics() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(new GraphNode("node_delay", "scope_root", GraphNodeType.DELAY, "等待", Map.of("mode", "DURATION", "value", "PT1H"), Map.of())),
                List.of(),
                Map.of()
        );

        assertThat(canonicalizer.canonicalize(graph)).contains("DELAY", "PT1H");
        HunyuanProcessDefinitionGraph changed = new HunyuanProcessDefinitionGraph(
                1, "scope_root", graph.scopes(),
                List.of(new GraphNode("node_delay", "scope_root", GraphNodeType.DELAY, "等待", Map.of("mode", "DURATION", "value", "PT2H"), Map.of())),
                List.of(), Map.of());
        assertThat(canonicalizer.semanticHash(changed)).isNotEqualTo(canonicalizer.semanticHash(graph));
    }

    @Test
    void invalidStableIdShouldBeRejected() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(new GraphNode("node with space", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of())),
                List.of(),
                Map.of()
        );

        assertThatThrownBy(() -> canonicalizer.canonicalize(graph))
                .isInstanceOf(GraphValidationException.class)
                .hasMessageContaining("节点 ID 非法");
    }

    private HunyuanProcessDefinitionGraph graph(Map<String, Object> reviewLayout) {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of("x", 0, "y", 0)),
                        new GraphNode("node_review", "scope_root", GraphNodeType.APPROVAL, "主管审批", Map.of("strategyRef", "manager"), reviewLayout),
                        new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of("x", 800, "y", 0))
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "node_start", "node_review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "node_review", "node_end", "default", Map.of())
                ),
                Map.of("displayName", "报销审批")
        );
    }
}
