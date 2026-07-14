package com.hunyuan.sa.bpm.module.candidate.domain.vo;

public record BpmIdentityOptionVO(
        String kind, Long stableId, String displayName,
        Long departmentId, String departmentName, boolean disabled
) {
}
