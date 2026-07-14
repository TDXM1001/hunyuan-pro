package com.hunyuan.sa.bpm.module.evolution.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationItemDao;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class BpmMigrationItemExecutor {
    @Resource private BpmInstanceDao instanceDao;
    @Resource private BpmMigrationRuntimeGateway runtimeGateway;
    @Resource private MigrationDataMappingService dataMappingService;
    @Resource private BpmProcessWorkingDataDao workingDataDao;
    @Resource private BpmMigrationItemDao itemDao;
    @Resource private BpmOrgIdentityGateway orgIdentityGateway;
    @Resource private GraphDefinitionVersionDao versionDao;

    public BpmMigrationItemExecutor() {
    }

    public BpmMigrationItemExecutor(BpmInstanceDao instanceDao, BpmMigrationRuntimeGateway runtimeGateway,
                                    BpmMigrationItemDao itemDao) {
        this.instanceDao = instanceDao;
        this.runtimeGateway = runtimeGateway;
        this.dataMappingService = new MigrationDataMappingService();
        this.itemDao = itemDao;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MigrationExecutionEvidence execute(Long itemId, Long instanceId, BpmMigrationBatchEntity batch,
                                              GraphDefinitionVersionEntity target,
                                              Map<String, String> mappings, String dataMappingJson,
                                              Long executedByEmployeeId) {
        BpmMigrationItemEntity item = itemDao.selectByIdForUpdate(itemId);
        if (item == null || !batch.getMigrationBatchId().equals(item.getMigrationBatchId())) {
            throw new IllegalStateException("迁移实例明细不存在或批次不匹配");
        }
        if ("SUCCEEDED".equals(item.getItemStatus())) {
            return new MigrationExecutionEvidence(item.getTargetSnapshotJson(), item.getEngineCommandEvidenceJson());
        }
        BpmInstanceEntity instance = instanceDao.selectByIdForUpdate(instanceId);
        GraphDefinitionVersionEntity lockedTarget = versionDao == null ? target
                : versionDao.selectByIdForUpdate(target.getGraphDefinitionVersionId());
        if (lockedTarget == null || !"ACTIVE".equals(lockedTarget.getLifecycleState())
                || !target.getEngineProcessDefinitionId().equals(lockedTarget.getEngineProcessDefinitionId())) {
            throw new IllegalStateException("执行前目标版本已不再是激活发布版本");
        }
        if (instance != null && batch.getTargetVersionId().equals(instance.getGraphDefinitionVersionId())) {
            String observed = runtimeGateway.currentEngineDefinitionId(instance);
            if (!target.getEngineProcessDefinitionId().equals(observed)) {
                throw new IllegalStateException("平台投影已是目标版本但 Flowable 观测不一致: " + observed);
            }
            return markSucceeded(item, instance, batch, observed, executedByEmployeeId, true,
                    recoveredVariableEvidence(item, instance, dataMappingJson));
        }
        if (instance == null || !batch.getSourceVersionId().equals(instance.getGraphDefinitionVersionId())) {
            throw new IllegalStateException("执行前实例源版本已变化");
        }
        MigrationSafetyAssessment assessment = runtimeGateway.assess(instance, target, mappings, dataMappingJson);
        if (!assessment.eligible()) {
            throw new MigrationSafetyChangedException(assessment);
        }
        BpmMigrationRuntimeGateway.MigrationRuntimeEvidence runtimeEvidence =
                runtimeGateway.migrate(instance, target, mappings, dataMappingJson);
        applyPlatformDataMapping(instance, dataMappingJson, executedByEmployeeId);
        if (instanceDao.updateMigrationProjection(instance.getInstanceId(), batch.getSourceVersionId(),
                batch.getTargetVersionId(), target.getEngineProcessDefinitionId()) != 1) {
            throw new IllegalStateException("引擎迁移后平台实例投影并发更新失败，需要人工对账");
        }
        String observedDefinitionId = runtimeGateway.currentEngineDefinitionId(instance);
        if (!target.getEngineProcessDefinitionId().equals(observedDefinitionId)) {
            throw new IllegalStateException("迁移后引擎定义观测不一致: " + observedDefinitionId);
        }
        return markSucceeded(item, instance, batch, observedDefinitionId, executedByEmployeeId, false,
                runtimeEvidence == null ? BpmMigrationRuntimeGateway.MigrationRuntimeEvidence.empty() : runtimeEvidence);
    }

    private BpmMigrationRuntimeGateway.MigrationRuntimeEvidence recoveredVariableEvidence(
            BpmMigrationItemEntity item, BpmInstanceEntity instance, String dataMappingJson) {
        MigrationDataMappingService.MappingPlan plan = dataMappingService.parse(dataMappingJson);
        if (plan.variableMappings().isEmpty()) {
            return runtimeGateway.inspectTargetVariables(instance, dataMappingJson);
        }
        com.alibaba.fastjson.JSONObject snapshot = JSON.parseObject(item.getSourceSnapshotJson());
        Map<String, String> names = new java.util.TreeMap<>();
        com.alibaba.fastjson.JSONObject storedNames = snapshot == null ? null
                : snapshot.getJSONObject("mappedVariableNames");
        if (storedNames != null) storedNames.forEach((source, target) -> names.put(source, String.valueOf(target)));
        String sourceDigest = snapshot == null ? null : snapshot.getString("sourceVariablesDigest");
        BpmMigrationRuntimeGateway.MigrationRuntimeEvidence targetEvidence =
                runtimeGateway.inspectTargetVariables(instance, dataMappingJson);
        if (!plan.variableMappings().equals(names) || sourceDigest == null || sourceDigest.isBlank()
                || targetEvidence == null || targetEvidence.targetVariablesDigest() == null
                || targetEvidence.targetVariablesDigest().isBlank()) {
            throw new IllegalStateException("迁移恢复缺少完整变量审计证据，需要人工对账");
        }
        return new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(names, sourceDigest,
                targetEvidence.targetVariablesDigest());
    }

    private MigrationExecutionEvidence markSucceeded(BpmMigrationItemEntity item, BpmInstanceEntity instance,
                                                      BpmMigrationBatchEntity batch, String observedDefinitionId,
                                                      Long executedByEmployeeId, boolean recovered,
                                                      BpmMigrationRuntimeGateway.MigrationRuntimeEvidence runtimeEvidence) {
        Map<String, Object> targetFacts = new java.util.LinkedHashMap<>();
        targetFacts.put("graphDefinitionVersionId", batch.getTargetVersionId());
        targetFacts.put("engineProcessDefinitionId", observedDefinitionId);
        targetFacts.put("processWorkingDataId", instance.getProcessWorkingDataId());
        targetFacts.put("formDataVersion", instance.getFormDataVersion());
        targetFacts.put("currentFormDataDigest", DigestUtils.sha256Hex(
                instance.getCurrentFormDataSnapshotJson() == null ? "" : instance.getCurrentFormDataSnapshotJson()));
        String targetSnapshot = JSON.toJSONString(targetFacts);
        Map<String, Object> engineFacts = new java.util.LinkedHashMap<>();
        engineFacts.put("engineProcessInstanceId", instance.getEngineProcessInstanceId());
        engineFacts.put("observedProcessDefinitionId", observedDefinitionId);
        engineFacts.put("status", recovered ? "RECOVERED_AFTER_COMMIT" : "MIGRATED");
        engineFacts.put("mappedVariableNames", runtimeEvidence.mappedVariableNames());
        engineFacts.put("sourceVariablesDigest", runtimeEvidence.sourceVariablesDigest());
        engineFacts.put("targetVariablesDigest", runtimeEvidence.targetVariablesDigest());
        String evidence = JSON.toJSONString(engineFacts);
        item.setItemStatus("SUCCEEDED"); item.setTargetSnapshotJson(targetSnapshot);
        item.setEngineCommandEvidenceJson(evidence); item.setExecutedByEmployeeId(executedByEmployeeId);
        item.setMigratedAt(java.time.LocalDateTime.now()); itemDao.updateById(item);
        return new MigrationExecutionEvidence(targetSnapshot, evidence);
    }

    private void applyPlatformDataMapping(BpmInstanceEntity instance, String dataMappingJson, Long actorEmployeeId) {
        MigrationDataMappingService.MappingPlan plan = dataMappingService.parse(dataMappingJson);
        String mappedForm = dataMappingService.applyJson(instance.getCurrentFormDataSnapshotJson(), plan.fieldMappings());
        if (mappedForm != null && !mappedForm.equals(instance.getCurrentFormDataSnapshotJson())) {
            instance.setCurrentFormDataSnapshotJson(mappedForm);
        }
        if (instance.getProcessWorkingDataId() != null && !plan.workingDataMappings().isEmpty() && workingDataDao != null) {
            BpmProcessWorkingDataEntity current = workingDataDao.selectById(instance.getProcessWorkingDataId());
            if (current != null) {
                String mapped = dataMappingService.applyJson(current.getDataJson(), plan.workingDataMappings());
                if (!mapped.equals(current.getDataJson())) {
                    BpmProcessWorkingDataEntity next = new BpmProcessWorkingDataEntity();
                    next.setApprovalSubjectSnapshotId(current.getApprovalSubjectSnapshotId());
                    next.setDataVersion(current.getDataVersion() + 1);
                    next.setDataJson(mapped); next.setActorEmployeeId(actorEmployeeId);
                    next.setActorNameSnapshot(actorName(actorEmployeeId)); next.setChangeReason("定义迁移数据映射");
                    next.setPreviousDataVersion(current.getDataVersion()); next.setDataDigest(DigestUtils.sha256Hex(mapped));
                    workingDataDao.insert(next);
                    instance.setProcessWorkingDataId(next.getProcessWorkingDataId());
                    instance.setFormDataVersion(next.getDataVersion());
                }
            }
        }
        instanceDao.updateById(instance);
    }

    private String actorName(Long actorEmployeeId) {
        return orgIdentityGateway == null ? "EMPLOYEE#" + actorEmployeeId
                : orgIdentityGateway.requireEmployee(actorEmployeeId).actualName();
    }

    public record MigrationExecutionEvidence(String targetSnapshotJson, String engineCommandEvidenceJson) {
    }

    public static class MigrationSafetyChangedException extends IllegalStateException {
        private final MigrationSafetyAssessment assessment;

        public MigrationSafetyChangedException(MigrationSafetyAssessment assessment) {
            super("执行前实例状态已变化: " + assessment.blockers());
            this.assessment = assessment;
        }

        public MigrationSafetyAssessment getAssessment() {
            return assessment;
        }
    }
}
