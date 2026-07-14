package com.hunyuan.sa.bpm.module.operations.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsCaseDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsRetentionPolicyDao;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsRetentionPolicyEntity;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmNotificationRecordDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 M4-M6 结构化失败事实投影为 M7 统一异常工单。
 */
@Service
public class BpmOperationsProjectionService {

    @Resource private BpmOperationsCaseDao operationsCaseDao;
    @Resource private BpmTimeEventDao timeEventDao;
    @Resource private BpmExternalWaitDao externalWaitDao;
    @Resource private BpmCallbackRecordDao callbackRecordDao;
    @Resource private BpmCommandRecordDao commandRecordDao;
    @Resource private BpmNotificationRecordDao notificationRecordDao;
    @Resource private BpmInstanceDao instanceDao;
    @Resource private BpmOperationsRetentionPolicyDao retentionPolicyDao;
    @Resource private BpmOrgIdentityGateway orgIdentityGateway;

    public BpmOperationsProjectionService() {
    }

    public BpmOperationsProjectionService(
            BpmOperationsCaseDao operationsCaseDao,
            BpmTimeEventDao timeEventDao,
            BpmExternalWaitDao externalWaitDao,
            BpmCallbackRecordDao callbackRecordDao,
            BpmCommandRecordDao commandRecordDao,
            BpmNotificationRecordDao notificationRecordDao,
            BpmInstanceDao instanceDao,
            BpmOperationsRetentionPolicyDao retentionPolicyDao,
            BpmOrgIdentityGateway orgIdentityGateway
    ) {
        this.operationsCaseDao = operationsCaseDao;
        this.timeEventDao = timeEventDao;
        this.externalWaitDao = externalWaitDao;
        this.callbackRecordDao = callbackRecordDao;
        this.commandRecordDao = commandRecordDao;
        this.notificationRecordDao = notificationRecordDao;
        this.instanceDao = instanceDao;
        this.retentionPolicyDao = retentionPolicyDao;
        this.orgIdentityGateway = orgIdentityGateway;
    }

    @Scheduled(fixedDelayString = "${hunyuan.bpm.m7.operations-scan-delay-ms:60000}")
    public void scheduledRefresh() {
        refresh();
    }

    @Transactional(rollbackFor = Exception.class)
    public int refresh() {
        List<FailureFact> facts = new ArrayList<>();
        timeEventDao.selectList(Wrappers.<BpmTimeEventEntity>lambdaQuery()
                        .in(BpmTimeEventEntity::getEventStatus, "FAILED_RETRYABLE", "FAILED_MANUAL"))
                .forEach(item -> facts.add(fromTimeEvent(item)));
        externalWaitDao.selectList(Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                        .in(BpmExternalWaitEntity::getWaitStatus, "TIMED_OUT", "FAILED_MANUAL"))
                .forEach(item -> facts.add(fromExternalWait(item)));
        callbackRecordDao.selectList(Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .in(BpmCallbackRecordEntity::getCallbackStatus, 2, 3))
                .forEach(item -> facts.add(fromCallback(item)));
        commandRecordDao.selectList(Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                        .eq(BpmCommandRecordEntity::getCommandStatus, 2))
                .forEach(item -> facts.add(fromCommand(item)));
        notificationRecordDao.selectList(Wrappers.<BpmNotificationRecordEntity>lambdaQuery()
                        .eq(BpmNotificationRecordEntity::getSendStatus, 2))
                .forEach(item -> facts.add(fromNotification(item)));

        Map<Long, BpmInstanceEntity> instances = loadInstances(facts);
        facts.forEach(fact -> upsert(fact, instances.get(fact.instanceId())));
        return facts.size();
    }

