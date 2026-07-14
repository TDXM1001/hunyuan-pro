package com.hunyuan.sa.bpm.operations;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsActionLogDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsCaseDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsRetentionPolicyDao;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsActionLogEntity;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsActionForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsCaseQueryForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsRetentionEvaluateForm;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsMetricVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsCaseDetailVO;
import com.hunyuan.sa.bpm.module.operations.service.BpmOperationsGovernanceService;
import com.hunyuan.sa.bpm.module.operations.service.BpmOperationsActionExecutor;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.operations.BpmOperationsAccessScope;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class BpmOperationsGovernanceServiceTest {

    @Test
    void queryExceptionQueueMustSupportBusinessSlaFailureAndEventFilters() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        Mockito.when(caseDao.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<BpmOperationsCaseEntity> page = invocation.getArgument(0);
            BpmOperationsCaseEntity entity = new BpmOperationsCaseEntity();
            entity.setOperationsCaseId(10L);
            entity.setBusinessKey("EXP-20260714-1");
            entity.setEventId("evt-1");
            entity.setFailureCode("CALLBACK_TIMEOUT");
            entity.setSlaLevel("BREACHED");
            entity.setCaseStatus("OPEN");
            page.setRecords(List.of(entity));
            page.setTotal(1);
            return page;
        });

        BpmOperationsCaseQueryForm form = new BpmOperationsCaseQueryForm();
        form.setBusinessKey("EXP-20260714-1");
        form.setGraphDefinitionVersionId(101L);
        form.setAssigneeEmployeeId(2001L);
        form.setCaseStatus("OPEN");
        form.setSlaLevel("BREACHED");
        form.setFailureCode("CALLBACK_TIMEOUT");
        form.setEventId("evt-1");
        form.setPageNum(1L);
        form.setPageSize(10L);

        var result = service(caseDao).queryCasePage(form).getData();

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).extracting("businessKey").containsExactly("EXP-20260714-1");
        Mockito.verify(caseDao).selectPage(any(), any());
    }

    @Test
    void highRiskActionMustRequireReasonAndWriteBeforeAfterSnapshotWithIdempotency() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsActionLogDao actionLogDao = Mockito.mock(BpmOperationsActionLogDao.class);
        BpmOperationsActionExecutor actionExecutor = Mockito.mock(BpmOperationsActionExecutor.class);
        BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOperationsCaseEntity openCase = openCase();
        Mockito.when(caseDao.selectById(10L)).thenReturn(openCase);
        Mockito.when(actionLogDao.selectList(any())).thenReturn(List.of());
        Mockito.when(actionLogDao.insertIdempotencyClaim(any())).thenAnswer(invocation -> {
            BpmOperationsActionLogEntity log = invocation.getArgument(0);
            log.setOperationsActionLogId(101L);
            return 1;
        });
        Mockito.when(actionExecutor.execute(any(), any(), any())).thenReturn(ResponseDTO.ok());
        Mockito.when(actorProvider.requireCurrentEmployeeId()).thenReturn(9001L);

        BpmOperationsActionForm form = new BpmOperationsActionForm();
        form.setActionType("TERMINATE");
        form.setIdempotencyKey("m7-action-1");
        form.setReason("外部系统确认无法恢复，终止流程并保留补偿证据");

        var response = service(caseDao, actionLogDao, actionExecutor, actorProvider).executeAction(10L, form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmOperationsActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmOperationsActionLogEntity.class);
        Mockito.verify(actionLogDao).insertIdempotencyClaim(logCaptor.capture());
        assertThat(logCaptor.getValue().getBeforeSnapshotJson()).contains("\"caseStatus\":\"OPEN\"");
        assertThat(logCaptor.getValue().getAfterSnapshotJson()).contains("\"caseStatus\":\"TERMINATED\"");
        assertThat(logCaptor.getValue().getIdempotencyKey()).isEqualTo("m7-action-1");
        assertThat(logCaptor.getValue().getActorEmployeeId()).isEqualTo(9001L);
        Mockito.verify(actionExecutor).execute(openCase, "TERMINATE", form.getReason());
        Mockito.verify(caseDao).updateById(any(BpmOperationsCaseEntity.class));
    }

    @Test
    void duplicateActionWithSameIdempotencyKeyMustNotWriteSecondAuditFact() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsActionLogDao actionLogDao = Mockito.mock(BpmOperationsActionLogDao.class);
        BpmOperationsActionLogEntity existing = new BpmOperationsActionLogEntity();
        existing.setOperationsActionLogId(7L);
        existing.setActionStatus("SUCCESS");
        Mockito.when(actionLogDao.selectList(any())).thenReturn(List.of(existing));
        Mockito.when(caseDao.selectById(10L)).thenReturn(openCase());

        BpmOperationsActionForm form = new BpmOperationsActionForm();
        form.setActionType("RETRY");
        form.setIdempotencyKey("same-command");
        form.setReason("安全重试");

        var response = service(caseDao, actionLogDao).executeAction(10L, form);

        assertThat(response.getOk()).isTrue();
        Mockito.verify(caseDao, Mockito.never()).updateById(Mockito.<BpmOperationsCaseEntity>any());
        Mockito.verify(actionLogDao, Mockito.never()).insertIdempotencyClaim(Mockito.any());
    }

    @Test
    void detailMustIncludeAppendOnlyActionAuditFacts() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsActionLogDao actionLogDao = Mockito.mock(BpmOperationsActionLogDao.class);
        Mockito.when(caseDao.selectById(10L)).thenReturn(openCase());
        BpmOperationsActionLogEntity audit = new BpmOperationsActionLogEntity();
        audit.setOperationsActionLogId(7L);
        audit.setActionType("RETRY");
        audit.setActionStatus("SUCCESS");
        audit.setReason("人工确认后重试");
        Mockito.when(actionLogDao.selectList(any())).thenReturn(List.of(audit));

        BpmOperationsCaseDetailVO detail = service(caseDao, actionLogDao).detail(10L).getData();

        assertThat(detail.getActionLogs()).hasSize(1);
        assertThat(detail.getActionLogs().get(0).getReason()).isEqualTo("人工确认后重试");
    }

    @Test
    void archiveActionMustNotBypassRetentionProtection() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsActionLogDao actionLogDao = Mockito.mock(BpmOperationsActionLogDao.class);
        BpmOperationsActionExecutor actionExecutor = Mockito.mock(BpmOperationsActionExecutor.class);
        BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOperationsCaseEntity active = openCase();
        Mockito.when(caseDao.selectById(10L)).thenReturn(active);
        Mockito.when(actionLogDao.selectList(any())).thenReturn(List.of());

        BpmOperationsActionForm form = new BpmOperationsActionForm();
        form.setActionType("ARCHIVE");
        form.setIdempotencyKey("archive-10");
        form.setReason("归档已恢复异常");

        var response = service(caseDao, actionLogDao, actionExecutor, actorProvider).executeAction(10L, form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("在途");
        Mockito.verifyNoInteractions(actionExecutor);
    }

    @Test
    void queryMustApplyAuthorizedOrganizationScopeInsteadOfTrustingClient() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        Mockito.when(caseDao.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        BpmOperationsAccessScope accessScope = Mockito.mock(BpmOperationsAccessScope.class);
        Mockito.when(accessScope.requireOrganizationScope(99L)).thenReturn(7L);
        BpmOperationsGovernanceService service = service(
                caseDao,
                Mockito.mock(BpmOperationsActionLogDao.class),
                Mockito.mock(BpmOperationsActionExecutor.class),
                Mockito.mock(BpmCurrentActorProvider.class),
                accessScope
        );
        BpmOperationsCaseQueryForm form = new BpmOperationsCaseQueryForm();
        form.setPageNum(1L);
        form.setPageSize(10L);
        form.setOrganizationId(99L);

        service.queryCasePage(form);

        assertThat(form.getOrganizationId()).isEqualTo(7L);
        Mockito.verify(accessScope).requireOrganizationScope(99L);
    }

    @Test
    void metricsMustAggregateSlaBacklogHandlingDurationAndFailureTrendWithoutFlowableTables() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsCaseEntity breached = openCase();
        breached.setGraphDefinitionVersionId(101L);
        breached.setDefinitionNodeId("finance_sync");
        breached.setOrganizationId(5L);
        breached.setOpenedAt(LocalDateTime.parse("2026-07-14T09:00:00"));
        breached.setResolvedAt(LocalDateTime.parse("2026-07-14T10:30:00"));
        breached.setSlaLevel("BREACHED");
        BpmOperationsCaseEntity open = openCase();
        open.setGraphDefinitionVersionId(101L);
        open.setDefinitionNodeId("finance_sync");
        open.setOrganizationId(5L);
        open.setOpenedAt(LocalDateTime.parse("2026-07-14T10:00:00"));
        Mockito.when(caseDao.selectList(any())).thenReturn(List.of(breached, open));

        List<BpmOperationsMetricVO> metrics = service(caseDao).queryMetrics(null).getData();

        assertThat(metrics).hasSize(1);
        assertThat(metrics.get(0).getGraphDefinitionVersionId()).isEqualTo(101L);
        assertThat(metrics.get(0).getNodeId()).isEqualTo("finance_sync");
        assertThat(metrics.get(0).getTotalCount()).isEqualTo(2);
        assertThat(metrics.get(0).getOpenCount()).isEqualTo(2);
        assertThat(metrics.get(0).getSlaBreachedCount()).isEqualTo(1);
        assertThat(metrics.get(0).getAverageHandlingMinutes()).isEqualTo(90L);
    }

    @Test
    void retentionMustRejectActiveLegalHoldOrReferencedEvidence() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsCaseEntity active = openCase();
        active.setLegalHoldFlag(false);
        active.setRetentionUntil(LocalDateTime.parse("2026-07-01T00:00:00"));
        Mockito.when(caseDao.selectById(10L)).thenReturn(active);

        BpmOperationsRetentionEvaluateForm form = new BpmOperationsRetentionEvaluateForm();
        form.setOperationsCaseId(10L);
        form.setArchiveRequestedAt(LocalDateTime.parse("2026-07-14T00:00:00"));

        var response = service(caseDao).evaluateRetention(form);

        assertThat(response.getData().isAllowed()).isFalse();
        assertThat(response.getData().getReason()).contains("在途");
    }

    @Test
    void retentionMustRejectBusinessEvidenceOrMigrationSourceReferences() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmOperationsCaseEntity resolved = openCase();
        resolved.setCaseStatus("RESOLVED");
        resolved.setBusinessEvidenceRefCount(1);
        resolved.setMigrationSourceRefCount(0);
        Mockito.when(caseDao.selectById(10L)).thenReturn(resolved);

        BpmOperationsRetentionEvaluateForm form = new BpmOperationsRetentionEvaluateForm();
        form.setOperationsCaseId(10L);

        var response = service(caseDao).evaluateRetention(form);

        assertThat(response.getData().isAllowed()).isFalse();
        assertThat(response.getData().getReason()).contains("业务证据");
    }

    private BpmOperationsGovernanceService service(BpmOperationsCaseDao caseDao) {
        return service(caseDao, Mockito.mock(BpmOperationsActionLogDao.class));
    }

    private BpmOperationsGovernanceService service(BpmOperationsCaseDao caseDao, BpmOperationsActionLogDao actionLogDao) {
        return new BpmOperationsGovernanceService(
                caseDao,
                actionLogDao,
                Mockito.mock(BpmOperationsRetentionPolicyDao.class)
        );
    }

    private BpmOperationsGovernanceService service(
            BpmOperationsCaseDao caseDao,
            BpmOperationsActionLogDao actionLogDao,
            BpmOperationsActionExecutor actionExecutor,
            BpmCurrentActorProvider actorProvider
    ) {
        return new BpmOperationsGovernanceService(
                caseDao,
                actionLogDao,
                Mockito.mock(BpmOperationsRetentionPolicyDao.class),
                actionExecutor,
                actorProvider
        );
    }

    private BpmOperationsGovernanceService service(
            BpmOperationsCaseDao caseDao,
            BpmOperationsActionLogDao actionLogDao,
            BpmOperationsActionExecutor actionExecutor,
            BpmCurrentActorProvider actorProvider,
            BpmOperationsAccessScope accessScope
    ) {
        return new BpmOperationsGovernanceService(
                caseDao,
                actionLogDao,
                Mockito.mock(BpmOperationsRetentionPolicyDao.class),
                actionExecutor,
                actorProvider,
                accessScope
        );
    }

    private BpmOperationsCaseEntity openCase() {
        BpmOperationsCaseEntity entity = new BpmOperationsCaseEntity();
        entity.setOperationsCaseId(10L);
        entity.setSourceType("CALLBACK");
        entity.setSourceId(99L);
        entity.setBusinessKey("EXP-20260714-1");
        entity.setCaseStatus("OPEN");
        entity.setFailureCode("CALLBACK_TIMEOUT");
        entity.setFailureReason("回调超时");
        entity.setSlaLevel("WARNING");
        entity.setRetryableFlag(true);
        entity.setHighRiskFlag(true);
        entity.setOpenedAt(LocalDateTime.parse("2026-07-14T09:00:00"));
        return entity;
    }
}
