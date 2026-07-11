package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.ast.BranchNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessBranch;
import com.hunyuan.sa.bpm.engine.compiler.CompiledDefinitionArtifact;
import com.hunyuan.sa.bpm.engine.compiler.ProcessAstParser;
import com.hunyuan.sa.bpm.engine.compiler.ProcessAstValidator;
import com.hunyuan.sa.bpm.engine.compiler.ProcessAstWalker;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelBpmnCompiler;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteConditionEvaluator;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseDefinitionSeedService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BpmM1BusinessFlowTest {

    private final ProcessAstParser parser = new ProcessAstParser();

    private final ProcessAstWalker walker = new ProcessAstWalker();

    private final BpmRouteExpressionRegistry expressionRegistry = new BpmRouteExpressionRegistry(List.of());

    private final BpmRouteConditionEvaluator evaluator = new BpmRouteConditionEvaluator(expressionRegistry);

    @Test
    void sampleExpenseModelShouldValidateAndCompileCompleteM1Topology() {
        String modelJson = sampleModelJson();
        ProcessAst ast = parser.parse(modelJson);

        assertThat(new ProcessAstValidator().validate(ast, formSchemaJson(), expressionRegistry)).isEmpty();

        CompiledDefinitionArtifact artifact = new SimpleModelBpmnCompiler().compile(
                "sample_expense_apply",
                "样板费用申请",
                modelJson,
                "{\"allowAll\":true}",
                "{}"
        );

        assertThat(artifact.compiledBpmnXml())
                .contains("<exclusiveGateway id=\"hy_gateway_sample_amount_route_split\"")
                .contains("<parallelGateway id=\"hy_gateway_sample_large_parallel_split\"")
                .contains("<parallelGateway id=\"hy_gateway_sample_large_parallel_join\"")
                .contains("<inclusiveGateway id=\"hy_gateway_sample_post_route_split\"")
                .contains("flowable:delegateExpression=\"${hunyuanRouteDecisionDelegate}\"")
                .contains("flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\"");
        assertThat(artifact.nodeSnapshots())
                .extracting(snapshot -> snapshot.nodeKey())
                .contains(
                        "sample_finance_review",
                        "sample_large_finance_review",
                        "sample_risk_review",
                        "sample_manual_handle",
                        "sample_finance_copy",
                        "sample_archive_review"
                );
    }

    @Test
    void sampleExpenseConditionsShouldChooseSmallLargeDefaultAndInclusivePaths() {
        ProcessAst ast = parser.parse(sampleModelJson());
        BranchNode amountRoute = branchNode(ast, "sample_amount_route");
        BranchNode postRoute = branchNode(ast, "sample_post_route");

        assertThat(matchedBranches(amountRoute, Map.of("requestedAmount", 4999)))
                .containsExactly("small_amount");
        assertThat(matchedBranches(amountRoute, Map.of("requestedAmount", 5001)))
                .containsExactly("large_amount");
        assertThat(matchedBranches(amountRoute, Map.of()))
                .containsExactly("manual_check");

        assertThat(matchedBranches(postRoute, Map.of("requestedAmount", 4999)))
                .containsExactly("archive_confirm");
        assertThat(matchedBranches(postRoute, Map.of("requestedAmount", 10000)))
                .containsExactly("finance_copy", "archive_confirm");
        assertThat(matchedBranches(postRoute, Map.of()))
                .containsExactly("missing_amount_archive");
    }

    private List<String> matchedBranches(BranchNode route, Map<String, Object> formData) {
        List<String> matched = route.branches().stream()
                .filter(branch -> !branch.defaultBranch())
                .filter(branch -> evaluator.evaluate(branch.condition(), formData, null).matched())
                .map(ProcessBranch::branchKey)
                .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return route.branches().stream()
                .filter(ProcessBranch::defaultBranch)
                .map(ProcessBranch::branchKey)
                .toList();
    }

    private BranchNode branchNode(ProcessAst ast, String nodeKey) {
        return walker.walk(ast).stream()
                .filter(node -> nodeKey.equals(node.nodeKey()))
                .filter(BranchNode.class::isInstance)
                .map(BranchNode.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到样板路由节点：" + nodeKey));
    }

    private String sampleModelJson() {
        return staticStringField("SIMPLE_MODEL_JSON");
    }

    private String formSchemaJson() {
        return staticStringField("FORM_SCHEMA_JSON");
    }

    private String staticStringField(String fieldName) {
        try {
            Field field = BpmSampleExpenseDefinitionSeedService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("读取样板定义常量失败：" + fieldName, ex);
        }
    }
}
