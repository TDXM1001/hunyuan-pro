package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 管理端在持久化前校验策略结构后得到的不可变结果。
 */
public record PolicyValidationResult(
        PolicyType type,
        Integer schemaVersion,
        String canonicalPayload,
        String digest
) {
}
