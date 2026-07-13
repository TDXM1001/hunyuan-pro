package com.hunyuan.sa.bpm.module.businesscontract.domain.model;

import org.apache.commons.lang3.StringUtils;

public record BusinessContractLifecycleCommand(
        String contractKey,
        Integer contractVersion,
        Long expectedCatalogRevision,
        Long actorEmployeeId
) {
    public BusinessContractLifecycleCommand {
        if (StringUtils.isBlank(contractKey) || contractVersion == null || contractVersion <= 0) {
            throw new IllegalArgumentException("业务契约版本引用不完整");
        }
        if (expectedCatalogRevision == null || expectedCatalogRevision < 0) {
            throw new IllegalArgumentException("业务契约目录修订号不能为空");
        }
        if (actorEmployeeId == null || actorEmployeeId <= 0) {
            throw new IllegalArgumentException("业务契约操作人员不能为空");
        }
    }
}
