package com.hunyuan.sa.base.module.support.config.api;

/**
 * 平台配置值只读协议。
 *
 * <p>业务 owner 只能按稳定 key 读取值，不能依赖配置中心的缓存、实体或持久化实现。</p>
 */
public interface PlatformConfigurationValueReader {

    String getValue(String configurationKey);
}
