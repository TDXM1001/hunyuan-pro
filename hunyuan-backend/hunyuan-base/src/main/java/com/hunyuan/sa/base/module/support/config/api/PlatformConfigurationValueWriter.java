package com.hunyuan.sa.base.module.support.config.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台配置值写入协议。
 *
 * <p>仅供确实拥有某个配置 key 语义的模块更新已有值；新增配置和管理端编辑继续走管理 Facade。</p>
 */
public interface PlatformConfigurationValueWriter {

    ResponseDTO<String> updateValue(String configurationKey, String configurationValue);
}
