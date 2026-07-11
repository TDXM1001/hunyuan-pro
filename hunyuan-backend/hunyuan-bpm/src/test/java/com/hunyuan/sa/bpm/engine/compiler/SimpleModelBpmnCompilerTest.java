package com.hunyuan.sa.bpm.engine.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleModelBpmnCompilerTest {

    private final SimpleModelBpmnCompiler compiler = new SimpleModelBpmnCompiler();

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
}
