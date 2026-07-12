package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 在阶段创建时写入运行时的候选成员事实。
 */
public record ResolvedCandidateMember(
        Long sourceEmployeeId,
        Long employeeId,
        String displayName,
        Long departmentId,
        String departmentName
) {
}
