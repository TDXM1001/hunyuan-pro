package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmNotificationSendStatusEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmNotificationRecordDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BPM 通知投递记录服务。
 */
@Service
public class BpmNotificationRecordService {

    @Resource
    private BpmNotificationRecordDao notificationRecordDao;

    public BpmNotificationRecordVO createPendingRecord(BpmNotificationCommand command, String channel) {
        BpmNotificationRecordEntity entity = new BpmNotificationRecordEntity();
        entity.setInstanceId(command.instanceId());
        entity.setTaskId(command.taskId());
        entity.setDefinitionId(command.definitionId());
        entity.setDefinitionNodeId(command.definitionNodeId());
        entity.setEventKey(command.eventKey());
        entity.setChannel(channel);
        entity.setReceiverEmployeeId(command.receiverEmployeeId());
        entity.setReceiverSnapshotJson(command.receiverSnapshotJson());
        entity.setTemplateCode(command.smsTemplateCode());
        entity.setTitle(command.title());
        entity.setContentSnapshot(StringUtils.left(command.content(), 1000));
        entity.setSendStatus(BpmNotificationSendStatusEnum.PENDING.getValue());
        entity.setRequestPayloadJson(buildRequestSnapshot(command, channel));
        notificationRecordDao.insert(entity);
        return toVO(entity);
    }

    public void markSuccess(Long notificationRecordId, String responseSnapshotJson) {
        if (notificationRecordId == null) {
            return;
        }
        BpmNotificationRecordEntity updateEntity = new BpmNotificationRecordEntity();
        updateEntity.setNotificationRecordId(notificationRecordId);
        updateEntity.setSendStatus(BpmNotificationSendStatusEnum.SUCCESS.getValue());
        updateEntity.setResponseSnapshotJson(responseSnapshotJson);
        updateEntity.setSentAt(LocalDateTime.now());
        notificationRecordDao.updateById(updateEntity);
    }

    public void markFail(Long notificationRecordId, String failReason) {
        if (notificationRecordId == null) {
            return;
        }
        BpmNotificationRecordEntity updateEntity = new BpmNotificationRecordEntity();
        updateEntity.setNotificationRecordId(notificationRecordId);
        updateEntity.setSendStatus(BpmNotificationSendStatusEnum.FAIL.getValue());
        updateEntity.setFailReason(StringUtils.left(failReason, 1000));
        updateEntity.setSentAt(LocalDateTime.now());
        notificationRecordDao.updateById(updateEntity);
    }

    public List<BpmNotificationRecordVO> queryByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return notificationRecordDao.selectList(Wrappers.<BpmNotificationRecordEntity>lambdaQuery()
                        .eq(BpmNotificationRecordEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmNotificationRecordEntity::getCreateTime, BpmNotificationRecordEntity::getNotificationRecordId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private String buildRequestSnapshot(BpmNotificationCommand command, String channel) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("channel", channel);
        snapshot.put("receiverEmployeeId", command.receiverEmployeeId());
        snapshot.put("title", command.title());
        snapshot.put("content", command.content());
        snapshot.put("smsTemplateCode", command.smsTemplateCode());
        return snapshot.toJSONString();
    }

    private BpmNotificationRecordVO toVO(BpmNotificationRecordEntity entity) {
        BpmNotificationRecordVO vo = new BpmNotificationRecordVO();
        vo.setNotificationRecordId(entity.getNotificationRecordId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setTaskId(entity.getTaskId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setDefinitionNodeId(entity.getDefinitionNodeId());
        vo.setEventKey(entity.getEventKey());
        vo.setChannel(entity.getChannel());
        vo.setReceiverEmployeeId(entity.getReceiverEmployeeId());
        vo.setReceiverSnapshotJson(entity.getReceiverSnapshotJson());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTitle(entity.getTitle());
        vo.setContentSnapshot(entity.getContentSnapshot());
        vo.setSendStatus(entity.getSendStatus());
        vo.setRequestPayloadJson(entity.getRequestPayloadJson());
        vo.setResponseSnapshotJson(entity.getResponseSnapshotJson());
        vo.setFailReason(entity.getFailReason());
        vo.setSentAt(entity.getSentAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
