package com.hunyuan.sa.bpm.engine.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;

import static org.assertj.core.api.Assertions.assertThat;

class GraphDocumentCodecTest {

    private final GraphDocumentCodec codec = new GraphDocumentCodec();

    @Test
    void importCopyShouldAllocateFreshIdsAndKeepTopology() {
        HunyuanProcessDefinitionGraph source = graph();

        HunyuanProcessDefinitionGraph copied = codec.importAsNewGraph(codec.export(source));

        assertThat(copied.rootScopeId()).isNotEqualTo(source.rootScopeId());
        assertThat(scopeIds(copied)).doesNotContainAnyElementsOf(scopeIds(source));
        assertThat(nodeIds(copied)).doesNotContainAnyElementsOf(nodeIds(source));
        assertThat(edgeIds(copied)).doesNotContainAnyElementsOf(edgeIds(source));
        assertThat(copied.nodes()).extracting(GraphNode::type)
                .containsExactlyElementsOf(source.nodes().stream().map(GraphNode::type).toList());
        assertThat(copied.nodes()).extracting(GraphNode::name)
                .containsExactlyElementsOf(source.nodes().stream().map(GraphNode::name).toList());
        assertThat(copied.nodes()).filteredOn(node -> node.name().equals("主管审批"))
                .singleElement()
                .extracting(GraphNode::layout)
                .isEqualTo(Map.of("x", 240, "y", 80));
        assertThat(copied.edges()).allSatisfy(edge -> {
            assertThat(nodeIds(copied)).contains(edge.sourceNodeId(), edge.targetNodeId());
            assertThat(scopeIds(copied)).contains(edge.scopeId());
        });
    }

    @Test
    void restoreShouldRoundTripCanonicalGraphAndLayout() {
        HunyuanProcessDefinitionGraph source = graph();

        String document = codec.export(source);
        HunyuanProcessDefinitionGraph restored = codec.restore(document);

        assertThat(new GraphCanonicalizer().canonicalize(restored))
                .isEqualTo(new GraphCanonicalizer().canonicalize(source));
        assertThat(restored.nodes()).filteredOn(node -> node.nodeId().equals("node_review"))
                .singleElement()
                .extracting(GraphNode::layout)
                .isEqualTo(Map.of("x", 240, "y", 80));
    }

    @Test
    void restoreStoredDraftShouldMergeCanonicalGraphAndSeparateLayout() {
        HunyuanProcessDefinitionGraph source = graph();
        Map<String, Object> layouts = Map.of(
                "node_start", Map.of("x", 0, "y", 80),
                "node_review", Map.of("x", 240, "y", 80),
                "node_end", Map.of("x", 480, "y", 80)
        );

        HunyuanProcessDefinitionGraph restored = codec.restoreStored(
                new GraphCanonicalizer().canonicalize(source),
                JSON.toJSONString(layouts)
        );

        assertThat(restored.nodes()).filteredOn(node -> node.nodeId().equals("node_review"))
                .singleElement()
                .extracting(GraphNode::layout)
                .isEqualTo(Map.of("x", 240, "y", 80));
    }

    private HunyuanProcessDefinitionGraph graph() {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(
                        new GraphScope("scope_root", null, "主流程"),
                        new GraphScope("scope_review", "scope_root", "审批区")
                ),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of("x", 0, "y", 80)),
                        new GraphNode("node_review", "scope_root", GraphNodeType.APPROVAL, "主管审批", Map.of("strategyRef", "manager"), Map.of("x", 240, "y", 80)),
                        new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of("x", 480, "y", 80))
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "node_start", "node_review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "node_review", "node_end", "default", Map.of())
                ),
                Map.of("riskLevel", "LOW")
        );
    }

    private Set<String> scopeIds(HunyuanProcessDefinitionGraph graph) {
        return graph.scopes().stream().map(GraphScope::scopeId).collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> nodeIds(HunyuanProcessDefinitionGraph graph) {
        return graph.nodes().stream().map(GraphNode::nodeId).collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> edgeIds(HunyuanProcessDefinitionGraph graph) {
        return graph.edges().stream().map(GraphEdge::edgeId).collect(java.util.stream.Collectors.toSet());
    }
}
