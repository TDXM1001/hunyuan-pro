package com.hunyuan.sa.bpm.engine.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleModelBpmnCompilerTest {

    private final SimpleModelBpmnCompiler compiler = new SimpleModelBpmnCompiler();

    @Test
    void compileV1GoldenModelShouldKeepExistingTopology() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "golden_v1",
                "v1 黄金模型",
                "{\"nodes\":["
                        + "{\"nodeKey\":\"manager\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":101},"
                        + "{\"nodeKey\":\"finance\",\"type\":\"userTask\",\"name\":\"财务顺签\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[102,103]},"
                        + "{\"nodeKey\":\"audit\",\"type\":\"userTask\",\"name\":\"审计会签\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[104,105]}]}",
                "{\"type\":\"ALL\"}",
                "{}"
        );

        assertThat(artifact.compiledBpmnXml())
                .contains("id=\"manager\"")
                .contains("id=\"finance_1\"")
                .contains("id=\"finance_2\"")
                .contains("id=\"gateway_audit_split\"")
                .contains("id=\"gateway_audit_join\"");
        assertThat(artifact.nodeSnapshots())
                .extracting(CompiledNodeSnapshot::nodeKey)
                .containsExactly("manager", "finance_1", "finance_2", "audit_1", "audit_2");
    }

    @Test
    void compileShouldExpandSequentialEmployeeApprovalToOrderedUserTasks() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "expense_apply",
                "费用审批",
                "{\"nodes\":[{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302,303],\"listeners\":[{\"channel\":\"MESSAGE\"}]}]}",
                "{\"type\":\"ALL\"}",
                "{\"title\":\"费用审批\"}"
        );

        assertThat(artifact.compiledBpmnXml()).contains("id=\"task_finance_1\"");
        assertThat(artifact.compiledBpmnXml()).contains("id=\"task_finance_2\"");
        assertThat(artifact.compiledBpmnXml()).contains("id=\"task_finance_3\"");
        assertThat(artifact.compiledBpmnXml()).contains("flowable:assignee=\"${assignee_task_finance_2}\"");

        assertThat(artifact.nodeSnapshots())
                .extracting(CompiledNodeSnapshot::nodeKey)
                .containsExactly("task_finance_1", "task_finance_2", "task_finance_3");
        assertThat(artifact.nodeSnapshots().get(0).nodeNameSnapshot()).isEqualTo("财务复核（1/3）");
        assertThat(artifact.nodeSnapshots().get(1).compiledNodeSnapshotJson())
                .contains("\"employeeId\":302")
                .contains("\"authoredNodeKey\":\"task_finance\"")
                .contains("\"approvalGroupKey\":\"task_finance\"")
                .contains("\"approvalGroupName\":\"财务复核\"")
                .contains("\"sequentialIndex\":2")
                .contains("\"sequentialTotal\":3");
        assertThat(artifact.nodeSnapshots().get(1).authoredRuleSnapshotJson())
                .contains("\"approvalMode\":\"sequential\"")
                .contains("\"employeeIds\":[301,302,303]");
    }

    @Test
    void compileShouldExpandParallelAllApprovalToFixedGatewayFragment() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "expense_apply",
                "费用审批",
                "{\"nodes\":["
                        + "{\"nodeKey\":\"manager_review\",\"name\":\"主管审批\",\"type\":\"userTask\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":100},"
                        + "{\"nodeKey\":\"finance_review\",\"name\":\"财务会签\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[101,102,103]},"
                        + "{\"nodeKey\":\"archive_review\",\"name\":\"归档审批\",\"type\":\"userTask\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":104}"
                        + "]}",
                "{\"type\":\"ALL\"}",
                "{}"
        );

        String xml = artifact.compiledBpmnXml();
        assertThat(xml).contains("<parallelGateway id=\"gateway_finance_review_split\"");
        assertThat(xml).contains("<userTask id=\"finance_review_1\"");
        assertThat(xml).contains("<userTask id=\"finance_review_2\"");
        assertThat(xml).contains("<userTask id=\"finance_review_3\"");
        assertThat(xml).contains("<parallelGateway id=\"gateway_finance_review_join\"");
        assertThat(xml).contains("sourceRef=\"manager_review\" targetRef=\"gateway_finance_review_split\"");
        assertThat(xml).contains("sourceRef=\"gateway_finance_review_join\" targetRef=\"archive_review\"");
        assertThat(xml).contains("flowable:assignee=\"${assignee_finance_review_2}\"");

        assertThat(artifact.nodeSnapshots())
                .extracting(CompiledNodeSnapshot::nodeKey)
                .containsExactly(
                        "manager_review",
                        "finance_review_1",
                        "finance_review_2",
                        "finance_review_3",
                        "archive_review"
                );
        assertThat(artifact.nodeSnapshots().get(2).compiledNodeSnapshotJson())
                .contains("\"approvalGroupKey\":\"finance_review\"")
                .contains("\"approvalGroupName\":\"财务会签\"")
                .contains("\"parallelIndex\":2")
                .contains("\"parallelTotal\":3")
                .contains("\"employeeId\":102");
    }

    @Test
    void compileShouldPreserveFieldPermissionsForEverySequentialMember() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "expense_apply",
                "费用审批",
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302],\"fieldPermissions\":[{\"fieldKey\":\"approvedAmount\",\"permission\":\"EDITABLE\",\"required\":true}]}]}",
                "{\"type\":\"ALL\"}",
                "{}"
        );

        assertThat(artifact.nodeSnapshots()).hasSize(2);
        assertThat(artifact.nodeSnapshots())
                .allSatisfy(snapshot -> assertThat(snapshot.compiledNodeSnapshotJson())
                        .contains("\"fieldPermissions\"")
                        .contains("\"fieldKey\":\"approvedAmount\"")
                        .contains("\"permission\":\"EDITABLE\"")
                        .contains("\"required\":true"));
    }

    @Test
    void compileV2ShouldBuildControlledBranchFragments() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "expense_v2",
                "费用路由",
                """
                        {"schemaVersion":2,"nodes":[
                          {"nodeKey":"amount_route","name":"金额路由","type":"EXCLUSIVE_BRANCH","branches":[
                            {"branchKey":"small","name":"小额","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"LTE","compareValue":5000},"nodes":[{"nodeKey":"manager_review","name":"经理审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]},
                            {"branchKey":"large","name":"大额","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":5000},"nodes":[
                              {"nodeKey":"joint_review","name":"联合审批","type":"PARALLEL_BRANCH","branches":[
                                {"branchKey":"finance","name":"财务","nodes":[{"nodeKey":"finance_review","name":"财务审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]},
                                {"branchKey":"legal","name":"法务","nodes":[{"nodeKey":"legal_review","name":"法务审批","type":"USER_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]}
                              ]}
                            ]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]},
                          {"nodeKey":"archive_route","name":"归档路由","type":"INCLUSIVE_BRANCH","branches":[
                            {"branchKey":"finance_copy","name":"财务抄送","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GTE","compareValue":10000},"nodes":[]},
                            {"branchKey":"archive","name":"归档","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GTE","compareValue":0},"nodes":[{"nodeKey":"archive_handle","name":"归档办理","type":"HANDLE_TASK","candidateResolverType":"EMPLOYEE","employeeId":1}]},
                            {"branchKey":"default","name":"默认","isDefault":true,"nodes":[]}
                          ]}
                        ]}
                        """,
                "{\"type\":\"ALL\"}",
                "{}"
        );

        String xml = artifact.compiledBpmnXml();
        assertThat(xml)
                .contains("flowable:delegateExpression=\"${hunyuanRouteDecisionDelegate}\"")
                .contains("<exclusiveGateway id=\"hy_gateway_amount_route_split\"")
                .contains("<parallelGateway id=\"hy_gateway_joint_review_split\"")
                .contains("<parallelGateway id=\"hy_gateway_joint_review_join\"")
                .contains("<inclusiveGateway id=\"hy_gateway_archive_route_split\"")
                .contains("${execution.getVariable('route_amount_route_small') == true}")
                .contains("${execution.getVariable('route_archive_route_archive') == true}")
                .doesNotContain("compareValue");
        assertThat(artifact.nodeSnapshots())
                .extracting(CompiledNodeSnapshot::nodeKey)
                .contains("amount_route", "manager_review", "joint_review", "finance_review", "legal_review", "archive_route", "archive_handle");
    }

    @Test
    void compileV2ShouldBuildNonBlockingCopyDelegate() {
        CompiledDefinitionArtifact artifact = compiler.compile(
                "copy_v2",
                "设计时抄送",
                "{\"schemaVersion\":2,\"nodes\":[{\"nodeKey\":\"notify_finance\",\"name\":\"抄送财务\",\"type\":\"COPY_TASK\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[1,2]}]}",
                "{\"type\":\"ALL\"}",
                "{}"
        );

        assertThat(artifact.compiledBpmnXml())
                .contains("flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\"")
                .contains("copyNodeKey")
                .contains("notify_finance");
        assertThat(artifact.nodeSnapshots()).singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.nodeKey()).isEqualTo("notify_finance");
                    assertThat(snapshot.nodeType()).isEqualTo("COPY_TASK");
                });
    }
}
