package com.hunyuan.sa.bpm.businesscontract;

import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.BusinessObjectValidationResult;
import com.hunyuan.sa.bpm.module.businesscontract.service.BusinessObjectV2DocumentMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessObjectV2DocumentMapperTest {
    private final BusinessObjectV2DocumentMapper mapper = new BusinessObjectV2DocumentMapper();

    @Test
    void v2ShouldKeepBusinessSemanticsSeparateFromPresentation() {
        BusinessObjectValidationResult result = mapper.compile(BusinessObjectFixtures.expense());
        assertThat(result.canonicalPayload()).contains("\"schemaVersion\":2", "\"fieldSchema\"", "\"presentation\"", "\"lineItemSchema\"");
        assertThat(result.businessSummary()).contains("费用申请", "申请金额", "费用明细");
        assertThat(result.digest()).hasSize(64);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void duplicateKeysAcrossZonesShouldReturnFieldFinding() {
        var source = BusinessObjectFixtures.expense();
        var invalid = source.withWorkingDataSchema(List.of(source.fieldSchema().get(0)));
        BusinessObjectValidationResult result = mapper.compile(invalid);
        assertThat(result.valid()).isFalse();
        assertThat(result.findings()).anySatisfy(finding -> assertThat(finding.fieldPath()).contains("workingDataSchema"));
    }
}
