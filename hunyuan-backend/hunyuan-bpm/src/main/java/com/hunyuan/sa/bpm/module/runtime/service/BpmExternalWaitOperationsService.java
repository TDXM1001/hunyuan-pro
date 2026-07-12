package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.common.enumeration.BpmExternalWaitStatusEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmExternalWaitQueryForm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 外部等待运营查询和人工处置。
 */
@Service
public class BpmExternalWaitOperationsService {

    @Resource
    private BpmExternalWaitDao bpmExternalWaitDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    public ResponseDTO<PageResult<BpmExternalWaitEntity>> queryPage(BpmExternalWaitQueryForm form) {
        Page<BpmExternalWaitEntity> page = (Page<BpmExternalWaitEntity>) SmartPageUtil.convert2PageQuery(form);
        var query = Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                .select(BpmExternalWaitEntity.class, info -> !"callback_token_hash".equals(info.getColumn()))
                .eq(form.getInstanceId() != null, BpmExternalWaitEntity::getInstanceId, form.getInstanceId())
                .eq(StringUtils.hasText(form.getConnectorKey()), BpmExternalWaitEntity::getConnectorKey, form.getConnectorKey())
                .eq(StringUtils.hasText(form.getWaitStatus()), BpmExternalWaitEntity::getWaitStatus, form.getWaitStatus())
                .orderByDesc(BpmExternalWaitEntity::getCreateTime);
        bpmExternalWaitDao.selectPage(page, query);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> retry(Long externalWaitId) {
        BpmExternalWaitEntity wait = bpmExternalWaitDao.selectById(externalWaitId);
        if (wait == null || !BpmExternalWaitStatusEnum.FAILED_MANUAL.name().equals(wait.getWaitStatus())) {
            return ResponseDTO.userErrorParam("只有人工处置状态可以重试");
        }
        return triggerAndMark(wait, BpmExternalWaitStatusEnum.RESUMED, "externalCallbackPayload", wait.getCallbackPayloadSnapshotJson());
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> cancel(Long externalWaitId) {
        BpmExternalWaitEntity wait = bpmExternalWaitDao.selectById(externalWaitId);
        if (wait == null || !BpmExternalWaitStatusEnum.WAITING.name().equals(wait.getWaitStatus())) {
            return ResponseDTO.userErrorParam("只有等待中的记录可以取消");
        }
        return triggerAndMark(wait, BpmExternalWaitStatusEnum.CANCELLED, "externalWaitCancelled", true);
    }

    private ResponseDTO<String> triggerAndMark(
            BpmExternalWaitEntity wait,
            BpmExternalWaitStatusEnum targetStatus,
            String variableName,
            Object variableValue
    ) {
        BpmExternalWaitEntity claim = new BpmExternalWaitEntity();
        claim.setWaitStatus(targetStatus.name());
        if (targetStatus == BpmExternalWaitStatusEnum.CANCELLED) {
            claim.setCancelledAt(LocalDateTime.now());
        } else {
            claim.setResumedAt(LocalDateTime.now());
        }
        int claimed = bpmExternalWaitDao.update(claim, Wrappers.<BpmExternalWaitEntity>lambdaUpdate()
                .eq(BpmExternalWaitEntity::getExternalWaitId, wait.getExternalWaitId())
                .eq(BpmExternalWaitEntity::getWaitStatus, wait.getWaitStatus()));
        if (claimed != 1) {
            return ResponseDTO.userErrorParam("等待记录已由其他动作处理");
        }
        flowableProcessInstanceGateway.trigger(wait.getEngineExecutionId(), variableName, variableValue);
        return ResponseDTO.ok();
    }
}
