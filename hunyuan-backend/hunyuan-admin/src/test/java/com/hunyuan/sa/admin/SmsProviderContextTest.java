package com.hunyuan.sa.admin;

import com.hunyuan.sa.base.module.support.sms.service.SmsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "project.log-directory=target/test-logs",
                "project.name=hunyuan-admin-test"
        }
)
class SmsProviderContextTest {

    @Autowired
    private SmsProvider smsProvider;

    @Test
    void smsProviderShouldBePresent() {
        assertThat(smsProvider).isNotNull();
    }
}
