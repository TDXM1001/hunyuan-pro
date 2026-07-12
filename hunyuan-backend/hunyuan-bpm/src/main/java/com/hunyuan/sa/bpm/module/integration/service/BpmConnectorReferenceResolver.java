package com.hunyuan.sa.bpm.module.integration.service;

/**
 * 解析连接器端点和凭据的安全引用。
 */
public interface BpmConnectorReferenceResolver {

    String resolve(String reference);
}
