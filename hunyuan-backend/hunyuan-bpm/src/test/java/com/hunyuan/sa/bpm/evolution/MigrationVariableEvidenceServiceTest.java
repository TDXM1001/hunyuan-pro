package com.hunyuan.sa.bpm.evolution;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationVariableEvidenceService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationVariableEvidenceServiceTest {
    private final MigrationVariableEvidenceService service = new MigrationVariableEvidenceService();

    @Test
    void variableDigestMustBeCanonicalAndAuditMustNotContainValues() {
        Map<String, Object> firstValue = new LinkedHashMap<>();
        firstValue.put("z", List.of(2, 1));
        firstValue.put("a", Map.of("inner", "sensitive-value"));
        Map<String, Object> reorderedValue = new LinkedHashMap<>();
        reorderedValue.put("a", Map.of("inner", "sensitive-value"));
        reorderedValue.put("z", List.of(2, 1));

        var first = service.source(Map.of("legacySecret", firstValue),
                Map.of("legacySecret", "currentSecret"));
        var reordered = service.source(Map.of("legacySecret", reorderedValue),
                Map.of("legacySecret", "currentSecret"));

        assertThat(first.sourceVariablesDigest()).hasSize(64).isEqualTo(reordered.sourceVariablesDigest());
        assertThat(JSON.toJSONString(first))
                .contains("legacySecret", "currentSecret", first.sourceVariablesDigest())
                .doesNotContain("sensitive-value");
    }

    @Test
    void targetEvidenceMustRejectMissingMappedVariable() {
        assertThatThrownBy(() -> service.target(Map.of(), Map.of("legacySecret", "currentSecret")))
                .hasMessageContaining("currentSecret", "人工对账");
    }
}