    private Map<Long, BpmInstanceEntity> loadInstances(List<FailureFact> facts) {
        Set<Long> instanceIds = new LinkedHashSet<>();
        facts.stream().map(FailureFact::instanceId).filter(java.util.Objects::nonNull).forEach(instanceIds::add);
        if (instanceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BpmInstanceEntity> result = new HashMap<>();
        for (BpmInstanceEntity instance : instanceDao.selectBatchIds((Collection<Long>) instanceIds)) {
            result.put(instance.getInstanceId(), instance);
        }
        return result;
    }

    private void upsert(FailureFact fact, BpmInstanceEntity instance) {
        BpmOperationsCaseEntity existing = operationsCaseDao.selectOne(
                Wrappers.<BpmOperationsCaseEntity>lambdaQuery()
                        .eq(BpmOperationsCaseEntity::getSourceType, fact.sourceType())
                        .eq(BpmOperationsCaseEntity::getSourceId, fact.sourceId())
        );
        BpmOperationsCaseEntity entity = existing == null ? new BpmOperationsCaseEntity() : existing;
        entity.setCaseCode("OPS-" + fact.sourceType() + "-" + fact.sourceId());
        entity.setSourceType(fact.sourceType());
        entity.setSourceId(fact.sourceId());
        entity.setEventId(fact.eventId());
        entity.setInstanceId(fact.instanceId());
        entity.setDefinitionId(fact.definitionId() != null ? fact.definitionId() : value(instance, BpmInstanceEntity::getDefinitionId));
        entity.setGraphDefinitionVersionId(fact.graphDefinitionVersionId() != null
                ? fact.graphDefinitionVersionId() : value(instance, BpmInstanceEntity::getGraphDefinitionVersionId));
        entity.setDefinitionNodeId(fact.definitionNodeId());
        entity.setOrganizationId(value(instance, BpmInstanceEntity::getStartDepartmentIdSnapshot));
        if (entity.getOrganizationId() != null) {
            entity.setAssigneeEmployeeId(orgIdentityGateway.resolveDepartmentManagerEmployeeId(entity.getOrganizationId()));
        }
        entity.setBusinessType(fact.businessType() != null ? fact.businessType() : value(instance, BpmInstanceEntity::getBusinessType));
        entity.setBusinessId(fact.businessId() != null ? fact.businessId() : value(instance, BpmInstanceEntity::getBusinessId));
        entity.setBusinessKey(fact.businessKey() != null ? fact.businessKey() : value(instance, BpmInstanceEntity::getBusinessKey));
        entity.setCaseStatus(existing == null ? "OPEN" : existing.getCaseStatus());
        entity.setSeverity(fact.severity());
        entity.setSlaLevel(fact.slaLevel());
        entity.setFailureCode(fact.failureCode());
        entity.setFailureReason(limit(fact.failureReason(), 1000));
        entity.setRetryableFlag(fact.retryable());
        entity.setCompensableFlag(fact.compensable());
        entity.setHighRiskFlag(fact.highRisk());
        entity.setLegalHoldFlag(existing != null && Boolean.TRUE.equals(existing.getLegalHoldFlag()));
        entity.setBusinessEvidenceRefCount(existing == null ? 0 : existing.getBusinessEvidenceRefCount());
        entity.setMigrationSourceRefCount(existing == null ? 0 : existing.getMigrationSourceRefCount());
        if (existing == null) {
            LocalDateTime openedAt = LocalDateTime.now();
            entity.setOpenedAt(openedAt);
            BpmOperationsRetentionPolicyEntity policy = resolveRetentionPolicy(entity);
            if (policy != null) {
                entity.setLegalHoldFlag(Boolean.TRUE.equals(policy.getLegalHoldFlag()));
                entity.setRetentionUntil(openedAt.plusDays(policy.getRetentionDays()));
            }
            operationsCaseDao.insert(entity);
        } else {
            operationsCaseDao.updateById(entity);
        }
    }

    private BpmOperationsRetentionPolicyEntity resolveRetentionPolicy(BpmOperationsCaseEntity entity) {
        List<BpmOperationsRetentionPolicyEntity> policies = retentionPolicyDao.selectList(
                Wrappers.<BpmOperationsRetentionPolicyEntity>lambdaQuery()
                        .eq(BpmOperationsRetentionPolicyEntity::getStatus, "ACTIVE")
                        .and(wrapper -> wrapper.isNull(BpmOperationsRetentionPolicyEntity::getDefinitionId)
                                .or().eq(BpmOperationsRetentionPolicyEntity::getDefinitionId, entity.getDefinitionId()))
                        .and(wrapper -> wrapper.isNull(BpmOperationsRetentionPolicyEntity::getBusinessType)
                                .or().eq(BpmOperationsRetentionPolicyEntity::getBusinessType, entity.getBusinessType()))
                        .orderByDesc(BpmOperationsRetentionPolicyEntity::getDefinitionId)
                        .orderByDesc(BpmOperationsRetentionPolicyEntity::getBusinessType)
        );
        return policies.isEmpty() ? null : policies.get(0);
    }

    private FailureFact fromTimeEvent(BpmTimeEventEntity item) {
        return new FailureFact("TIME_EVENT", item.getTimeEventId(), item.getEventKey(), item.getInstanceId(),
                item.getDefinitionId(), item.getGraphDefinitionVersionId(), string(item.getDefinitionNodeId()),
                null, null, null, item.getEventStatus(), item.getLastError(), true, false, false,
                "FAILED_MANUAL".equals(item.getEventStatus()) ? "HIGH" : "MEDIUM", "BREACHED");
    }

    private FailureFact fromExternalWait(BpmExternalWaitEntity item) {
        return new FailureFact("EXTERNAL_WAIT", item.getExternalWaitId(), item.getCorrelationKey(), item.getInstanceId(),
                item.getDefinitionId(), item.getGraphDefinitionVersionId(), string(item.getDefinitionNodeId()),
                null, null, null, item.getWaitStatus(), item.getLastError(),
                "FAILED_MANUAL".equals(item.getWaitStatus()), false, true, "HIGH", "BREACHED");
    }

    private FailureFact fromCallback(BpmCallbackRecordEntity item) {
        return new FailureFact("CALLBACK", item.getCallbackRecordId(), item.getEventId(), item.getInstanceId(),
                null, null, null, item.getBusinessType(), item.getBusinessId(), item.getBusinessKey(),
                item.getCallbackStatus() == 3 ? "CALLBACK_NEEDS_COMPENSATION" : "CALLBACK_FAILED",
                item.getFailureReason(), item.getCallbackStatus() == 2, item.getCallbackStatus() == 3,
                item.getCallbackStatus() == 3, "HIGH", "BREACHED");
    }

    private FailureFact fromCommand(BpmCommandRecordEntity item) {
        return new FailureFact("COMMAND", item.getCommandRecordId(), item.getCommandKey(), item.getInstanceId(),
                null, null, null, item.getBusinessType(), item.getBusinessId(), null,
                "COMMAND_FAILED", item.getFailureReason(), false, false, true, "HIGH", "BREACHED");
    }

    private FailureFact fromNotification(BpmNotificationRecordEntity item) {
        return new FailureFact("NOTIFICATION", item.getNotificationRecordId(), item.getEventKey(), item.getInstanceId(),
                item.getDefinitionId(), null, string(item.getDefinitionNodeId()), null, null, null,
                "NOTIFICATION_FAILED", item.getFailReason(), false, false, false, "MEDIUM", "WARNING");
    }

    private String string(Long value) {
        return value == null ? null : value.toString();
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private <T> T value(BpmInstanceEntity instance, java.util.function.Function<BpmInstanceEntity, T> getter) {
        return instance == null ? null : getter.apply(instance);
    }

    private record FailureFact(
            String sourceType, Long sourceId, String eventId, Long instanceId, Long definitionId,
            Long graphDefinitionVersionId, String definitionNodeId, String businessType, Long businessId,
            String businessKey, String failureCode, String failureReason, boolean retryable,
            boolean compensable, boolean highRisk, String severity, String slaLevel
    ) {
    }
}
