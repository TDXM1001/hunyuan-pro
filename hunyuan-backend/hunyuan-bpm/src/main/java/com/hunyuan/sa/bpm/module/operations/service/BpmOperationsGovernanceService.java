package com.hunyuan.sa.bpm.module.operations.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsActionLogDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsCaseDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsRetentionPolicyDao;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsActionLogEntity;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsActionForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsCaseQueryForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsMetricQueryForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsRetentionEvaluateForm;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsActionResultVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsActionLogVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsCaseVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsCaseDetailVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsMetricVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsRetentionDecisionVO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.operations.BpmOperationsAccessScope;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BPM 运营治理统一查询、处置、指标与归档保留服务。
 */
@Service
public class BpmOperationsGovernanceService {

    private static final Set<String> OPEN_STATUSES = Set.of("OPEN", "PROCESSING", "BLOCKED");
    private static final Set<String> ACTIONS = Set.of("RETRY", "COMPENSATE", "TERMINATE", "ARCHIVE");

    @Resource
    private BpmOperationsCaseDao bpmOperationsCaseDao;

    @Resource
    private BpmOperationsActionLogDao bpmOperationsActionLogDao;

    @Resource
    private BpmOperationsRetentionPolicyDao bpmOperationsRetentionPolicyDao;

    @Resource
    private BpmOperationsActionExecutor bpmOperationsActionExecutor;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOperationsAccessScope bpmOperationsAccessScope;

    public BpmOperationsGovernanceService() {
    }

    public BpmOperationsGovernanceService(
            BpmOperationsCaseDao bpmOperationsCaseDao,
            BpmOperationsActionLogDao bpmOperationsActionLogDao,
            BpmOperationsRetentionPolicyDao bpmOperationsRetentionPolicyDao
    ) {
        this.bpmOperationsCaseDao = bpmOperationsCaseDao;
        this.bpmOperationsActionLogDao = bpmOperationsActionLogDao;
        this.bpmOperationsRetentionPolicyDao = bpmOperationsRetentionPolicyDao;
    }

    public BpmOperationsGovernanceService(
            BpmOperationsCaseDao bpmOperationsCaseDao,
            BpmOperationsActionLogDao bpmOperationsActionLogDao,
            BpmOperationsRetentionPolicyDao bpmOperationsRetentionPolicyDao,
            BpmOperationsActionExecutor bpmOperationsActionExecutor,
            BpmCurrentActorProvider bpmCurrentActorProvider
    ) {
        this(bpmOperationsCaseDao, bpmOperationsActionLogDao, bpmOperationsRetentionPolicyDao);
        this.bpmOperationsActionExecutor = bpmOperationsActionExecutor;
        this.bpmCurrentActorProvider = bpmCurrentActorProvider;
    }

    public BpmOperationsGovernanceService(
            BpmOperationsCaseDao bpmOperationsCaseDao,
            BpmOperationsActionLogDao bpmOperationsActionLogDao,
            BpmOperationsRetentionPolicyDao bpmOperationsRetentionPolicyDao,
            BpmOperationsActionExecutor bpmOperationsActionExecutor,
            BpmCurrentActorProvider bpmCurrentActorProvider,
            BpmOperationsAccessScope bpmOperationsAccessScope
    ) {
        this(bpmOperationsCaseDao, bpmOperationsActionLogDao, bpmOperationsRetentionPolicyDao,
                bpmOperationsActionExecutor, bpmCurrentActorProvider);
        this.bpmOperationsAccessScope = bpmOperationsAccessScope;
    }

    public ResponseDTO<PageResult<BpmOperationsCaseVO>> queryCasePage(BpmOperationsCaseQueryForm queryForm) {
        applyOrganizationScope(queryForm);
        Page<BpmOperationsCaseEntity> page = buildPage(queryForm);
        Page<BpmOperationsCaseEntity> result = bpmOperationsCaseDao.selectPage(page, buildCaseQuery(queryForm));
        List<BpmOperationsCaseVO> records = result.getRecords().stream().map(this::toCaseVO).toList();
        return ResponseDTO.ok(toPageResult(result, records));
    }

