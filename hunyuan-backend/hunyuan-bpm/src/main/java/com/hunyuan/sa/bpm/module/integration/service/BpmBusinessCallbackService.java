package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * BPM 业务回调记录服务。
 */
@Service
public class BpmBusinessCallbackService {

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private BpmBusinessCallbackExecutor callbackExecutor;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    public ResponseDTO<String> retry(Long callbackRecordId) {
        BpmBusinessCallbackExecuteResult result = callbackExecutor.execute(
                callbackRecordId,
                BpmBusinessCallbackTriggerType.MANUAL
        );
        if (!result.processed() && "回调记录不存在".equals(result.message())) {
            return ResponseDTO.userErrorParam(result.message());
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> compensate(Long callbackRecordId, BpmCallbackCompensateForm form) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return ResponseDTO.userErrorParam("回调记录不存在");
        }
        if (!BpmCallbackStatusEnum.NEEDS_COMPENSATION.equalsValue(record.getCallbackStatus())) {
            return ResponseDTO.userErrorParam("只有需人工补偿的回调记录才能标记补偿");
        }
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackStatus(BpmCallbackStatusEnum.COMPENSATED.getValue());
        update.setCompensatedAt(LocalDateTime.now());
        update.setCompensatedBy(bpmCurrentActorProvider.requireCurrentEmployeeId());
        update.setCompensationReason(limit(form.getReason(), 500));
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.update(update, new UpdateWrapper<BpmCallbackRecordEntity>()
                .eq("callback_record_id", callbackRecordId)
                .set("next_retry_at", null));
        return ResponseDTO.ok();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
