package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调处理器。
 */
public interface BpmBusinessCallbackHandler {

    /**
     * 当前处理器支持的业务类型。
     */
    String businessType();

    /**
     * 执行业务回调。业务侧必须保证同一 eventId 幂等。
     */
    BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context);
}
