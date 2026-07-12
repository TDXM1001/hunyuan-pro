package com.hunyuan.sa.bpm.module.candidate.domain.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Graph 对目录中一个精确策略版本的引用。
 */
public record PolicyReference(PolicyType type, String policyKey, int policyVersion) {

    public PolicyReference {
        if (type == null || StringUtils.isBlank(policyKey) || policyVersion <= 0) {
            throw new IllegalArgumentException("策略引用不合法");
        }
        policyKey = policyKey.trim();
    }
}
