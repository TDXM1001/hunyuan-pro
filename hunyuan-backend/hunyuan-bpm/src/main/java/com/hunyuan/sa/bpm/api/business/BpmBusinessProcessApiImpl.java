package com.hunyuan.sa.bpm.api.business;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessInstanceStatus;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Hunyuan BPM 业务接入 API。
 */
@Service
public class BpmBusinessProcessApiImpl implements BpmBusinessProcessApi {

    private static final int COMMAND_STATUS_PENDING = 0;

    private static final int COMMAND_STATUS_SUCCEEDED = 1;

    private static final int COMMAND_STATUS_FAILED = 2;

    private static final int CALLBACK_STATUS_PENDING = 0;

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmCommandRecordDao bpmCommandRecordDao;

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Override
    public Long start(BpmBusinessStartCommand command) {
        validateStartCommand(command);
        String commandKey = buildStartCommandKey(command);
        BpmCommandRecordEntity existingRecord = bpmCommandRecordDao.selectOne(
                Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                        .eq(BpmCommandRecordEntity::getCommandKey, commandKey)
        );
        if (existingRecord != null) {
            if (COMMAND_STATUS_SUCCEEDED == existingRecord.getCommandStatus()
                    && existingRecord.getInstanceId() != null) {
                return existingRecord.getInstanceId();
            }
            throw new IllegalStateException("业务流程命令已存在但尚未成功");
        }

        BpmCommandRecordEntity commandRecord = buildStartCommandRecord(command, commandKey);
        bpmCommandRecordDao.insert(commandRecord);
        try {
            ResponseDTO<Long> response = bpmInstanceService.startBusinessInstance(command);
            if (!Boolean.TRUE.equals(response.getOk())) {
                throw new IllegalStateException(response.getMsg());
            }
            updateCommandRecord(commandRecord.getCommandRecordId(), response.getData(), COMMAND_STATUS_SUCCEEDED, null);
            return response.getData();
        } catch (RuntimeException ex) {
            updateCommandRecord(commandRecord.getCommandRecordId(), null, COMMAND_STATUS_FAILED, limitFailureReason(ex));
            throw ex;
        }
    }

    @Override
    public BpmBusinessInstanceStatus getStatus(String businessType, Long businessId) {
        validateBusinessKey(businessType, businessId);
        BpmInstanceEntity instance = bpmInstanceDao.selectOne(Wrappers.<BpmInstanceEntity>lambdaQuery()
                .eq(BpmInstanceEntity::getBusinessType, businessType)
                .eq(BpmInstanceEntity::getBusinessId, businessId)
                .orderByDesc(BpmInstanceEntity::getInstanceId)
                .last("LIMIT 1"));
        if (instance == null) {
            return null;
        }

        BpmBusinessInstanceStatus status = new BpmBusinessInstanceStatus();
        status.setInstanceId(instance.getInstanceId());
        status.setInstanceNo(instance.getInstanceNo());
        status.setBusinessType(instance.getBusinessType());
        status.setBusinessId(instance.getBusinessId());
        status.setRunState(instance.getRunState());
        status.setResultState(instance.getResultState());
        status.setLastActionAt(instance.getLastActionAt());
        return status;
    }

    @Override
    public void publishResultEvent(BpmBusinessResultEvent event) {
        validateResultEvent(event);
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
        callbackRecord.setCallbackStatus(CALLBACK_STATUS_PENDING);
        callbackRecord.setRequestPayloadJson(StringUtils.hasText(event.getPayloadJson())
                ? event.getPayloadJson()
                : JSON.toJSONString(event));
        callbackRecord.setRetryCount(0);
        callbackRecord.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.insert(callbackRecord);
    }

    private BpmCommandRecordEntity buildStartCommandRecord(BpmBusinessStartCommand command, String commandKey) {
        BpmCommandRecordEntity commandRecord = new BpmCommandRecordEntity();
        commandRecord.setCommandKey(commandKey);
        commandRecord.setCommandType("START");
        commandRecord.setBusinessType(command.getBusinessType());
        commandRecord.setBusinessId(command.getBusinessId());
        commandRecord.setCommandStatus(COMMAND_STATUS_PENDING);
        commandRecord.setRequestPayloadJson(JSON.toJSONString(command));
        commandRecord.setUpdateTime(LocalDateTime.now());
        return commandRecord;
    }

    private void updateCommandRecord(Long commandRecordId, Long instanceId, Integer commandStatus, String failureReason) {
        BpmCommandRecordEntity update = new BpmCommandRecordEntity();
        update.setCommandRecordId(commandRecordId);
        update.setInstanceId(instanceId);
        update.setCommandStatus(commandStatus);
        update.setFailureReason(failureReason);
        update.setUpdateTime(LocalDateTime.now());
        bpmCommandRecordDao.updateById(update);
    }

    private String buildStartCommandKey(BpmBusinessStartCommand command) {
        return "START:%s:%s:%s".formatted(
                command.getBusinessType(),
                command.getBusinessId(),
                command.getDefinitionKey()
        );
    }

    private void validateStartCommand(BpmBusinessStartCommand command) {
        if (command == null || !StringUtils.hasText(command.getBusinessType())) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        if (command.getBusinessId() == null) {
            throw new IllegalArgumentException("businessId不能为空");
        }
        if (!StringUtils.hasText(command.getDefinitionKey())) {
            throw new IllegalArgumentException("definitionKey不能为空");
        }
        if (command.getStartEmployeeId() == null) {
            throw new IllegalArgumentException("startEmployeeId不能为空");
        }
    }

    private void validateBusinessKey(String businessType, Long businessId) {
        if (!StringUtils.hasText(businessType)) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId不能为空");
        }
    }

    private void validateResultEvent(BpmBusinessResultEvent event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            throw new IllegalArgumentException("eventId不能为空");
        }
        if (event.getInstanceId() == null) {
            throw new IllegalArgumentException("instanceId不能为空");
        }
        validateBusinessKey(event.getBusinessType(), event.getBusinessId());
    }

    private String limitFailureReason(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
