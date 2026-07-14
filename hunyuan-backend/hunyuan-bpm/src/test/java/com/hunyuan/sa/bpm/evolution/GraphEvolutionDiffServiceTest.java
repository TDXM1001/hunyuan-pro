package com.hunyuan.sa.bpm.evolution;

import com.hunyuan.sa.bpm.module.evolution.domain.model.GraphEvolutionDiff;
import com.hunyuan.sa.bpm.module.evolution.service.GraphEvolutionDiffService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphEvolutionDiffServiceTest {

    private final GraphEvolutionDiffService service = new GraphEvolutionDiffService();

    @Test
    void layoutOnlyChangeMustNotBeReportedAsRuntimeSemanticChange() {
        String graph = "{\"nodes\":[{\"nodeId\":\"start\",\"type\":\"START\",\"name\":\"开始\",\"properties\":{}}],\"edges\":[],\"scopes\":[]}";

        GraphEvolutionDiff diff = service.compare(graph, "{\"start\":{\"x\":10}}", "{\"policy\":1}",
                graph, "{\"start\":{\"x\":80}}", "{\"policy\":1}");

        assertThat(diff.layoutChanged()).isTrue();
        assertThat(diff.semanticChanged()).isFalse();
        assertThat(diff.migrationSuggested()).isFalse();
        assertThat(diff.changes()).extracting(GraphEvolutionDiff.Change::kind)
                .containsExactly("LAYOUT_CHANGED");
    }

    @Test
    void stableIdsMustClassifyNodeEdgeConfigurationAndDependencyChanges() {
        String source = "{\"nodes\":[{\"nodeId\":\"start\",\"type\":\"START\",\"name\":\"开始\",\"properties\":{}},{\"nodeId\":\"approve\",\"type\":\"APPROVAL\",\"name\":\"主管审批\",\"properties\":{\"policyVersionId\":1}}],\"edges\":[{\"edgeId\":\"e1\",\"sourceNodeId\":\"start\",\"targetNodeId\":\"approve\",\"properties\":{}}],\"scopes\":[]}";
        String target = "{\"nodes\":[{\"nodeId\":\"start\",\"type\":\"START\",\"name\":\"开始\",\"properties\":{}},{\"nodeId\":\"approve\",\"type\":\"APPROVAL\",\"name\":\"财务审批\",\"properties\":{\"policyVersionId\":2}},{\"nodeId\":\"copy\",\"type\":\"COPY\",\"name\":\"抄送\",\"properties\":{}}],\"edges\":[{\"edgeId\":\"e2\",\"sourceNodeId\":\"approve\",\"targetNodeId\":\"copy\",\"properties\":{}}],\"scopes\":[]}";

        GraphEvolutionDiff diff = service.compare(source, "{}", "{\"policy\":1}", target, "{}", "{\"policy\":2}");

        assertThat(diff.semanticChanged()).isTrue();
        assertThat(diff.migrationSuggested()).isTrue();
        assertThat(diff.changes()).extracting(GraphEvolutionDiff.Change::kind)
                .contains("NODE_ADDED", "NODE_CONFIG_CHANGED", "EDGE_ADDED", "EDGE_REMOVED", "DEPENDENCY_CHANGED");
    }

    @Test
    void propertyOrderOnlyChangeMustRemainEquivalent() {
        String source = "{\"nodes\":[{\"nodeId\":\"start\",\"type\":\"START\",\"properties\":{\"a\":1,\"b\":2}}],\"edges\":[],\"scopes\":[]}";
        String target = "{\"nodes\":[{\"properties\":{\"b\":2,\"a\":1},\"type\":\"START\",\"nodeId\":\"start\"}],\"edges\":[],\"scopes\":[]}";
        assertThat(service.compare(source, "{}", "{}", target, "{}", "{}").semanticChanged()).isFalse();
    }
}
