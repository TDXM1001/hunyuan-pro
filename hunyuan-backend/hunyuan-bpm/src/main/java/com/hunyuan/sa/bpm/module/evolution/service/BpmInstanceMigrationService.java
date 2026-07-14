package com.hunyuan.sa.bpm.module.evolution.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationBatchDao;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationItemDao;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationPreviewForm;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationDispositionForm;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmMigrationBatchDetailVO;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmMigrationItemVO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.dao.DuplicateKeyException;

@Service
public class BpmInstanceMigrationService {
    private static final Set<String> TERMINAL = Set.of("SUCCEEDED", "PARTIAL_FAILED", "FAILED");

    @Resource private BpmMigrationBatchDao batchDao;
    @Resource private BpmMigrationItemDao itemDao;
    @Resource private BpmInstanceDao instanceDao;
    @Resource private GraphDefinitionVersionDao versionDao;
    @Resource private BpmMigrationRuntimeGateway runtimeGateway;
    @Resource private BpmCurrentActorProvider actorProvider;
    @Resource private BpmMigrationItemExecutor itemExecutor;

    public BpmInstanceMigrationService() {
    }

    public BpmInstanceMigrationService(BpmMigrationBatchDao batchDao, BpmMigrationItemDao itemDao,
                                       BpmInstanceDao instanceDao, GraphDefinitionVersionDao versionDao,
                                       BpmMigrationRuntimeGateway runtimeGateway, BpmCurrentActorProvider actorProvider) {
        this.batchDao = batchDao;
        this.itemDao = itemDao;
        this.instanceDao = instanceDao;
        this.versionDao = versionDao;
        this.runtimeGateway = runtimeGateway;
        this.actorProvider = actorProvider;
        this.itemExecutor = new BpmMigrationItemExecutor(instanceDao, runtimeGateway, itemDao);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<BpmMigrationBatchDetailVO> preview(BpmMigrationPreviewForm form) {
        String validation = validate(form);
        if (validation != null) return ResponseDTO.userErrorParam(validation);
        String requestHash = requestHash(form);
        BpmMigrationBatchEntity duplicate = batchDao.selectByIdempotencyKey(form.getIdempotencyKey());
        if (duplicate != null) return duplicateResult(duplicate, requestHash);

        GraphDefinitionVersionEntity source = versionDao.selectById(form.getSourceVersionId());
        GraphDefinitionVersionEntity target = versionDao.selectById(form.getTargetVersionId());
        if (source == null || target == null) return ResponseDTO.userErrorParam("源或目标 Graph 定义版本不存在");
        if (!source.getProcessKey().equals(target.getProcessKey())) return ResponseDTO.userErrorParam("只允许同一流程键的版本迁移");
        if (source.getDefinitionVersion() == null || target.getDefinitionVersion() == null
                || target.getDefinitionVersion() <= source.getDefinitionVersion()) {
            return ResponseDTO.userErrorParam("目标版本必须严格高于源版本");
        }
        if (!"ACTIVE".equals(target.getLifecycleState()) || target.getDeploymentId() == null
                || target.getDeploymentId().isBlank() || target.getEngineProcessDefinitionId() == null
                || target.getEngineProcessDefinitionId().isBlank()) {
            return ResponseDTO.userErrorParam("目标版本必须是已激活且已部署的发布版本");
        }

        List<BpmInstanceEntity> instances = instanceDao.selectByIdsForMigration(form.getInstanceIds());
        if (instances.size() != form.getInstanceIds().stream().distinct().count()) {
            return ResponseDTO.userErrorParam("迁移实例不存在或实例列表包含重复项");
        }
        if (instances.stream().anyMatch(value -> !form.getSourceVersionId().equals(value.getGraphDefinitionVersionId()))) {
            return ResponseDTO.userErrorParam("实例源版本与迁移批次不一致");
        }

        LocalDateTime now = LocalDateTime.now();
        BpmMigrationBatchEntity batch = new BpmMigrationBatchEntity();
        batch.setBatchCode("MIG-" + form.getIdempotencyKey());
        batch.setIdempotencyKey(form.getIdempotencyKey());
        batch.setSourceVersionId(form.getSourceVersionId());
        batch.setTargetVersionId(form.getTargetVersionId());
        batch.setBatchStatus("PREVIEWED");
        batch.setMappingJson(JSON.toJSONString(form.getNodeMappings()));
        batch.setDataMappingJson(form.getDataMappingJson());
        batch.setDiffSnapshotJson(JSON.toJSONString(Map.of("requestHash", requestHash)));
        batch.setReason(form.getReason());
        batch.setActorEmployeeId(actorProvider.requireCurrentEmployeeId());
        batch.setTotalCount(instances.size());
        batch.setPreviewedAt(now);
        try {
            batchDao.insert(batch);
        } catch (DuplicateKeyException ex) {
            BpmMigrationBatchEntity raced = batchDao.selectByIdempotencyKeyForUpdate(form.getIdempotencyKey());
            if (raced != null) return duplicateResult(raced, requestHash);
            throw ex;
        }

        int eligible = 0;
        List<BpmMigrationItemEntity> previewItems = new ArrayList<>();
        for (BpmInstanceEntity instance : instances) {
            MigrationSafetyAssessment assessment = runtimeGateway.assess(instance, target, form.getNodeMappings(),
                    form.getDataMappingJson());
            BpmMigrationItemEntity item = new BpmMigrationItemEntity();
            item.setMigrationBatchId(batch.getMigrationBatchId());
            item.setInstanceId(instance.getInstanceId());
            item.setIdempotencyKey(form.getIdempotencyKey() + ":" + instance.getInstanceId());
            item.setItemStatus(assessment.eligible() ? "ELIGIBLE" : "BLOCKED");
            item.setBlockersJson(JSON.toJSONString(assessment.blockers()));
            item.setSourceSnapshotJson(sourceSnapshot(instance,
                    runtimeGateway.inspectSourceVariables(instance, form.getDataMappingJson())));
            itemDao.insert(item);
            previewItems.add(item);
            if (assessment.eligible()) eligible++;
        }
        batch.setEligibleCount(eligible);
        batch.setBlockedCount(instances.size() - eligible);
        batch.setSucceededCount(0);
        batch.setFailedCount(0);
        batchDao.updateById(batch);
        return detail(batch, previewItems);
    }

    public ResponseDTO<BpmMigrationBatchDetailVO> execute(Long batchId) {
        BpmMigrationBatchEntity batch = batchDao.selectById(batchId);
        if (batch == null) return ResponseDTO.userErrorParam("迁移批次不存在");
        if (TERMINAL.contains(batch.getBatchStatus())) return detail(batch);
        GraphDefinitionVersionEntity target = versionDao.selectById(batch.getTargetVersionId());
        GraphDefinitionVersionEntity source = versionDao.selectById(batch.getSourceVersionId());
        if (source == null || target == null || source.getDefinitionVersion() == null
                || target.getDefinitionVersion() == null || target.getDefinitionVersion() <= source.getDefinitionVersion()
                || !"ACTIVE".equals(target.getLifecycleState()) || target.getEngineProcessDefinitionId() == null
                || target.getEngineProcessDefinitionId().isBlank()) {
            return ResponseDTO.userErrorParam("执行前目标版本已不再满足前向激活发布约束");
        }
        if (itemDao.selectByBatchId(batchId).stream().noneMatch(item -> "ELIGIBLE".equals(item.getItemStatus()))) {
            return ResponseDTO.userErrorParam("迁移批次没有可确认的合格实例");
        }
        String executionOwnerKey = java.util.UUID.randomUUID().toString();
        Long confirmedBy = actorProvider.requireCurrentEmployeeId();
        if (batchDao.claimForExecution(batchId, executionOwnerKey, confirmedBy) != 1) {
            BpmMigrationBatchEntity current = batchDao.selectById(batchId);
            if (current != null && TERMINAL.contains(current.getBatchStatus())) return detail(current);
            return ResponseDTO.userErrorParam("迁移批次正在执行，请稍后查询审计结果");
        }
        batch = batchDao.selectById(batchId);
        if (batch == null || !executionOwnerKey.equals(batch.getExecutionOwnerKey())) {
            return ResponseDTO.userErrorParam("迁移批次执行租约已丢失");
        }
        Map<String, String> mappings = parseMappings(batch.getMappingJson());
        List<BpmMigrationItemEntity> items = itemDao.selectByBatchId(batchId);
        for (BpmMigrationItemEntity item : items) {
            if (!"ELIGIBLE".equals(item.getItemStatus())) continue;
            if (batchDao.renewExecutionLease(batchId, executionOwnerKey) != 1) {
                return ResponseDTO.userErrorParam("迁移批次执行租约已丢失，已停止后续实例迁移");
            }
            try {
                BpmMigrationItemExecutor.MigrationExecutionEvidence evidence = itemExecutor.execute(
                        item.getMigrationItemId(), item.getInstanceId(), batch, target, mappings,
                        batch.getDataMappingJson(), confirmedBy);
                item.setItemStatus("SUCCEEDED");
                item.setTargetSnapshotJson(evidence.targetSnapshotJson());
                item.setEngineCommandEvidenceJson(evidence.engineCommandEvidenceJson());
                item.setMigratedAt(LocalDateTime.now());
            } catch (RuntimeException ex) {
                if (ex instanceof BpmMigrationItemExecutor.MigrationSafetyChangedException changed) {
                    item.setBlockersJson(JSON.toJSONString(changed.getAssessment().blockers()));
                }
                item.setItemStatus("FAILED");
                item.setFailureReason(ex.getMessage());
                item.setCompensationResult(null);
                item.setEngineCommandEvidenceJson(failureEvidence(item.getInstanceId(), ex));
            }
            if (!"SUCCEEDED".equals(item.getItemStatus())) itemDao.updateById(item);
        }
        List<BpmMigrationItemEntity> persisted = itemDao.selectByBatchId(batchId);
        int succeeded = (int) persisted.stream().filter(value -> "SUCCEEDED".equals(value.getItemStatus())).count();
        int failed = (int) persisted.stream().filter(value -> "FAILED".equals(value.getItemStatus())).count();
        String terminalStatus = failed == 0 ? "SUCCEEDED" : succeeded == 0 ? "FAILED" : "PARTIAL_FAILED";
        if (batchDao.finalizeExecution(batchId, executionOwnerKey, terminalStatus, succeeded, failed) != 1) {
            return ResponseDTO.userErrorParam("迁移批次执行租约已被其他执行者接管，请查询最终审计结果");
        }
        return detail(batchDao.selectById(batchId), persisted);
    }

    public ResponseDTO<BpmMigrationBatchDetailVO> detail(Long batchId) {
        BpmMigrationBatchEntity batch = batchDao.selectById(batchId);
        return batch == null ? ResponseDTO.userErrorParam("迁移批次不存在") : detail(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<BpmMigrationBatchDetailVO> dispose(Long itemId, BpmMigrationDispositionForm form) {
        if (form == null || form.getAction() == null || form.getReason() == null
                || form.getReason().trim().length() < 6) {
            return ResponseDTO.userErrorParam("失败处置动作和至少 6 个字符的原因不能为空");
        }
        if (form.getReason().trim().length() > 512) return ResponseDTO.userErrorParam("处置原因不能超过 512 个字符");
        BpmMigrationItemEntity item = itemDao.selectByIdForUpdate(itemId);
        if (item == null || !"FAILED".equals(item.getItemStatus())) {
            return ResponseDTO.userErrorParam("只有失败迁移项可以处置");
        }
        BpmMigrationBatchEntity batch = batchDao.selectById(item.getMigrationBatchId());
        if (batch == null) return ResponseDTO.userErrorParam("迁移批次不存在");
        Long actor = actorProvider.requireCurrentEmployeeId();
        String action = form.getAction().trim().toUpperCase();
        switch (action) {
            case "RETRY" -> {
                BpmInstanceEntity instance = instanceDao.selectByIdForUpdate(item.getInstanceId());
                if (instance == null || !batch.getSourceVersionId().equals(instance.getGraphDefinitionVersionId())) {
                    return ResponseDTO.userErrorParam("实例已不在源版本，不能重试迁移");
                }
                item.setItemStatus("ELIGIBLE");
                item.setFailureReason(null);
                item.setCompensationResult(prefixedResult("RETRY_APPROVED: ", form.getReason()));
                batch.setBatchStatus("PREVIEWED");
                batch.setCompletedAt(null);
            }
            case "KEEP_SOURCE" -> {
                item.setItemStatus("RESOLVED_SOURCE");
                item.setCompensationResult(prefixedResult("KEEP_SOURCE: ", form.getReason()));
            }
            case "COMPENSATED" -> {
                if (form.getCompensationResult() == null || form.getCompensationResult().trim().length() < 6) {
                    return ResponseDTO.userErrorParam("补偿处置必须记录真实补偿结果");
                }
                if (form.getCompensationResult().trim().length() > 512) {
                    return ResponseDTO.userErrorParam("补偿结果不能超过 512 个字符");
                }
                item.setItemStatus("RESOLVED_COMPENSATED");
                item.setCompensationResult(form.getCompensationResult().trim());
            }
            default -> { return ResponseDTO.userErrorParam("不支持的失败处置动作"); }
        }
        item.setDispositionByEmployeeId(actor);
        item.setDisposedAt(LocalDateTime.now());
        itemDao.updateById(item);
        batchDao.updateById(batch);
        return detail(batch, itemDao.selectByBatchId(batch.getMigrationBatchId()));
    }

    private String prefixedResult(String prefix, String reason) {
        String value = prefix + reason.trim();
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private ResponseDTO<BpmMigrationBatchDetailVO> detail(BpmMigrationBatchEntity batch) {
        return detail(batch, itemDao.selectByBatchId(batch.getMigrationBatchId()));
    }

    private ResponseDTO<BpmMigrationBatchDetailVO> detail(BpmMigrationBatchEntity batch, List<BpmMigrationItemEntity> items) {
        BpmMigrationBatchDetailVO vo = new BpmMigrationBatchDetailVO();
        vo.setMigrationBatchId(batch.getMigrationBatchId()); vo.setBatchCode(batch.getBatchCode());
        vo.setIdempotencyKey(batch.getIdempotencyKey()); vo.setSourceVersionId(batch.getSourceVersionId());
        vo.setTargetVersionId(batch.getTargetVersionId()); vo.setBatchStatus(batch.getBatchStatus());
        vo.setMappingJson(batch.getMappingJson()); vo.setDataMappingJson(batch.getDataMappingJson());
        vo.setDiffSnapshotJson(batch.getDiffSnapshotJson()); vo.setReason(batch.getReason());
        vo.setActorEmployeeId(batch.getActorEmployeeId()); vo.setPreviewedAt(batch.getPreviewedAt());
        vo.setConfirmedByEmployeeId(batch.getConfirmedByEmployeeId());
        vo.setConfirmedAt(batch.getConfirmedAt()); vo.setCompletedAt(batch.getCompletedAt());
        vo.setItems(items.stream().map(this::toItem).toList());
        vo.setTotalCount(items.size());
        vo.setEligibleCount((int) items.stream().filter(i -> "ELIGIBLE".equals(i.getItemStatus())).count());
        vo.setBlockedCount((int) items.stream().filter(i -> "BLOCKED".equals(i.getItemStatus())).count());
        vo.setSucceededCount((int) items.stream().filter(i -> "SUCCEEDED".equals(i.getItemStatus())).count());
        vo.setFailedCount((int) items.stream().filter(i -> "FAILED".equals(i.getItemStatus())).count());
        return ResponseDTO.ok(vo);
    }

    private BpmMigrationItemVO toItem(BpmMigrationItemEntity item) {
        BpmMigrationItemVO vo = new BpmMigrationItemVO();
        vo.setMigrationItemId(item.getMigrationItemId()); vo.setInstanceId(item.getInstanceId());
        vo.setItemStatus(item.getItemStatus()); vo.setBlockersJson(item.getBlockersJson());
        vo.setSourceSnapshotJson(item.getSourceSnapshotJson()); vo.setTargetSnapshotJson(item.getTargetSnapshotJson());
        vo.setEngineCommandEvidenceJson(item.getEngineCommandEvidenceJson()); vo.setFailureReason(item.getFailureReason());
        vo.setCompensationResult(item.getCompensationResult()); vo.setMigratedAt(item.getMigratedAt());
        vo.setExecutedByEmployeeId(item.getExecutedByEmployeeId());
        vo.setDispositionByEmployeeId(item.getDispositionByEmployeeId()); vo.setDisposedAt(item.getDisposedAt());
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMappings(String json) {
        return json == null || json.isBlank() ? Collections.emptyMap() : JSON.parseObject(json, Map.class);
    }

    private String validate(BpmMigrationPreviewForm form) {
        if (form == null || form.getSourceVersionId() == null || form.getTargetVersionId() == null
                || form.getSourceVersionId().equals(form.getTargetVersionId())) return "源和目标版本必须存在且不同";
        if (form.getInstanceIds() == null || form.getInstanceIds().isEmpty()) return "至少选择一个迁移实例";
        if (form.getInstanceIds().stream().anyMatch(java.util.Objects::isNull)
                || form.getInstanceIds().stream().distinct().count() != form.getInstanceIds().size()) {
            return "迁移实例不能包含空值或重复项";
        }
        if (form.getNodeMappings() == null) return "节点映射不能为空";
        if (form.getNodeMappings().entrySet().stream().anyMatch(entry -> entry.getKey() == null
                || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank())) {
            return "节点映射的源和目标稳定 ID 均不能为空";
        }
        if (form.getDataMappingJson() == null || form.getDataMappingJson().isBlank()) return "数据映射不能为空";
        if (form.getIdempotencyKey() == null || form.getIdempotencyKey().isBlank()) return "幂等键不能为空";
        if (form.getIdempotencyKey().length() > 128) return "幂等键不能超过 128 个字符";
        if (form.getReason() == null || form.getReason().trim().length() < 6) return "迁移原因至少包含 6 个字符";
        if (form.getReason().trim().length() > 512) return "迁移原因不能超过 512 个字符";
        try { JSON.parseObject(form.getDataMappingJson()); } catch (RuntimeException ex) { return "数据映射必须是合法 JSON"; }
        return null;
    }

    private ResponseDTO<BpmMigrationBatchDetailVO> duplicateResult(BpmMigrationBatchEntity batch, String requestHash) {
        try {
            String stored = JSON.parseObject(batch.getDiffSnapshotJson()).getString("requestHash");
            if (!requestHash.equals(stored)) return ResponseDTO.userErrorParam("幂等键已被不同迁移请求使用");
        } catch (RuntimeException ex) {
            return ResponseDTO.userErrorParam("已有迁移批次缺少可验证的请求摘要，不能复用幂等键");
        }
        return detail(batch);
    }

    private String requestHash(BpmMigrationPreviewForm form) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("sourceVersionId", form.getSourceVersionId());
        canonical.put("targetVersionId", form.getTargetVersionId());
        canonical.put("instanceIds", form.getInstanceIds().stream().distinct().sorted().toList());
        canonical.put("nodeMappings", new TreeMap<>(form.getNodeMappings()));
        canonical.put("dataMapping", JSON.parseObject(form.getDataMappingJson()));
        canonical.put("reason", form.getReason().trim());
        return DigestUtils.sha256Hex(JSON.toJSONString(canonical));
    }

    private String sourceSnapshot(BpmInstanceEntity instance,
                                  BpmMigrationRuntimeGateway.MigrationRuntimeEvidence variableEvidence) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("instanceId", instance.getInstanceId());
        snapshot.put("graphDefinitionVersionId", instance.getGraphDefinitionVersionId());
        snapshot.put("engineProcessDefinitionId", instance.getEngineProcessDefinitionId());
        snapshot.put("engineProcessInstanceId", instance.getEngineProcessInstanceId());
        snapshot.put("runState", instance.getRunState());
        snapshot.put("activeTaskCount", instance.getActiveTaskCount());
        snapshot.put("processWorkingDataId", instance.getProcessWorkingDataId());
        snapshot.put("formDataVersion", instance.getFormDataVersion());
        snapshot.put("currentFormDataDigest", DigestUtils.sha256Hex(
                instance.getCurrentFormDataSnapshotJson() == null ? "" : instance.getCurrentFormDataSnapshotJson()));
        if (variableEvidence != null) {
            snapshot.put("mappedVariableNames", variableEvidence.mappedVariableNames());
            snapshot.put("sourceVariablesDigest", variableEvidence.sourceVariablesDigest());
        }
        return JSON.toJSONString(snapshot);
    }

    private String failureEvidence(Long instanceId, RuntimeException error) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("status", "FAILED");
        evidence.put("errorType", error.getClass().getSimpleName());
        BpmInstanceEntity current = instanceDao.selectById(instanceId);
        if (current != null) {
            evidence.put("platformGraphDefinitionVersionId", current.getGraphDefinitionVersionId());
            evidence.put("platformEngineProcessDefinitionId", current.getEngineProcessDefinitionId());
            try {
                evidence.put("observedProcessDefinitionId", runtimeGateway.currentEngineDefinitionId(current));
            } catch (RuntimeException observationError) {
                evidence.put("observationError", observationError.getClass().getSimpleName());
            }
        }
        return JSON.toJSONString(evidence);
    }
}
