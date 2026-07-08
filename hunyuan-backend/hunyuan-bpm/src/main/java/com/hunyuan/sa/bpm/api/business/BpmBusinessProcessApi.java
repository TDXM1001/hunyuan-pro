package com.hunyuan.sa.bpm.api.business;

import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessInstanceStatus;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;

/**
 * 业务模块接入 Hunyuan BPM 的公共 API。
 */
public interface BpmBusinessProcessApi {

    Long start(BpmBusinessStartCommand command);

    BpmBusinessInstanceStatus getStatus(String businessType, Long businessId);

    void publishResultEvent(BpmBusinessResultEvent event);
}
