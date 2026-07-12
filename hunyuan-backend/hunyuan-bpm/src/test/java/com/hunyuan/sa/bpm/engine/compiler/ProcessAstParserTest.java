package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAstParserTest {

    private final ProcessAstParser parser = new ProcessAstParser();

    private final ProcessAstWalker walker = new ProcessAstWalker();

    @Test
    void missingSchemaVersionShouldReadAsV1() {
        ProcessAst ast = parser.parse(
                "{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"审批\"}]}"
        );

        assertThat(ast.schemaVersion()).isEqualTo(1);
        assertThat(ast.nodes()).hasSize(1);
        assertThat(ast.nodes().get(0).type()).isEqualTo(ProcessNodeType.USER_TASK);
    }

    @Test
    void parseV2ShouldKeepNestedAuthoredOrder() {
        ProcessAst ast = parser.parse("""
                {
                  "schemaVersion": 2,
                  "settings": {"maxBranchDepth": 3},
                  "nodes": [
                    {
                      "nodeKey": "amount_route",
                      "name": "金额路由",
                      "type": "EXCLUSIVE_BRANCH",
                      "branches": [
                        {
                          "branchKey": "small",
                          "name": "小额",
                          "condition": {"sourceType": "FORM_FIELD", "fieldKey": "amount", "valueType": "NUMBER", "operator": "LTE", "compareValue": 5000},
                          "nodes": [{"nodeKey": "small_review", "name": "小额审批", "type": "USER_TASK"}]
                        },
                        {
                          "branchKey": "large",
                          "name": "大额",
                          "condition": {"sourceType": "FORM_FIELD", "fieldKey": "amount", "valueType": "NUMBER", "operator": "GT", "compareValue": 5000},
                          "nodes": [
                            {
                              "nodeKey": "large_parallel",
                              "name": "大额并行",
                              "type": "PARALLEL_BRANCH",
                              "branches": [
                                {"branchKey": "finance", "name": "财务", "nodes": [{"nodeKey": "finance_review", "name": "财务审批", "type": "USER_TASK"}]},
                                {"branchKey": "director", "name": "总监", "nodes": [{"nodeKey": "director_review", "name": "总监审批", "type": "USER_TASK"}]}
                              ]
                            }
                          ]
                        },
                        {"branchKey": "default", "name": "默认", "isDefault": true, "nodes": []}
                      ]
                    },
                    {"nodeKey": "archive", "name": "归档", "type": "HANDLE_TASK"}
                  ]
                }
                """);

        assertThat(ast.schemaVersion()).isEqualTo(2);
        assertThat(walker.walk(ast))
                .extracting(ProcessNode::nodeKey)
                .containsExactly(
                        "amount_route",
                        "small_review",
                        "large_parallel",
                        "finance_review",
                        "director_review",
                        "archive"
                );
    }

    @Test
    void parseV3ShouldReadDelayAndExternalTriggerNodes() {
        ProcessAst ast = parser.parse("""
                {
                  "schemaVersion": 3,
                  "nodes": [
                    {
                      "nodeKey": "wait_one_day",
                      "name": "等待一天",
                      "type": "DELAY",
                      "mode": "DURATION",
                      "value": "P1D",
                      "timezone": "Asia/Shanghai",
                      "overduePolicy": "TRIGGER_IMMEDIATELY"
                    },
                    {
                      "nodeKey": "sync_finance",
                      "name": "同步财务系统",
                      "type": "EXTERNAL_TRIGGER",
                      "connectorKey": "finance",
                      "operationKey": "createExpense",
                      "requestMapping": {"amount": "approvedAmount"},
                      "responseMapping": {"externalNo": "financeNo"},
                      "waitMode": "WAIT_CALLBACK",
                      "timeoutPolicy": {"timeoutAfter": "PT30M"}
                    }
                  ]
                }
                """);

        assertThat(ast.schemaVersion()).isEqualTo(3);
        assertThat(ast.nodes()).extracting(node -> node.type().name())
                .containsExactly("DELAY", "EXTERNAL_TRIGGER");
        assertThat(ast.nodes()).extracting(ProcessNode::nodeKey)
                .containsExactly("wait_one_day", "sync_finance");
    }
}
