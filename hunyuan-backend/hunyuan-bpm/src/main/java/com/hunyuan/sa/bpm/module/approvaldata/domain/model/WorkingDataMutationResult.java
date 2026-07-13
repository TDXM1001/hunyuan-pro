package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

public record WorkingDataMutationResult(
        Long processWorkingDataId,
        Long taskActionEvidenceId,
        long dataVersion,
        String dataJson
) {
}
