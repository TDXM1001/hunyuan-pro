package com.hunyuan.sa.admin.module.platform.notification.sms.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsSendForm;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsSendResult;

/**
 * SMS provider adapter.
 */
public interface SmsProvider {

    ResponseDTO<SmsSendResult> send(SmsSendForm sendForm);
}
