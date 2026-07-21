package com.hunyuan.sa.admin;

import com.hunyuan.sa.base.config.SmsConfig;
import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.service.SmsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class SmsProviderContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("sms.mode=mock");

    @Test
    void smsProviderShouldBePresent() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(SmsProvider.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SmsProperties.class)
    @Import(SmsConfig.class)
    static class TestConfiguration {
    }
}
