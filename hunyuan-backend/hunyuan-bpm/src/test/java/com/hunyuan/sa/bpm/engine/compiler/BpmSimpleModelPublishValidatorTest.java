package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmSimpleModelPublishValidatorTest {

    private final BpmSimpleModelPublishValidator validator = new BpmSimpleModelPublishValidator();

    @Test
    void validateShouldRejectEmployeeSelectAtStartWhenFormFieldMissing() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"input\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("发起时选择审批").contains("approverEmployeeId").contains("不存在");
    }

    @Test
    void validateShouldRejectEmployeeSelectAtStartWhenFormFieldTypeMismatch() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"input\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("发起时选择审批").contains("approverEmployeeId").contains("员工单选");
    }

    @Test
    void validateShouldAcceptNestedEmployeeSelectAtStartField() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"group\",\"type\":\"group\",\"children\":[{\"field\":\"approverEmployeeId\",\"props\":{\"component\":\"employeeSelect\"}}]}]}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldRejectEmployeeSelectFieldWhenSchemaOnlyProvidesAliasName() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"name\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("approverEmployeeId").contains("不存在");
    }

    @Test
    void validateShouldRejectInvalidFormSchemaJson() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("表单 Schema JSON 不合法");
    }
}