    public ResponseDTO<BpmOperationsCaseDetailVO> detail(Long operationsCaseId) {
        BpmOperationsCaseEntity entity = bpmOperationsCaseDao.selectById(operationsCaseId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("运营治理工单不存在");
        }
        checkOrganizationAccess(entity);
        BpmOperationsCaseDetailVO detail = toCaseDetailVO(entity);
        detail.setActionLogs(bpmOperationsActionLogDao.selectList(
                Wrappers.<BpmOperationsActionLogEntity>lambdaQuery()
                        .eq(BpmOperationsActionLogEntity::getOperationsCaseId, operationsCaseId)
                        .orderByDesc(BpmOperationsActionLogEntity::getActionAt)
        ).stream().map(this::toActionLogVO).toList());
        return ResponseDTO.ok(detail);
    }

    public ResponseDTO<List<BpmOperationsCaseVO>> exportCases(BpmOperationsCaseQueryForm queryForm) {
        queryForm.setPageNum(1L);
        queryForm.setPageSize(500L);
        return ResponseDTO.ok(queryCasePage(queryForm).getData().getList());
    }

    public ResponseDTO<BpmOperationsActionResultVO> executeAction(Long operationsCaseId, BpmOperationsActionForm form) {
        String actionType = normalizeActionType(form.getActionType());
        String reason = trim(form.getReason());
        String idempotencyKey = trim(form.getIdempotencyKey());
        if (!ACTIONS.contains(actionType)) {
            return ResponseDTO.userErrorParam("不支持的运营治理动作");
        }
        if (StringUtils.isBlank(reason) || StringUtils.isBlank(idempotencyKey)) {
            return ResponseDTO.userErrorParam("运营治理动作必须包含原因和幂等键");
        }

        BpmOperationsCaseEntity current = bpmOperationsCaseDao.selectById(operationsCaseId);
        if (current == null) {
            return ResponseDTO.userErrorParam("运营治理工单不存在");
        }
        checkOrganizationAccess(current);
        List<BpmOperationsActionLogEntity> duplicated = bpmOperationsActionLogDao.selectList(
                Wrappers.<BpmOperationsActionLogEntity>lambdaQuery()
                        .eq(BpmOperationsActionLogEntity::getIdempotencyKey, idempotencyKey)
        );
        if (!duplicated.isEmpty()) {
            BpmOperationsActionLogEntity existing = duplicated.get(0);
            return ResponseDTO.ok(new BpmOperationsActionResultVO(
                    existing.getOperationsActionLogId(),
                    existing.getActionStatus(),
                    "幂等命令已处理"
            ));
        }
        if (Boolean.TRUE.equals(current.getHighRiskFlag()) && StringUtils.length(reason) < 6) {
            return ResponseDTO.userErrorParam("高风险治理动作必须说明处置原因");
        }
        if ("ARCHIVE".equals(actionType)) {
            BpmOperationsRetentionDecisionVO decision = evaluateRetentionDecision(current, LocalDateTime.now());
            if (!decision.isAllowed()) {
                return ResponseDTO.userErrorParam(decision.getReason());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String beforeSnapshot = snapshot(current, current.getCaseStatus());
        String nextStatus = nextStatus(actionType, current.getCaseStatus());

        BpmOperationsActionLogEntity actionLog = buildActionLog(
                current, actionType, idempotencyKey, reason, beforeSnapshot, null,
                "PROCESSING", null, now
        );
        if (bpmOperationsActionLogDao.insertIdempotencyClaim(actionLog) != 1) {
            BpmOperationsActionLogEntity existing = bpmOperationsActionLogDao.selectOne(
                    Wrappers.<BpmOperationsActionLogEntity>lambdaQuery()
                            .eq(BpmOperationsActionLogEntity::getIdempotencyKey, idempotencyKey)
            );
            return ResponseDTO.ok(new BpmOperationsActionResultVO(
                    existing == null ? null : existing.getOperationsActionLogId(),
                    existing == null ? "PROCESSING" : existing.getActionStatus(),
                    "幂等命令已接收"
            ));
        }

        ResponseDTO<String> execution = bpmOperationsActionExecutor.execute(current, actionType, reason);
        if (!Boolean.TRUE.equals(execution.getOk())) {
            finishActionLog(actionLog, "FAILED", null, execution.getMsg());
            return ResponseDTO.error(execution);
        }

        BpmOperationsCaseEntity update = new BpmOperationsCaseEntity();
        update.setOperationsCaseId(operationsCaseId);
        update.setCaseStatus(nextStatus);
        update.setLastActionAt(now);
        if (!OPEN_STATUSES.contains(nextStatus)) {
            update.setResolvedAt(now);
        }
        update.setAfterSnapshotJson(snapshot(current, nextStatus));
        bpmOperationsCaseDao.updateById(update);

        finishActionLog(actionLog, "SUCCESS", snapshot(current, nextStatus), null);

        return ResponseDTO.ok(new BpmOperationsActionResultVO(
                actionLog.getOperationsActionLogId(),
                "SUCCESS",
                "治理动作已记录"
        ));
    }

    public ResponseDTO<List<BpmOperationsMetricVO>> queryMetrics(BpmOperationsMetricQueryForm form) {
        if (form == null) {
            form = new BpmOperationsMetricQueryForm();
        }
        if (bpmOperationsAccessScope != null) {
            form.setOrganizationId(bpmOperationsAccessScope.requireOrganizationScope(form.getOrganizationId()));
        }
        LambdaQueryWrapper<BpmOperationsCaseEntity> wrapper = Wrappers.<BpmOperationsCaseEntity>lambdaQuery();
        if (form != null) {
            wrapper.eq(form.getGraphDefinitionVersionId() != null,
                    BpmOperationsCaseEntity::getGraphDefinitionVersionId, form.getGraphDefinitionVersionId());
            wrapper.eq(StringUtils.isNotBlank(form.getDefinitionNodeId()),
                    BpmOperationsCaseEntity::getDefinitionNodeId, form.getDefinitionNodeId());
            wrapper.eq(form.getOrganizationId() != null,
                    BpmOperationsCaseEntity::getOrganizationId, form.getOrganizationId());
        }
        return ResponseDTO.ok(aggregateMetrics(bpmOperationsCaseDao.selectList(wrapper)));
    }

    public ResponseDTO<BpmOperationsRetentionDecisionVO> evaluateRetention(BpmOperationsRetentionEvaluateForm form) {
        BpmOperationsCaseEntity entity = bpmOperationsCaseDao.selectById(form.getOperationsCaseId());
        if (entity == null) {
            return ResponseDTO.ok(new BpmOperationsRetentionDecisionVO(false, "运营治理工单不存在"));
        }
        checkOrganizationAccess(entity);
        LocalDateTime requestedAt = form.getArchiveRequestedAt() == null ? LocalDateTime.now() : form.getArchiveRequestedAt();
        return ResponseDTO.ok(evaluateRetentionDecision(entity, requestedAt));
    }

    private BpmOperationsRetentionDecisionVO evaluateRetentionDecision(
            BpmOperationsCaseEntity entity,
            LocalDateTime requestedAt
    ) {
        if (OPEN_STATUSES.contains(entity.getCaseStatus())) {
            return new BpmOperationsRetentionDecisionVO(false, "在途工单不能归档或清理");
        }
        if (Boolean.TRUE.equals(entity.getLegalHoldFlag())) {
            return new BpmOperationsRetentionDecisionVO(false, "法定保留记录不能归档或清理");
        }
        if (entity.getBusinessEvidenceRefCount() != null && entity.getBusinessEvidenceRefCount() > 0) {
            return new BpmOperationsRetentionDecisionVO(false, "仍被业务证据引用，不能归档或清理");
        }
        if (entity.getMigrationSourceRefCount() != null && entity.getMigrationSourceRefCount() > 0) {
            return new BpmOperationsRetentionDecisionVO(false, "仍被迁移来源引用，不能归档或清理");
        }
        if (entity.getRetentionUntil() != null && requestedAt.isBefore(entity.getRetentionUntil())) {
            return new BpmOperationsRetentionDecisionVO(false, "尚未到达保留截止时间");
        }
        return new BpmOperationsRetentionDecisionVO(true, "允许归档");
    }

    private LambdaQueryWrapper<BpmOperationsCaseEntity> buildCaseQuery(BpmOperationsCaseQueryForm form) {
        return Wrappers.<BpmOperationsCaseEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(form.getBusinessKey()), BpmOperationsCaseEntity::getBusinessKey, form.getBusinessKey())
                .eq(form.getGraphDefinitionVersionId() != null, BpmOperationsCaseEntity::getGraphDefinitionVersionId, form.getGraphDefinitionVersionId())
                .eq(form.getAssigneeEmployeeId() != null, BpmOperationsCaseEntity::getAssigneeEmployeeId, form.getAssigneeEmployeeId())
                .eq(StringUtils.isNotBlank(form.getCaseStatus()), BpmOperationsCaseEntity::getCaseStatus, form.getCaseStatus())
                .eq(StringUtils.isNotBlank(form.getSlaLevel()), BpmOperationsCaseEntity::getSlaLevel, form.getSlaLevel())
                .eq(StringUtils.isNotBlank(form.getFailureCode()), BpmOperationsCaseEntity::getFailureCode, form.getFailureCode())
                .eq(StringUtils.isNotBlank(form.getEventId()), BpmOperationsCaseEntity::getEventId, form.getEventId())
                .eq(form.getOrganizationId() != null, BpmOperationsCaseEntity::getOrganizationId, form.getOrganizationId())
                .eq(StringUtils.isNotBlank(form.getDefinitionNodeId()), BpmOperationsCaseEntity::getDefinitionNodeId, form.getDefinitionNodeId())
                .orderByDesc(BpmOperationsCaseEntity::getOpenedAt, BpmOperationsCaseEntity::getOperationsCaseId);
    }

    private void applyOrganizationScope(BpmOperationsCaseQueryForm form) {
        if (bpmOperationsAccessScope != null) {
            form.setOrganizationId(bpmOperationsAccessScope.requireOrganizationScope(form.getOrganizationId()));
        }
    }

    private void checkOrganizationAccess(BpmOperationsCaseEntity entity) {
        if (bpmOperationsAccessScope != null) {
            bpmOperationsAccessScope.checkOrganizationAccess(entity.getOrganizationId());
        }
    }

    private List<BpmOperationsMetricVO> aggregateMetrics(List<BpmOperationsCaseEntity> cases) {
        Map<String, BpmOperationsMetricVO> grouped = new LinkedHashMap<>();
        Map<String, List<Long>> durations = new LinkedHashMap<>();
        for (BpmOperationsCaseEntity entity : cases) {
            String metricDate = entity.getOpenedAt() == null ? "UNKNOWN" : entity.getOpenedAt().toLocalDate().toString();
            String key = entity.getGraphDefinitionVersionId() + "|" + entity.getDefinitionNodeId() + "|"
                    + entity.getOrganizationId() + "|" + metricDate + "|" + entity.getFailureCode();
            BpmOperationsMetricVO metric = grouped.computeIfAbsent(key, ignored -> {
                BpmOperationsMetricVO vo = new BpmOperationsMetricVO();
                vo.setGraphDefinitionVersionId(entity.getGraphDefinitionVersionId());
                vo.setNodeId(entity.getDefinitionNodeId());
                vo.setOrganizationId(entity.getOrganizationId());
                vo.setMetricDate(entity.getOpenedAt() == null ? null : entity.getOpenedAt().toLocalDate());
                vo.setFailureCode(entity.getFailureCode());
                return vo;
            });
            metric.setTotalCount(metric.getTotalCount() + 1);
            if (OPEN_STATUSES.contains(entity.getCaseStatus())) {
                metric.setOpenCount(metric.getOpenCount() + 1);
            }
            if ("BREACHED".equals(entity.getSlaLevel())) {
                metric.setSlaBreachedCount(metric.getSlaBreachedCount() + 1);
            }
            if (Boolean.TRUE.equals(entity.getRetryableFlag())) {
                metric.setRetryableCount(metric.getRetryableCount() + 1);
            }
            if (Boolean.TRUE.equals(entity.getCompensableFlag())) {
                metric.setCompensableCount(metric.getCompensableCount() + 1);
            }
            if (entity.getOpenedAt() != null && entity.getResolvedAt() != null) {
                durations.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(Duration.between(entity.getOpenedAt(), entity.getResolvedAt()).toMinutes());
            }
        }
        grouped.forEach((key, metric) -> {
            List<Long> values = durations.getOrDefault(key, List.of());
            if (!values.isEmpty()) {
                metric.setAverageHandlingMinutes((long) values.stream().mapToLong(Long::longValue).average().orElse(0));
            }
        });
        return new ArrayList<>(grouped.values());
    }

    private String nextStatus(String actionType, String currentStatus) {
        return switch (actionType) {
            case "TERMINATE" -> "TERMINATED";
            case "RETRY", "COMPENSATE" -> "RESOLVED";
            case "ARCHIVE" -> "ARCHIVED";
            default -> currentStatus;
        };
    }

    private String snapshot(BpmOperationsCaseEntity entity, String caseStatus) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("operationsCaseId", entity.getOperationsCaseId());
        snapshot.put("sourceType", entity.getSourceType());
        snapshot.put("sourceId", entity.getSourceId());
        snapshot.put("businessKey", entity.getBusinessKey());
        snapshot.put("caseStatus", caseStatus);
        snapshot.put("failureCode", entity.getFailureCode());
        snapshot.put("failureReason", entity.getFailureReason());
        snapshot.put("slaLevel", entity.getSlaLevel());
        return snapshot.toJSONString();
    }

