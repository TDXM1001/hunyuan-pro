package com.hunyuan.sa.bpm.api.business;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmEventSubscriptionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Hunyuan BPM 业务接入 API。
 */
@Service
public class BpmBusinessProcessApiImpl implements BpmBusinessProcessApi {

    private static final int CALLBACK_STATUS_PENDING = 0;

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private BpmEventSubscriptionService bpmEventSubscriptionService;

    @Override
    public void publishResultEvent(BpmBusinessResultEvent event) {
        validateResultEvent(event);
        if (bpmEventSubscriptionService != null && bpmEventSubscriptionService.enqueue(event) > 0) {
            return;
        }
        BpmCallbackRecordEntity existingRecord = bpmCallbackRecordDao.selectOne(
                Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .eq(BpmCallbackRecordEntity::getEventId, event.getEventId())
        );
        if (existingRecord != null) {
            return;
        }

        BpmCallbackRecordEntity callbackRecord = new BpmCallbackRecordEntity();
        callbackRecord.setEventId(event.getEventId());
        callbackRecord.setInstanceId(event.getInstanceId());
        callbackRecord.setBusinessType(event.getBusinessType());
        callbackRecord.setBusinessId(event.getBusinessId());
        callbackRecord.setBusinessKey(event.getBusinessKey());
        callbackRecord.setCallbackStatus(CALLBACK_STATUS_PENDING);
        callbackRecord.setRequestPayloadJson(StringUtils.hasText(event.getPayloadJson())
                ? event.getPayloadJson()
                : JSON.toJSONString(event));
        callbackRecord.setRetryCount(0);
        callbackRecord.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.insert(callbackRecord);
    }

    private void validateResultEvent(BpmBusinessResultEvent event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            throw new IllegalArgumentException("eventId不能为空");
        }
        if (event.getInstanceId() == null) {
            throw new IllegalArgumentException("instanceId不能为空");
        }
        if (!StringUtils.hasText(event.getBusinessType())) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        if (event.getBusinessId() == null && !StringUtils.hasText(event.getBusinessKey())) {
            throw new IllegalArgumentException("businessId与businessKey不能同时为空");
        }
    }

}
