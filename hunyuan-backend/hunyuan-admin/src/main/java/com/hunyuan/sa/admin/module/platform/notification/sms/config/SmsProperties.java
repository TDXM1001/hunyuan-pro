package com.hunyuan.sa.admin.module.platform.notification.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMS runtime properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String mode = "mock";

    private Long idempotentExpireSeconds = 300L;

    private Long rateLimitSeconds = 60L;
}
