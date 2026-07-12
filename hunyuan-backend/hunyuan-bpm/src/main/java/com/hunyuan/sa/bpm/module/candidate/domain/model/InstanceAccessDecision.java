package com.hunyuan.sa.bpm.module.candidate.domain.model;

/**
 * M2 对实例发现性的结论，不授予任务处理权或 M3 字段读取权限。
 */
public record InstanceAccessDecision(boolean allowed, String matchedRule, String reason) {
}
