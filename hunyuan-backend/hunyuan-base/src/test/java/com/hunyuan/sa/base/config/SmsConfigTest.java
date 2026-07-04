package com.hunyuan.sa.base.config;

import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.service.MockSmsProvider;
import com.hunyuan.sa.base.module.support.sms.service.SmsProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsConfigTest {

    private final SmsConfig smsConfig = new SmsConfig();

    @Test
    void shouldCreateMockProviderWhenModeIsBlank() {
        SmsProperties properties = new SmsProperties();
        properties.setMode("  ");

        SmsProvider smsProvider = smsConfig.smsProvider(properties);

        assertThat(smsProvider).isInstanceOf(MockSmsProvider.class);
        assertThat(properties.getMode()).isEqualTo("mock");
    }

    @Test
    void shouldCreateMockProviderWhenModeIsMock() {
        SmsProperties properties = new SmsProperties();
        properties.setMode("mock");

        SmsProvider smsProvider = smsConfig.smsProvider(properties);

        assertThat(smsProvider).isInstanceOf(MockSmsProvider.class);
    }

    @Test
    void shouldFailFastWhenModeRequiresExplicitProvider() {
        SmsProperties properties = new SmsProperties();
        properties.setMode("aliyun");

        assertThatThrownBy(() -> smsConfig.smsProvider(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No SmsProvider bean configured for sms.mode=aliyun");
    }
}
