package com.hunyuan.sa.base.module.support.reload.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

/**
 * 平台运行时重载公开边界。
 */
public interface PlatformRuntimeReloadFacade {

    ResponseDTO<List<PlatformRuntimeReloadItemView>> listItems();

    ResponseDTO<List<PlatformRuntimeReloadResultView>> listResults(String tag);

    ResponseDTO<String> updateItem(PlatformRuntimeReloadUpdateCommand command);
}
