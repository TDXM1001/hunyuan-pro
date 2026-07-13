package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

public record ApprovalRuntimeBinding(
        Long approvalSubjectSnapshotId,
        Long routingFactSnapshotId,
        Long processWorkingDataId,
        Long businessContractVersionId,
        String sourceSystem,
        String businessType,
        String businessKey,
        String title,
        String summary,
        String workingDataJson,
        long workingDataVersion
) {
}
