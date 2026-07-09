package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调处理结果。
 */
public record BpmBusinessCallbackResult(
        boolean success,
        String responsePayloadJson,
        String failureReason
) {

    public static BpmBusinessCallbackResult success(String responsePayloadJson) {
        return new BpmBusinessCallbackResult(true, responsePayloadJson, null);
    }

    public static BpmBusinessCallbackResult failed(String failureReason, String responsePayloadJson) {
        return new BpmBusinessCallbackResult(false, responsePayloadJson, failureReason);
    }
}
