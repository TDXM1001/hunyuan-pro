package com.hunyuan.sa.bpm.api.identity;

public record BpmIdentityOptionSnapshot(
        String kind, Long stableId, String displayName,
        Long departmentId, String departmentName, boolean disabled
) {
}
