package com.hunyuan.sa.base.module.support.config.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台配置管理公开边界。
 *
 * <p>该边界仅承载受权限保护的管理读取，不替代运行时策略和缓存读取能力。</p>
 */
public interface PlatformConfigurationFacade {

    ResponseDTO<PageResult<PlatformConfigurationSummary>> queryPage(PlatformConfigurationPageQuery query);

    ResponseDTO<String> create(PlatformConfigurationCreateCommand command);

    ResponseDTO<String> update(Long configurationId, PlatformConfigurationUpdateCommand command);
}
