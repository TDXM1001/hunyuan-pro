package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 外部发起命令的独立持久化边界。 */
@Service
public class BpmExternalStartCommandStore {

    private final BpmCommandRecordDao commandRecordDao;

    public BpmExternalStartCommandStore(BpmCommandRecordDao commandRecordDao) {
        this.commandRecordDao = commandRecordDao;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Claim loadOrCreate(String commandKey, String businessType, String requestJson) {
        BpmCommandRecordEntity existing = find(commandKey);
        if (existing != null) {
            return new Claim(existing, false);
        }
        BpmCommandRecordEntity command = new BpmCommandRecordEntity();
        command.setCommandKey(commandKey);
        command.setCommandType("EXTERNAL_START");
        command.setBusinessType(businessType);
        command.setCommandStatus(0);
        command.setRequestPayloadJson(requestJson);
        try {
            commandRecordDao.insert(command);
            return new Claim(command, true);
        } catch (DuplicateKeyException ignored) {
            return new Claim(find(commandKey), false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSucceeded(Long commandId, Long instanceId) {
        BpmCommandRecordEntity update = new BpmCommandRecordEntity();
        update.setCommandRecordId(commandId);
        update.setInstanceId(instanceId);
        update.setCommandStatus(1);
        update.setResponsePayloadJson("{\"instanceNo\":\"BP-" + instanceId + "\"}");
        update.setFailureReason(null);
        update.setUpdateTime(LocalDateTime.now());
        commandRecordDao.updateById(update);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long commandId, String reason) {
        BpmCommandRecordEntity update = new BpmCommandRecordEntity();
        update.setCommandRecordId(commandId);
        update.setCommandStatus(2);
        update.setFailureReason(reason);
        update.setUpdateTime(LocalDateTime.now());
        commandRecordDao.updateById(update);
    }

    private BpmCommandRecordEntity find(String commandKey) {
        return commandRecordDao.selectOne(Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                .eq(BpmCommandRecordEntity::getCommandKey, commandKey)
                .last("LIMIT 1"));
    }

    public record Claim(BpmCommandRecordEntity command, boolean owner) {
    }
}
