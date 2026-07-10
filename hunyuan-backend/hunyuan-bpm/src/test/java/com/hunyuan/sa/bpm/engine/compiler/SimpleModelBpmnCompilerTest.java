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
                .contains("\"sequentialIndex\":2")
                .contains("\"sequentialTotal\":3");
        assertThat(artifact.nodeSnapshots().get(1).authoredRuleSnapshotJson())
                .contains("\"approvalMode\":\"sequential\"")
                .contains("\"employeeIds\":[301,302,303]");
    }
}
