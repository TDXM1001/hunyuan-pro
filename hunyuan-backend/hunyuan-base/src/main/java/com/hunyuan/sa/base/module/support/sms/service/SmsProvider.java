package com.hunyuan.sa.base.module.support.sms.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendResult;

/**
 * SMS provider adapter.
 */
public interface SmsProvider {

    ResponseDTO<SmsSendResult> send(SmsSendForm sendForm);
}
