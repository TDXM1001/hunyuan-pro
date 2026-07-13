package com.hunyuan.sa.bpm.module.businesscontract.domain.model;

public record BusinessContractValidationResult(
        Integer schemaVersion,
        String canonicalContractJson,
        String contractDigest
) {
}
