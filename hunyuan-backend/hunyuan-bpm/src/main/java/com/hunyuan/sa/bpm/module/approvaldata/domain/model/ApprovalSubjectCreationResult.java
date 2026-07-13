package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

public record ApprovalSubjectCreationResult(
        Long approvalSubjectSnapshotId,
        Long routingFactSnapshotId,
        Long processWorkingDataId,
        long subjectVersion,
        long routingFactVersion,
        long workingDataVersion
) {
}
