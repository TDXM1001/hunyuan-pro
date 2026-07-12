package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * 候选解析过程中的可审计诊断。
 */
public record CandidateDiagnostic(String code, String message, Long sourceEmployeeId) {
}
