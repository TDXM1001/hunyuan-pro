package com.hunyuan.sa.base.module.support.heartbeat.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台心跳记录只读公开边界。
 */
public interface PlatformHeartbeatFacade {

    ResponseDTO<PageResult<PlatformHeartbeatRecordView>> queryRecords(
            PlatformHeartbeatPageQuery query);
}
