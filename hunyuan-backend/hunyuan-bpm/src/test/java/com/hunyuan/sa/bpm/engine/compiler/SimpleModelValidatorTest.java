package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleModelValidatorTest {

    private final SimpleModelValidator validator = new SimpleModelValidator();

    @Test
    void validateShouldAcceptStartEmployeeCandidateStrategies() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":["
                        + "{\"id\":\"task_self\",\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"},"
                        + "{\"id\":\"task_start_manager\",\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                        + "]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldExplainSupportedCandidateStrategiesWhenResolverTypeUnsupported() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_unknown\",\"nodeKey\":\"task_unknown\",\"name\":\"未知审批\",\"type\":\"userTask\",\"candidateResolverType\":\"USER_GROUP\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg())
                .contains("EMPLOYEE")
                .contains("DEPARTMENT_MANAGER")
                .contains("ROLE")
                .contains("START_EMPLOYEE")
                .contains("START_DEPARTMENT_MANAGER")
                .contains("EMPLOYEE_SELECT_AT_START");
    }

    @Test
    void validateShouldAcceptEmployeeSelectAtStartWhenFieldKeyConfigured() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_selected\",\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldRejectEmployeeSelectAtStartWhenFieldKeyMissing() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_selected\",\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("发起时自选审批人字段");
    }

    @Test
    void validateShouldAcceptSequentialEmployeeApprovalWhenEmployeeIdsConfigured() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldRejectSequentialEmployeeApprovalWhenEmployeeIdsDuplicated() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,301]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("重复");
    }

    @Test
    void validateShouldRejectSequentialApprovalWhenResolverIsNotEmployee() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"ROLE\",\"employeeIds\":[301,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("指定员工");
    }

    @Test
    void validateShouldRejectSequentialApprovalWhenEmployeeIdsAreInsufficient() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("至少配置 2 名员工");
    }

    @Test
    void validateShouldRejectSequentialApprovalWhenEmployeeIdIsInvalid() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,0]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("员工 ID 无效");
    }

    @Test
    void validateShouldRejectSequentialApprovalWhenEmployeeIdHasHiddenFraction() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301.0000000000000000001,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("员工 ID 无效");
    }

    @Test
    void validateShouldRejectSequentialApprovalWhenEmployeeIdOverflowsLong() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[9223372036854775808,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("顺序审批").contains("员工 ID 无效");
    }

    @Test
    void validateShouldRejectSequentialExpandedNodeKeyCollision() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":["
                        + "{\"id\":\"task_finance\",\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]},"
                        + "{\"id\":\"task_finance_1\",\"nodeKey\":\"task_finance_1\",\"name\":\"财务归档\",\"type\":\"userTask\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":303}"
                        + "]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("task_finance_1").contains("冲突");
    }

    @Test
    void validateShouldRejectSequentialExpandedNodeKeyLongerThanDatabaseLimit() {
        String nodeKey = "a".repeat(127);
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"" + nodeKey + "\",\"nodeKey\":\"" + nodeKey
                        + "\",\"name\":\"超长节点\",\"type\":\"userTask\",\"approvalMode\":\"sequential\","
                        + "\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("128").contains("节点 key");
    }

    @Test
    void validateShouldRejectSequentialExpandedNodeKeyWhenFormatInvalid() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"1task_finance\",\"nodeKey\":\"1task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\","
                        + "\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("节点 key").contains("格式非法");
    }

    @Test
    void validateShouldRejectNodeKeyWithHyphenBecauseItBreaksAssigneeExpression() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"finance-review\",\"nodeKey\":\"finance-review\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"single\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg())
                .contains("节点 key")
                .contains("格式非法")
                .doesNotContain("中划线");
    }

    @Test
    void validateShouldRejectCompilerReservedBpmnIds() {
        for (String reservedNodeKey : new String[]{"startEvent", "endEvent", "flow_0", "flow_12", "flow_end"}) {
            ResponseDTO<String> response = validator.validate(
                    "{\"nodes\":[{\"id\":\"" + reservedNodeKey + "\",\"nodeKey\":\"" + reservedNodeKey
                            + "\",\"name\":\"保留节点\",\"type\":\"userTask\",\"approvalMode\":\"single\","
                            + "\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301}]}",
                    "{\"type\":\"ALL\"}"
            );

            assertThat(response.getOk()).as(reservedNodeKey).isFalse();
            assertThat(response.getMsg()).as(reservedNodeKey)
                    .contains("节点 key")
                    .contains("编译器保留");
        }
    }
}
