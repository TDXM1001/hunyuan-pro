package com.hunyuan.sa.bpm.module.candidate.domain.model;

public record ApprovalStageFact(String stageInvocationId, Long tenantId, ApprovalStageState state) {
}
