package com.hunyuan.sa.bpm.module.candidate.domain.model;

import org.apache.commons.lang3.StringUtils;

/**
 * 新建策略草稿的受控输入。策略版本号由目录生成，调用方不能覆盖既有版本。
 */
public record PolicyDraftCommand(
        PolicyType type,
        String policyKey,
        Integer schemaVersion,
        String policyJson,
        Long createdByEmployeeId
) {

    public PolicyDraftCommand {
        if (type == null) {
            throw new IllegalArgumentException("策略类型不能为空");
        }
        if (StringUtils.isBlank(policyKey)) {
            throw new IllegalArgumentException("策略编码不能为空");
        }
        if (schemaVersion == null || schemaVersion <= 0) {
            throw new IllegalArgumentException("策略 schema 版本必须为正整数");
        }
        if (StringUtils.isBlank(policyJson)) {
            throw new IllegalArgumentException("策略内容不能为空");
        }
        if (createdByEmployeeId == null || createdByEmployeeId <= 0) {
            throw new IllegalArgumentException("策略创建人员不能为空");
        }
    }
}
