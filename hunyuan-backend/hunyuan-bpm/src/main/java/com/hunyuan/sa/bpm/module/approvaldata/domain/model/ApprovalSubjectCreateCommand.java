package com.hunyuan.sa.bpm.module.approvaldata.domain.model;

public record ApprovalSubjectCreateCommand(
        String contractKey,
        Integer contractVersion,
        String sourceSystem,
        String businessType,
        String businessKey,
        String title,
        String summary,
        String fieldsJson,
        String lineItemsJson,
        String attachmentsJson,
        String routingFactsJson,
        String workingDataJson,
        Long submitterEmployeeId,
        String submitterName
) {
}
