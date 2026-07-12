package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 对精确策略版本执行生命周期变更时的并发约束。
 */
public record PolicyLifecycleCommand(
        PolicyReference reference,
        Long expectedCatalogRevision,
        Long operatedByEmployeeId
) {

    public PolicyLifecycleCommand {
        if (reference == null) {
            throw new IllegalArgumentException("策略版本引用不能为空");
        }
        if (expectedCatalogRevision == null || expectedCatalogRevision < 0) {
            throw new IllegalArgumentException("策略目录版本不能为空或为负数");
        }
        if (operatedByEmployeeId == null || operatedByEmployeeId <= 0) {
            throw new IllegalArgumentException("策略操作人员不能为空");
        }
    }
}