    private BpmOperationsActionLogEntity buildActionLog(
            BpmOperationsCaseEntity operationsCase,
            String actionType,
            String idempotencyKey,
            String reason,
            String beforeSnapshot,
            String afterSnapshot,
            String actionStatus,
            String failureReason,
            LocalDateTime actionAt
    ) {
        BpmOperationsActionLogEntity actionLog = new BpmOperationsActionLogEntity();
        actionLog.setOperationsCaseId(operationsCase.getOperationsCaseId());
        actionLog.setActionType(actionType);
        actionLog.setActionStatus(actionStatus);
        actionLog.setIdempotencyKey(idempotencyKey);
        actionLog.setActorEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        actionLog.setReason(reason);
        actionLog.setBeforeSnapshotJson(beforeSnapshot);
        actionLog.setAfterSnapshotJson(afterSnapshot);
        actionLog.setFailureReason(failureReason);
        actionLog.setActionAt(actionAt);
        return actionLog;
    }

    private void finishActionLog(
            BpmOperationsActionLogEntity actionLog,
            String actionStatus,
            String afterSnapshot,
            String failureReason
    ) {
        BpmOperationsActionLogEntity update = new BpmOperationsActionLogEntity();
        update.setOperationsActionLogId(actionLog.getOperationsActionLogId());
        update.setActionStatus(actionStatus);
        update.setAfterSnapshotJson(afterSnapshot);
        update.setFailureReason(failureReason);
        bpmOperationsActionLogDao.updateById(update);
        actionLog.setActionStatus(actionStatus);
        actionLog.setAfterSnapshotJson(afterSnapshot);
        actionLog.setFailureReason(failureReason);
    }

