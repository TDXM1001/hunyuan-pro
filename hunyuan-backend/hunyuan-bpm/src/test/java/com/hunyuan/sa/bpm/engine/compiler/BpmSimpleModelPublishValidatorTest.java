package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmSimpleModelPublishValidatorTest {

    private final BpmSimpleModelPublishValidator validator = new BpmSimpleModelPublishValidator();

    @Test
    void validateShouldCheckFieldPermissionsInsideV2Branch() {
        ResponseDTO<String> response = validator.validate(
                "{\"schemaVersion\":2,\"nodes\":[{\"nodeKey\":\"route\",\"type\":\"EXCLUSIVE_BRANCH\",\"branches\":["
                        + "{\"branchKey\":\"a\",\"nodes\":[{\"nodeKey\":\"review\",\"name\":\"嵌套审批\",\"type\":\"USER_TASK\",\"fieldPermissions\":[{\"fieldKey\":\"missingField\",\"permission\":\"READONLY\"}]}]},"
                        + "{\"branchKey\":\"default\",\"isDefault\":true,\"nodes\":[]}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("missingField").contains("不存在");
    }

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

    @Test
    void validateShouldAcceptValidNodeFieldPermissions() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"fieldPermissions\":["
                        + "{\"fieldKey\":\"amount\",\"permission\":\"EDITABLE\",\"required\":true},"
                        + "{\"fieldKey\":\"applicant\",\"permission\":\"READONLY\",\"required\":false},"
                        + "{\"fieldKey\":\"internalCode\",\"permission\":\"HIDDEN\",\"required\":false}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"},{\"field\":\"applicant\",\"type\":\"input\"},{\"field\":\"internalCode\",\"type\":\"input\"}]}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldRejectFieldPermissionWhenFieldMissing() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"fieldPermissions\":[{\"fieldKey\":\"approvedAmount\",\"permission\":\"EDITABLE\"}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("财务复核").contains("approvedAmount").contains("不存在");
    }

    @Test
    void validateShouldRejectDuplicateFieldPermission() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"fieldPermissions\":["
                        + "{\"fieldKey\":\"amount\",\"permission\":\"READONLY\"},{\"fieldKey\":\"amount\",\"permission\":\"EDITABLE\"}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("amount").contains("重复");
    }

    @Test
    void validateShouldRejectRequiredPermissionWhenNotEditable() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"fieldPermissions\":[{\"fieldKey\":\"amount\",\"permission\":\"READONLY\",\"required\":true}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("amount").contains("必填").contains("可编辑");
    }

    @Test
    void validateShouldRejectUnknownPermissionMode() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"fieldPermissions\":[{\"fieldKey\":\"amount\",\"permission\":\"MASKED\"}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("amount").contains("READONLY").contains("EDITABLE").contains("HIDDEN");
    }

    @Test
    void validateShouldRejectEditableFieldOnParallelAll() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"nodeKey\":\"finance\",\"name\":\"财务会签\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"fieldPermissions\":[{\"fieldKey\":\"amount\",\"permission\":\"EDITABLE\"}]}]}",
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("并行全员会签").contains("amount").contains("可编辑");
    }
}
