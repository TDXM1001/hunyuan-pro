package com.hunyuan.sa.bpm.module.businesscontract.domain.model;

public record BusinessContractCatalogVersion(
        Long businessContractVersionId,
        String contractKey,
        Integer contractVersion,
        String lifecycleState,
        Integer schemaVersion,
        String canonicalContractJson,
        String contractDigest,
        Long catalogRevision
) {
}
