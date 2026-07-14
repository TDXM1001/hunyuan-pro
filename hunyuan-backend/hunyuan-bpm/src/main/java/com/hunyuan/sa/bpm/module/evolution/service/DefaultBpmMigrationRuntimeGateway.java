package com.hunyuan.sa.bpm.module.evolution.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyFacts;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultBpmMigrationRuntimeGateway implements BpmMigrationRuntimeGateway {
    @Resource private GraphDefinitionElementMappingDao mappingDao;
    @Resource private BpmTimeEventDao timeEventDao;
    @Resource private BpmExternalWaitDao externalWaitDao;
    @Resource private BpmSubProcessLinkDao subProcessLinkDao;
    @Resource private BpmCallbackRecordDao callbackRecordDao;
    @Resource private BpmTaskDao taskDao;
    @Resource private FlowableProcessInstanceGateway engineGateway;
    @Resource private MigrationSafetyEvaluator safetyEvaluator;
    @Resource private com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao versionDao;
    @Resource private MigrationDataMappingService dataMappingService;
    @Resource private MigrationVariableEvidenceService variableEvidenceService;
    @Resource private BpmProcessWorkingDataDao workingDataDao;

    @Override
    public MigrationSafetyAssessment assess(BpmInstanceEntity instance, GraphDefinitionVersionEntity target,
                                            Map<String, String> authoredNodeMappings, String dataMappingJson) {
        Set<String> activeActivityIds = engineGateway.activeActivityIds(instance.getEngineProcessInstanceId());
        Map<String, String> compiledMappings = compiledMappings(instance.getGraphDefinitionVersionId(),
                target.getGraphDefinitionVersionId(), activeActivityIds, authoredNodeMappings);
        int activeTasks = taskDao.selectList(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getInstanceId, instance.getInstanceId())
                .eq(BpmTaskEntity::getTaskState, BpmTaskStateEnum.PENDING.getValue()).last("FOR UPDATE")).size();
        activeTasks = Math.max(activeTasks, Math.toIntExact(engineGateway.activeTaskCount(instance.getEngineProcessInstanceId())));
        int pendingTimers = timeEventDao.selectList(Wrappers.<BpmTimeEventEntity>lambdaQuery()
                .eq(BpmTimeEventEntity::getInstanceId, instance.getInstanceId())
                .in(BpmTimeEventEntity::getEventStatus, "SCHEDULED", "TRIGGERED", "FAILED_RETRYABLE", "FAILED_MANUAL")
                .last("FOR UPDATE")).size();
        int externalWaits = externalWaitDao.selectList(Wrappers.<BpmExternalWaitEntity>lambdaQuery()
                .eq(BpmExternalWaitEntity::getInstanceId, instance.getInstanceId())
                .in(BpmExternalWaitEntity::getWaitStatus, "WAITING", "FAILED_MANUAL")
                .last("FOR UPDATE")).size();
        int subProcesses = subProcessLinkDao.selectList(Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                .eq(BpmSubProcessLinkEntity::getParentInstanceId, instance.getInstanceId())
                .in(BpmSubProcessLinkEntity::getLinkStatus, "WAITING", "FAILED_MANUAL", "FAILED_PAUSED")
                .last("FOR UPDATE")).size();
        subProcesses = Math.max(subProcesses,
                engineGateway.activeChildProcessInstanceIds(instance.getEngineProcessInstanceId()).size());
        int sideEffects = callbackRecordDao.selectList(Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                .eq(BpmCallbackRecordEntity::getInstanceId, instance.getInstanceId())
                .in(BpmCallbackRecordEntity::getCallbackStatus,
                        BpmCallbackStatusEnum.SUCCEEDED.getValue(), BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue())
                .last("FOR UPDATE")).size();
        GraphDefinitionVersionEntity sourceVersion = versionDao.selectById(instance.getGraphDefinitionVersionId());
        BpmProcessWorkingDataEntity workingData = instance.getProcessWorkingDataId() == null
                ? null : workingDataDao.selectById(instance.getProcessWorkingDataId());
        MigrationDataMappingService.Validation dataValidation = sourceVersion == null
                ? new MigrationDataMappingService.Validation(false, List.of("源定义版本不存在"), Map.of(), Map.of(), Map.of())
                : dataMappingService.validateRuntime(sourceVersion.getDependencyVersionsJson(),
                        target.getDependencyVersionsJson(), dataMappingJson,
                        instance.getCurrentFormDataSnapshotJson(), workingData == null ? null : workingData.getDataJson(),
                        engineGateway.variables(instance.getEngineProcessInstanceId()));
        MigrationSafetyFacts facts = new MigrationSafetyFacts(
                BpmInstanceRunStateEnum.RUNNING.equalsValue(instance.getRunState()),
                activeTasks,
                Math.max(0, Math.toIntExact(engineGateway.activeActivityCount(instance.getEngineProcessInstanceId())) - 1),
                pendingTimers, externalWaits, subProcesses, sideEffects,
                compiledMappings.size() == activeActivityIds.size(), dataValidation.valid());
        MigrationSafetyAssessment assessment = safetyEvaluator.evaluate(facts);
        if (!dataValidation.valid()) {
            List<MigrationSafetyAssessment.Blocker> blockers = new java.util.ArrayList<>(assessment.blockers());
            dataValidation.reasons().forEach(reason -> blockers.add(
                    new MigrationSafetyAssessment.Blocker("DATA_MAPPING_INVALID", reason)));
            assessment = new MigrationSafetyAssessment(false, List.copyOf(blockers));
        }
        if (!assessment.eligible()) return assessment;
        List<String> engineErrors = engineGateway.validateMigration(instance.getEngineProcessInstanceId(),
                target.getEngineProcessDefinitionId(), compiledMappings);
        if (engineErrors.isEmpty()) return assessment;
        return new MigrationSafetyAssessment(false, engineErrors.stream()
                .map(message -> new MigrationSafetyAssessment.Blocker("ENGINE_VALIDATION_FAILED", message)).toList());
    }

    @Override
    public MigrationRuntimeEvidence migrate(BpmInstanceEntity instance, GraphDefinitionVersionEntity target,
                                            Map<String, String> authoredNodeMappings, String dataMappingJson) {
        Set<String> active = engineGateway.activeActivityIds(instance.getEngineProcessInstanceId());
        Map<String, String> compiled = compiledMappings(instance.getGraphDefinitionVersionId(),
                target.getGraphDefinitionVersionId(), active, authoredNodeMappings);
        if (compiled.size() != active.size()) throw new IllegalStateException("执行时节点映射不完整");
        MigrationDataMappingService.MappingPlan plan = dataMappingService.parse(dataMappingJson);
        Map<String, Object> variables = engineGateway.variables(instance.getEngineProcessInstanceId());
        MigrationRuntimeEvidence sourceEvidence = variableEvidenceService.source(variables, plan.variableMappings());
        Map<String, Object> migratedVariables = new LinkedHashMap<>();
        plan.variableMappings().forEach((source, targetName) -> {
            if (variables.containsKey(source)) {
                migratedVariables.put(targetName, variables.get(source));
            }
        });
        if (instance.getCurrentFormDataSnapshotJson() != null) {
            String mappedForm = dataMappingService.applyJson(instance.getCurrentFormDataSnapshotJson(), plan.fieldMappings());
            migratedVariables.put("formDataJson", mappedForm);
            migratedVariables.put("formData", com.alibaba.fastjson.JSON.parseObject(mappedForm));
        }
        engineGateway.migrate(instance.getEngineProcessInstanceId(), target.getEngineProcessDefinitionId(), compiled,
                migratedVariables);
        String observed = engineGateway.currentProcessDefinitionId(instance.getEngineProcessInstanceId());
        if (!target.getEngineProcessDefinitionId().equals(observed)) {
            throw new IllegalStateException("Flowable 迁移后目标定义观测不一致: " + observed);
        }
        MigrationRuntimeEvidence targetEvidence = inspectTargetVariables(instance, dataMappingJson);
        return new MigrationRuntimeEvidence(sourceEvidence.mappedVariableNames(),
                sourceEvidence.sourceVariablesDigest(), targetEvidence.targetVariablesDigest());
    }

    @Override
    public MigrationRuntimeEvidence inspectSourceVariables(BpmInstanceEntity instance, String dataMappingJson) {
        MigrationDataMappingService.MappingPlan plan = dataMappingService.parse(dataMappingJson);
        return variableEvidenceService.source(engineGateway.variables(instance.getEngineProcessInstanceId()),
                plan.variableMappings());
    }

    @Override
    public MigrationRuntimeEvidence inspectTargetVariables(BpmInstanceEntity instance, String dataMappingJson) {
        MigrationDataMappingService.MappingPlan plan = dataMappingService.parse(dataMappingJson);
        return variableEvidenceService.target(engineGateway.variables(instance.getEngineProcessInstanceId()),
                plan.variableMappings());
    }

    @Override
    public String currentEngineDefinitionId(BpmInstanceEntity instance) {
        return engineGateway.currentProcessDefinitionId(instance.getEngineProcessInstanceId());
    }

    private Map<String, String> compiledMappings(Long sourceVersionId, Long targetVersionId,
                                                 Set<String> activeCompiledIds, Map<String, String> authoredMappings) {
        List<GraphDefinitionElementMappingEntity> source = mappings(sourceVersionId);
        List<GraphDefinitionElementMappingEntity> target = mappings(targetVersionId);
        Map<String, GraphDefinitionElementMappingEntity> sourceByCompiled = source.stream().collect(Collectors.toMap(
                GraphDefinitionElementMappingEntity::getCompiledElementId, Function.identity(), (a, b) -> a));
        Map<String, GraphDefinitionElementMappingEntity> targetByAuthored = target.stream().collect(Collectors.toMap(
                GraphDefinitionElementMappingEntity::getAuthoredElementId, Function.identity(), (a, b) -> a));
        Map<String, String> result = new LinkedHashMap<>();
        for (String activeId : activeCompiledIds) {
            GraphDefinitionElementMappingEntity sourceMapping = sourceByCompiled.get(activeId);
            if (sourceMapping == null) continue;
            String targetAuthored = authoredMappings.getOrDefault(sourceMapping.getAuthoredElementId(),
                    sourceMapping.getAuthoredElementId());
            GraphDefinitionElementMappingEntity targetMapping = targetByAuthored.get(targetAuthored);
            if (targetMapping != null) result.put(activeId, targetMapping.getCompiledElementId());
        }
        return result;
    }

    private List<GraphDefinitionElementMappingEntity> mappings(Long versionId) {
        return mappingDao.selectList(Wrappers.<GraphDefinitionElementMappingEntity>lambdaQuery()
                .eq(GraphDefinitionElementMappingEntity::getGraphDefinitionVersionId, versionId));
    }

}
