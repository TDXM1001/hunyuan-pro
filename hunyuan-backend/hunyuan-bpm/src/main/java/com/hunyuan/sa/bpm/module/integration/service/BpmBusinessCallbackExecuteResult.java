package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调执行结果。
 */
public record BpmBusinessCallbackExecuteResult(
        boolean processed,
        boolean succeeded,
        String message
) {

    public static BpmBusinessCallbackExecuteResult skipped(String message) {
        return new BpmBusinessCallbackExecuteResult(false, false, message);
    }

    public static BpmBusinessCallbackExecuteResult success() {
        return new BpmBusinessCallbackExecuteResult(true, true, "回调执行成功");
    }

    public static BpmBusinessCallbackExecuteResult failed(String message) {
        return new BpmBusinessCallbackExecuteResult(true, false, message);
    }
}
