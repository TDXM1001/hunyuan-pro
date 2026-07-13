package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

public record WorkingDataMutationCommand(
        Long approvalSubjectSnapshotId,
        Long taskId,
        Long expectedDataVersion,
        String patchJson,
        String reason,
        Long actorEmployeeId,
        String actorName,
        String actionType,
        String comment,
        String attachmentsJson
) {
}
