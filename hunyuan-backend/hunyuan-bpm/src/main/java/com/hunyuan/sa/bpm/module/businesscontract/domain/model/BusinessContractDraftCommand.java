package com.hunyuan.sa.bpm.module.businesscontract.domain.model;

import org.apache.commons.lang3.StringUtils;

public record BusinessContractDraftCommand(
        String contractKey,
        Integer schemaVersion,
        String contractJson,
        Long createdByEmployeeId
) {
    public BusinessContractDraftCommand {
        if (StringUtils.isBlank(contractKey) || contractKey.length() > 64) {
            throw new IllegalArgumentException("业务契约编码必须为 1 到 64 个字符");
        }
        if (schemaVersion == null || schemaVersion <= 0) {
            throw new IllegalArgumentException("业务契约 schema 版本必须为正整数");
        }
        if (StringUtils.isBlank(contractJson)) {
            throw new IllegalArgumentException("业务契约内容不能为空");
        }
        if (createdByEmployeeId == null || createdByEmployeeId <= 0) {
            throw new IllegalArgumentException("业务契约创建人员不能为空");
        }
    }
}
