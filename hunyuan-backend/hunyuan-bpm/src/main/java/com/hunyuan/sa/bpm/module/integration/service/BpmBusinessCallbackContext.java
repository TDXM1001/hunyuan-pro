package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调执行上下文。
 */
public record BpmBusinessCallbackContext(
        Long callbackRecordId,
        String eventId,
        Long instanceId,
        String businessType,
        Long businessId,
        String requestPayloadJson
) {
}
