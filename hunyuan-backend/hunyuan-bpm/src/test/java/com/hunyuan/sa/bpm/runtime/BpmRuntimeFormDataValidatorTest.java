package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeFormDataValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BpmRuntimeFormDataValidatorTest {

    private final BpmRuntimeFormDataValidator validator = new BpmRuntimeFormDataValidator();

    @Test
    void validateShouldRejectSchemaUnknownField() {
        ResponseDTO<String> response = validator.validateFullData(
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}",
                "{\"amount\":100,\"internalCode\":\"secret\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("internalCode").contains("表单");
    }

    @Test
    void validateShouldRejectRequiredEditableFieldWhenBlank() {
        BpmFieldPermissionVO permission = new BpmFieldPermissionVO();
        permission.setFieldKey("approvedAmount");
        permission.setPermission("EDITABLE");
        permission.setRequired(true);

        ResponseDTO<String> response = validator.validateTaskData(
                "{\"fields\":[{\"field\":\"approvedAmount\",\"type\":\"number\"}]}",
                "{\"approvedAmount\":null}",
                List.of(permission)
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("approvedAmount").contains("必填");
    }

    @Test
    void validateShouldRejectSupportedBasicTypeMismatch() {
        ResponseDTO<String> response = validator.validateFullData(
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}",
                "{\"amount\":\"not-a-number\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("amount").contains("数值");
    }
}
