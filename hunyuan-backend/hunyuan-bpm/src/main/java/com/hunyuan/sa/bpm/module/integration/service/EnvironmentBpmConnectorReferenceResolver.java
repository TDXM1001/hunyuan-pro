package com.hunyuan.sa.bpm.module.integration.service;

import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 首期只接受 env: 前缀，避免把真实地址和凭据写入数据库快照。
 */
@Component
public class EnvironmentBpmConnectorReferenceResolver implements BpmConnectorReferenceResolver {

    @Resource
    private Environment environment;

    @Override
    public String resolve(String reference) {
        if (reference == null || !reference.startsWith("env:") || reference.length() <= 4) {
            throw new IllegalArgumentException("连接器安全引用必须使用 env: 前缀");
        }
        String value = environment.getProperty(reference.substring(4));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("连接器安全引用未配置：" + reference);
        }
        return value;
    }
}
