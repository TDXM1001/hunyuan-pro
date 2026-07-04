package com.hunyuan.sa.base.module.support.sms.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.redis.RedisService;
import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.constant.SmsSendStatusEnum;
import com.hunyuan.sa.base.module.support.sms.dao.SmsSendLogDao;
import com.hunyuan.sa.base.module.support.sms.dao.SmsTemplateDao;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendLogEntity;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsServiceTest {

    private SmsService smsService;

    private SmsProvider smsProvider;

    private SmsSendLogDao smsSendLogDao;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
        smsProvider = mock(SmsProvider.class);
        smsSendLogDao = mock(SmsSendLogDao.class);

        RedisService redisService = mock(RedisService.class);
        when(redisService.generateRedisKey(anyString(), anyString())).thenAnswer(invocation -> {
            String prefix = invocation.getArgument(0);
            String key = invocation.getArgument(1);
            return prefix + key;
        });
        when(redisService.getLock(anyString(), anyLong())).thenReturn(true);

        ReflectionTestUtils.setField(smsService, "smsProvider", smsProvider);
        ReflectionTestUtils.setField(smsService, "redisService", redisService);
        ReflectionTestUtils.setField(smsService, "smsTemplateDao", mock(SmsTemplateDao.class));
        ReflectionTestUtils.setField(smsService, "smsSendLogDao", smsSendLogDao);
        ReflectionTestUtils.setField(smsService, "smsProperties", new SmsProperties());
    }

    @Test
    void sendShouldPersistFailureWhenProviderThrows() {
        when(smsProvider.send(any())).thenThrow(new IllegalStateException("provider down"));

        ResponseDTO<SmsSendResult> response = smsService.send(buildSendForm());

        assertThat(response.getOk()).isFalse();
        SmsSendLogEntity updatedLog = captureUpdatedLog();
        assertThat(updatedLog.getSendStatus()).isEqualTo(SmsSendStatusEnum.FAIL.getValue());
        assertThat(updatedLog.getFailReason()).isEqualTo("provider down");
    }

    @Test
    void sendShouldPersistFailureWhenProviderReturnsNullResponse() {
        when(smsProvider.send(any())).thenReturn(null);

        ResponseDTO<SmsSendResult> response = smsService.send(buildSendForm());

        assertThat(response.getOk()).isFalse();
        SmsSendLogEntity updatedLog = captureUpdatedLog();
        assertThat(updatedLog.getSendStatus()).isEqualTo(SmsSendStatusEnum.FAIL.getValue());
        assertThat(updatedLog.getFailReason()).isEqualTo("sms provider returned null response");
    }

    @Test
    void sendShouldPersistFailureWhenProviderReturnsEmptyResult() {
        when(smsProvider.send(any())).thenReturn(ResponseDTO.ok());

        ResponseDTO<SmsSendResult> response = smsService.send(buildSendForm());

        assertThat(response.getOk()).isFalse();
        SmsSendLogEntity updatedLog = captureUpdatedLog();
        assertThat(updatedLog.getSendStatus()).isEqualTo(SmsSendStatusEnum.FAIL.getValue());
        assertThat(updatedLog.getFailReason()).isEqualTo("sms provider returned empty result");
    }

    private SmsSendForm buildSendForm() {
        SmsSendForm sendForm = new SmsSendForm();
        sendForm.setPhone("13800138000");
        sendForm.setTemplateCode("login_verification_code");
        sendForm.setContent("Your code is 123456");
        sendForm.setIdempotentKey("request-1");
        return sendForm;
    }

    private SmsSendLogEntity captureUpdatedLog() {
        ArgumentCaptor<SmsSendLogEntity> captor = ArgumentCaptor.forClass(SmsSendLogEntity.class);
        verify(smsSendLogDao).updateById(captor.capture());
        return captor.getValue();
    }
}
