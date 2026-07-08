package com.hunyuan.sa.bpm.module.integration.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * BPM 业务回调记录服务。
 */
@Service
public class BpmBusinessCallbackService {

    private static final int CALLBACK_STATUS_SUCCEEDED = 1;

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    public ResponseDTO<String> retry(Long callbackRecordId) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return ResponseDTO.userErrorParam("回调记录不存在");
        }
        if (CALLBACK_STATUS_SUCCEEDED == record.getCallbackStatus()) {
            return ResponseDTO.ok();
        }

        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackRecordId(callbackRecordId);
        update.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.updateById(update);
        return ResponseDTO.ok();
    }
}
