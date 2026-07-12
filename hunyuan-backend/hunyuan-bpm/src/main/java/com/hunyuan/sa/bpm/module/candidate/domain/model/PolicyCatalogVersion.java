package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 面向管理端和发布端的不可变策略版本摘要。
 */
public record PolicyCatalogVersion(
        PolicyReference reference,
        String lifecycleState,
        Integer schemaVersion,
        String canonicalPayload,
        String digest,
        Long catalogRevision
) {
}