    private String normalizeActionType(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private <T> Page<T> buildPage(PageParam queryForm) {
        Page<T> page = new Page<>(queryForm.getPageNum(), queryForm.getPageSize());
        if (queryForm.getSearchCount() != null) {
            page.setSearchCount(queryForm.getSearchCount());
        }
        return page;
    }

    private <T> PageResult<T> toPageResult(Page<?> page, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());
        pageResult.setTotal(page.getTotal());
        pageResult.setPages(page.getPages());
        pageResult.setList(records);
        pageResult.setEmptyFlag(records.isEmpty());
        return pageResult;
    }

    private BpmOperationsCaseVO toCaseVO(BpmOperationsCaseEntity entity) {
        BpmOperationsCaseVO vo = new BpmOperationsCaseVO();
        vo.setOperationsCaseId(entity.getOperationsCaseId());
        vo.setCaseCode(entity.getCaseCode());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceId(entity.getSourceId());
        vo.setEventId(entity.getEventId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setGraphDefinitionVersionId(entity.getGraphDefinitionVersionId());
        vo.setDefinitionNodeId(entity.getDefinitionNodeId());
        vo.setNodeName(entity.getNodeName());
        vo.setOrganizationId(entity.getOrganizationId());
        vo.setAssigneeEmployeeId(entity.getAssigneeEmployeeId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setBusinessKey(entity.getBusinessKey());
        vo.setCaseStatus(entity.getCaseStatus());
        vo.setSeverity(entity.getSeverity());
        vo.setSlaLevel(entity.getSlaLevel());
        vo.setFailureCode(entity.getFailureCode());
        vo.setFailureReason(entity.getFailureReason());
        vo.setRetryableFlag(entity.getRetryableFlag());
        vo.setCompensableFlag(entity.getCompensableFlag());
        vo.setHighRiskFlag(entity.getHighRiskFlag());
        vo.setLegalHoldFlag(entity.getLegalHoldFlag());
        vo.setOpenedAt(entity.getOpenedAt());
        vo.setLastActionAt(entity.getLastActionAt());
        vo.setResolvedAt(entity.getResolvedAt());
        vo.setRetentionUntil(entity.getRetentionUntil());
        return vo;
    }

    private BpmOperationsCaseDetailVO toCaseDetailVO(BpmOperationsCaseEntity entity) {
        BpmOperationsCaseDetailVO target = new BpmOperationsCaseDetailVO();
        target.setOperationsCaseId(entity.getOperationsCaseId());
        target.setCaseCode(entity.getCaseCode());
        target.setSourceType(entity.getSourceType());
        target.setSourceId(entity.getSourceId());
        target.setEventId(entity.getEventId());
        target.setInstanceId(entity.getInstanceId());
        target.setGraphDefinitionVersionId(entity.getGraphDefinitionVersionId());
        target.setDefinitionNodeId(entity.getDefinitionNodeId());
        target.setNodeName(entity.getNodeName());
        target.setOrganizationId(entity.getOrganizationId());
        target.setAssigneeEmployeeId(entity.getAssigneeEmployeeId());
        target.setBusinessType(entity.getBusinessType());
        target.setBusinessId(entity.getBusinessId());
        target.setBusinessKey(entity.getBusinessKey());
        target.setCaseStatus(entity.getCaseStatus());
        target.setSeverity(entity.getSeverity());
        target.setSlaLevel(entity.getSlaLevel());
        target.setFailureCode(entity.getFailureCode());
        target.setFailureReason(entity.getFailureReason());
        target.setRetryableFlag(entity.getRetryableFlag());
        target.setCompensableFlag(entity.getCompensableFlag());
        target.setHighRiskFlag(entity.getHighRiskFlag());
        target.setLegalHoldFlag(entity.getLegalHoldFlag());
        target.setOpenedAt(entity.getOpenedAt());
        target.setLastActionAt(entity.getLastActionAt());
        target.setResolvedAt(entity.getResolvedAt());
        target.setRetentionUntil(entity.getRetentionUntil());
        return target;
    }

    private BpmOperationsActionLogVO toActionLogVO(BpmOperationsActionLogEntity entity) {
        BpmOperationsActionLogVO vo = new BpmOperationsActionLogVO();
        vo.setOperationsActionLogId(entity.getOperationsActionLogId());
        vo.setActionType(entity.getActionType());
        vo.setActionStatus(entity.getActionStatus());
        vo.setActorEmployeeId(entity.getActorEmployeeId());
        vo.setReason(entity.getReason());
        vo.setFailureReason(entity.getFailureReason());
        vo.setActionAt(entity.getActionAt());
        return vo;
    }
}
