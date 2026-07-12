package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * M2 对单次发起资格的服务端结论。
 */
public record StartDecision(boolean allowed, String matchedRule, String reason) {
}
