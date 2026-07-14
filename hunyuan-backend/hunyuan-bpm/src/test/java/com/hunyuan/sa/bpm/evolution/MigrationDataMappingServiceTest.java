package com.hunyuan.sa.bpm.evolution;

import com.hunyuan.sa.bpm.module.evolution.service.MigrationDataMappingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;

class MigrationDataMappingServiceTest {
    private final MigrationDataMappingService service = new MigrationDataMappingService();

    @Test
    void changedRequiredContractFieldMustBeBlockedWithoutTypedMapping() {
        var result = service.validate(dependencies("oldAmount", "DECIMAL", true),
                dependencies("approvedAmount", "DECIMAL", true), "{}");

        assertThat(result.valid()).isFalse();
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("approvedAmount"));
    }

    @Test
    void typedFieldAndVariableMappingsMustValidateAndTransformSnapshots() {
        String mapping = "{\"fieldMappings\":{\"oldAmount\":\"approvedAmount\"},"
                + "\"workingDataMappings\":{\"oldAmount\":\"approvedAmount\"},"
                + "\"variableMappings\":{\"legacyFlag\":\"approvalFlag\"}}";

        var result = service.validate(dependencies("oldAmount", "DECIMAL", true),
                dependencies("approvedAmount", "DECIMAL", true), mapping);

        assertThat(result.valid()).isTrue();
        assertThat(service.applyJson("{\"oldAmount\":12.5,\"note\":\"keep\"}", result.fieldMappings()))
                .contains("\"approvedAmount\":12.5", "\"note\":\"keep\"")
                .doesNotContain("oldAmount");
    }

    @Test
    void missingFrozenContractMustFailClosed() {
        var result = service.validate("{}", "{}", "{}");
        assertThat(result.valid()).isFalse();
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("冻结业务契约"));
    }

    @Test
    void runtimePayloadAndVariableMappingsMustBeVerifiable() {
        String mapping = "{\"fieldMappings\":{\"oldAmount\":\"approvedAmount\"},"
                + "\"workingDataMappings\":{\"oldAmount\":\"approvedAmount\"},"
                + "\"variableMappings\":{\"legacyFlag\":\"approvalFlag\"}}";
        var result = service.validateRuntime(dependencies("oldAmount", "DECIMAL", true),
                dependencies("approvedAmount", "DECIMAL", true), mapping,
                "{\"oldAmount\":\"not-a-number\"}", "{}", Map.of());
        assertThat(result.valid()).isFalse();
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("值类型"));
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("legacyFlag"));
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("工作数据"));
    }

    @Test
    void duplicateMappingTargetsMustBeRejected() {
        String mapping = "{\"fieldMappings\":{\"a\":\"target\",\"b\":\"target\"}}";
        var result = service.validate(dependencies("a", "STRING", false),
                dependencies("target", "STRING", false), mapping);
        assertThat(result.valid()).isFalse();
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("多个来源"));
    }

    private String dependencies(String key, String type, boolean required) {
        String contract = "{\"fieldSchema\":[{\"key\":\"" + key + "\",\"type\":\"" + type
                + "\",\"required\":" + required + "}],\"workingDataSchema\":[{\"key\":\"" + key
                + "\",\"type\":\"" + type + "\",\"required\":" + required + "}]}";
        return "{\"businessContract\":{\"canonicalPayload\":" + quote(contract) + "}}";
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
