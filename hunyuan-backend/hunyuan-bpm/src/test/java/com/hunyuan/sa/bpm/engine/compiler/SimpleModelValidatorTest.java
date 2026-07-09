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
                .contains("START_DEPARTMENT_MANAGER");
    }
}
