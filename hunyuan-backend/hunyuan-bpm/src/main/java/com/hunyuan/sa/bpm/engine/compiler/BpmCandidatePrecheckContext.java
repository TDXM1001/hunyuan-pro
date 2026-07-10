package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 候选策略预检时可选的模拟运行上下文。
 */
public record BpmCandidatePrecheckContext(
        Long startEmployeeId,
        Long startDepartmentId,
        String formDataJson
) {

    public static BpmCandidatePrecheckContext empty() {
        return new BpmCandidatePrecheckContext(null, null, null);
    }
}
