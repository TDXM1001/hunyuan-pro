package com.hunyuan.sa.base.module.support.sms.service;

import cn.hutool.core.util.IdUtil;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Default SMS provider for local and test environments.
 */
@Slf4j
public class MockSmsProvider implements SmsProvider {

    private static final String PROVIDER_NAME = "mock";

    private final SmsProperties smsProperties;

    public MockSmsProvider(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    @Override
    public ResponseDTO<SmsSendResult> send(SmsSendForm sendForm) {
        SmsSendResult result = new SmsSendResult();
        result.setProvider(PROVIDER_NAME);
        result.setMode(smsProperties.getMode());
        result.setRequestId(IdUtil.fastSimpleUUID());
        result.setPhone(sendForm.getPhone());
        result.setTemplateCode(sendForm.getTemplateCode());

        log.info("mock sms sent, mode={}, phone={}, templateCode={}, requestId={}",
                smsProperties.getMode(), maskPhone(sendForm.getPhone()), sendForm.getTemplateCode(), result.getRequestId());
        return ResponseDTO.ok(result);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
