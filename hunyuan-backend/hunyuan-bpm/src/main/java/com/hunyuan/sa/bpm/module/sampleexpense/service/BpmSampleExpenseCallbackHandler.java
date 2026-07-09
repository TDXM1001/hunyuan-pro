package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackHandler;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * BPM 样板费用申请回调处理器。
 */
@Component
public class BpmSampleExpenseCallbackHandler implements BpmBusinessCallbackHandler {

    @Resource
    private BpmSampleExpenseService bpmSampleExpenseService;

    @Override
    public String businessType() {
        return BpmSampleExpenseService.BUSINESS_TYPE;
    }

    @Override
    public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
        return bpmSampleExpenseService.handleCallback(context);
    }
}
