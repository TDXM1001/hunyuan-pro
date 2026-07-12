package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.common.enumeration.BpmTimeEventStatusEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTimeEventQueryForm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 时间事件运营查询和人工重试。
 */
@Service
public class BpmTimeEventOperationsService {

    @Resource
    private BpmTimeEventDao bpmTimeEventDao;

    @Resource
    private BpmRuntimeCommandCoordinator bpmRuntimeCommandCoordinator;

    public ResponseDTO<PageResult<BpmTimeEventEntity>> queryPage(BpmTimeEventQueryForm form) {
        Page<BpmTimeEventEntity> page = (Page<BpmTimeEventEntity>) SmartPageUtil.convert2PageQuery(form);
        var query = Wrappers.<BpmTimeEventEntity>lambdaQuery()
                .eq(form.getInstanceId() != null, BpmTimeEventEntity::getInstanceId, form.getInstanceId())
                .eq(StringUtils.hasText(form.getEventKind()), BpmTimeEventEntity::getEventKind, form.getEventKind())
                .eq(StringUtils.hasText(form.getEventStatus()), BpmTimeEventEntity::getEventStatus, form.getEventStatus())
                .orderByDesc(BpmTimeEventEntity::getScheduledAt);
        bpmTimeEventDao.selectPage(page, query);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> retry(Long timeEventId) {
        BpmTimeEventEntity event = bpmTimeEventDao.selectById(timeEventId);
        if (event == null) {
            return ResponseDTO.userErrorParam("时间事件不存在");
        }
        if (!BpmTimeEventStatusEnum.FAILED_RETRYABLE.name().equals(event.getEventStatus())
                && !BpmTimeEventStatusEnum.FAILED_MANUAL.name().equals(event.getEventStatus())) {
            return ResponseDTO.userErrorParam("只有失败事件可以人工重试");
        }
        try {
            bpmRuntimeCommandCoordinator.executeTimeEvent(event);
            BpmTimeEventEntity update = new BpmTimeEventEntity();
            update.setTimeEventId(timeEventId);
            update.setEventStatus(BpmTimeEventStatusEnum.SUCCEEDED.name());
            update.setCompletedAt(LocalDateTime.now());
            update.setLastError(null);
            bpmTimeEventDao.updateById(update);
            return ResponseDTO.ok();
        } catch (Exception ex) {
            BpmTimeEventEntity update = new BpmTimeEventEntity();
            update.setTimeEventId(timeEventId);
            update.setEventStatus(BpmTimeEventStatusEnum.FAILED_MANUAL.name());
            update.setLastError(ex.getMessage());
            bpmTimeEventDao.updateById(update);
            return ResponseDTO.userErrorParam(ex.getMessage());
        }
    }
}
