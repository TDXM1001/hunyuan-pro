package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 外部连接器命令的独立提交边界。 */
@Service
public class BpmConnectorCommandStore {

    @Resource
    private BpmCommandRecordDao bpmCommandRecordDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BpmCommandRecordEntity loadOrCreate(String commandKey, Long instanceId, String requestJson) {
        BpmCommandRecordEntity existing = bpmCommandRecordDao.selectOne(
                Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                        .eq(BpmCommandRecordEntity::getCommandKey, commandKey).last("LIMIT 1"));
        if (existing != null) return existing;
        BpmCommandRecordEntity command = new BpmCommandRecordEntity();
        command.setCommandKey(commandKey);
        command.setCommandType("M5_CONNECTOR_INVOKE");
        command.setInstanceId(instanceId);
        command.setCommandStatus(0);
        command.setAttemptCount(0);
        command.setRequestPayloadJson(requestJson);
        bpmCommandRecordDao.insert(command);
        return command;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSucceeded(Long commandId, int attempt, String responseJson) {
        BpmCommandRecordEntity update = new BpmCommandRecordEntity();
        update.setCommandRecordId(commandId);
        update.setCommandStatus(1);
        update.setAttemptCount(attempt);
        update.setResponsePayloadJson(responseJson);
        update.setFailureReason(null);
        update.setNextRetryAt(null);
        bpmCommandRecordDao.updateById(update);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long commandId, int attempt, String error) {
        BpmCommandRecordEntity update = new BpmCommandRecordEntity();
        update.setCommandRecordId(commandId);
        update.setCommandStatus(2);
        update.setAttemptCount(attempt);
        update.setFailureReason(error);
        update.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(300, 30L * attempt)));
        bpmCommandRecordDao.updateById(update);
    }
}
