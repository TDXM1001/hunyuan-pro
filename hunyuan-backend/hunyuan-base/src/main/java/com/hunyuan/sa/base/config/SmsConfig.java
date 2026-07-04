package com.hunyuan.sa.base.config;

import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.service.MockSmsProvider;
import com.hunyuan.sa.base.module.support.sms.service.SmsProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SMS provider wiring.
 */
@Configuration
public class SmsConfig {

    private static final String MOCK_MODE = "mock";

    @Bean
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider smsProvider(SmsProperties smsProperties) {
        String mode = StringUtils.trimToEmpty(smsProperties.getMode());
        if (StringUtils.isBlank(mode) || MOCK_MODE.equalsIgnoreCase(mode)) {
            smsProperties.setMode(MOCK_MODE);
            return new MockSmsProvider(smsProperties);
        }
        throw new IllegalStateException("No SmsProvider bean configured for sms.mode=" + mode);
    }
}
