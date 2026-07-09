package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;

/**
 * BPM 任务候选人解析上下文。
 */
public record BpmTaskAssignmentContext(
        BpmEmployeeSnapshot startEmployeeSnapshot,
        String formDataJson
) {
}
