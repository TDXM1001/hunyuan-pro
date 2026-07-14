package com.hunyuan.sa.bpm.api.business;

import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;

/**
 * 业务模块接入 Hunyuan BPM 的公共 API。
 */
public interface BpmBusinessProcessApi {

    void publishResultEvent(BpmBusinessResultEvent event);
}
