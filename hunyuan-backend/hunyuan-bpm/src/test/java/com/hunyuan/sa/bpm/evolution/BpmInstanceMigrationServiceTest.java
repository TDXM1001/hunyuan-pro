package com.hunyuan.sa.bpm.evolution;

import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationBatchDao;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationItemDao;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationPreviewForm;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.domain.vo.BpmMigrationBatchDetailVO;
import com.hunyuan.sa.bpm.module.evolution.service.BpmInstanceMigrationService;
import com.hunyuan.sa.bpm.module.evolution.service.BpmMigrationRuntimeGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class BpmInstanceMigrationServiceTest {

    @Test
    void previewMustRejectBackwardOrInactiveTargetVersion() {
        Fixture fixture = fixture();
        GraphDefinitionVersionEntity target = version(2L, "engine-v2");
        target.setDefinitionVersion(1);
        Mockito.when(fixture.versionDao.selectById(2L)).thenReturn(target);

        var response = fixture.service.preview(form());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("严格高于");
        Mockito.verify(fixture.batchDao, Mockito.never()).insert(Mockito.any(BpmMigrationBatchEntity.class));
    }

    @Test
    void idempotencyKeyReuseWithUnverifiablePayloadMustBeRejected() {
        Fixture fixture = fixture();
        BpmMigrationBatchEntity duplicate = new BpmMigrationBatchEntity();
        duplicate.setIdempotencyKey("m8-batch-1");
        duplicate.setDiffSnapshotJson("{\"requestHash\":\"different\"}");
        Mockito.when(fixture.batchDao.selectByIdempotencyKey("m8-batch-1")).thenReturn(duplicate);

        var response = fixture.service.preview(form());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("不同迁移请求");
    }

    @Test
    void duplicateKeyRaceMustReloadWinnerWithCurrentLockingRead() {
        Fixture fixture = fixture();
        BpmMigrationBatchEntity winner = new BpmMigrationBatchEntity();
        winner.setMigrationBatchId(200L);
        winner.setIdempotencyKey("m8-batch-1");
        winner.setDiffSnapshotJson("{\"requestHash\":\"" + requestHash(fixture.service, form()) + "\"}");
        Mockito.when(fixture.instanceDao.selectByIdsForMigration(List.of(11L, 12L)))
                .thenReturn(List.of(instance(11L), instance(12L)));
        Mockito.when(fixture.batchDao.insert(Mockito.any(BpmMigrationBatchEntity.class)))
                .thenThrow(new DuplicateKeyException("concurrent winner"));
        Mockito.when(fixture.batchDao.selectByIdempotencyKeyForUpdate("m8-batch-1")).thenReturn(winner);
        Mockito.when(fixture.itemDao.selectByBatchId(200L)).thenReturn(List.of());

        var response = fixture.service.preview(form());

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getMigrationBatchId()).isEqualTo(200L);
        Mockito.verify(fixture.batchDao).selectByIdempotencyKeyForUpdate("m8-batch-1");
    }

    @Test
    void previewMustPersistEligibleAndBlockedInstanceResults() {
        Fixture fixture = fixture();
        Mockito.when(fixture.instanceDao.selectByIdsForMigration(List.of(11L, 12L)))
                .thenReturn(List.of(instance(11L), instance(12L)));
        Mockito.when(fixture.gateway.assess(any(), any(), any(), any())).thenReturn(
                new MigrationSafetyAssessment(true, List.of()),
                new MigrationSafetyAssessment(false, List.of(
                        new MigrationSafetyAssessment.Blocker("ACTIVE_HUMAN_TASK", "实例存在活动人工任务"))));
        Mockito.when(fixture.gateway.inspectSourceVariables(any(), any())).thenReturn(
                new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(
                        Map.of("legacySecret", "currentSecret"), "source-digest", null));

        BpmMigrationBatchDetailVO detail = fixture.service.preview(form()).getData();

        assertThat(detail.getEligibleCount()).isEqualTo(1);
        assertThat(detail.getBlockedCount()).isEqualTo(1);
        ArgumentCaptor<BpmMigrationItemEntity> item = ArgumentCaptor.forClass(BpmMigrationItemEntity.class);
        Mockito.verify(fixture.itemDao, Mockito.times(2)).insert(item.capture());
        assertThat(item.getAllValues()).extracting(BpmMigrationItemEntity::getItemStatus)
                .containsExactly("ELIGIBLE", "BLOCKED");
        assertThat(item.getAllValues().get(1).getBlockersJson()).contains("ACTIVE_HUMAN_TASK");
        assertThat(item.getAllValues()).allSatisfy(value -> assertThat(value.getSourceSnapshotJson())
                .contains("legacySecret", "currentSecret", "source-digest"));
    }

    @Test
    void executeMustRecheckDriftKeepFailedProjectionAndReturnSameBatchForDuplicateCommand() {
        Fixture fixture = fixture();
        BpmMigrationBatchEntity batch = new BpmMigrationBatchEntity();
        batch.setMigrationBatchId(100L);
        batch.setIdempotencyKey("m8-batch-1");
        batch.setSourceVersionId(1L);
        batch.setTargetVersionId(2L);
        batch.setBatchStatus("PREVIEWED");
        batch.setMappingJson("{\"approve\":\"approve_v2\"}");
        BpmMigrationItemEntity first = item(101L, 11L, "ELIGIBLE");
        BpmMigrationItemEntity second = item(102L, 12L, "ELIGIBLE");
        Mockito.when(fixture.batchDao.selectById(100L)).thenReturn(batch);
        Mockito.when(fixture.batchDao.claimForExecution(Mockito.eq(100L), Mockito.anyString(), Mockito.eq(9001L)))
                .thenAnswer(invocation -> {
                    batch.setExecutionOwnerKey(invocation.getArgument(1));
                    batch.setBatchStatus("EXECUTING");
                    return 1;
                });
        Mockito.when(fixture.batchDao.renewExecutionLease(Mockito.eq(100L), Mockito.anyString())).thenReturn(1);
        Mockito.when(fixture.batchDao.finalizeExecution(Mockito.eq(100L), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
                    batch.setBatchStatus(invocation.getArgument(2));
                    batch.setSucceededCount(invocation.getArgument(3));
                    batch.setFailedCount(invocation.getArgument(4));
                    return 1;
                });
        Mockito.when(fixture.itemDao.selectByIdForUpdate(101L)).thenReturn(first);
        Mockito.when(fixture.itemDao.selectByIdForUpdate(102L)).thenReturn(second);
        Mockito.when(fixture.itemDao.selectByBatchId(100L)).thenReturn(List.of(first, second));
        Mockito.when(fixture.instanceDao.selectByIdForUpdate(11L)).thenReturn(instance(11L));
        Mockito.when(fixture.instanceDao.selectByIdForUpdate(12L)).thenReturn(instance(12L));
        Mockito.when(fixture.gateway.assess(any(), any(), any(), any()))
                .thenReturn(new MigrationSafetyAssessment(true, List.of()));
        Mockito.when(fixture.gateway.migrate(any(), any(), any(), any()))
                .thenReturn(BpmMigrationRuntimeGateway.MigrationRuntimeEvidence.empty());
        Mockito.when(fixture.gateway.currentEngineDefinitionId(any())).thenReturn("engine-v2");
        Mockito.when(fixture.gateway.migrate(Mockito.argThat(value -> value.getInstanceId().equals(12L)),
                any(), any(), any())).thenThrow(new IllegalStateException("Flowable validation failed"));
        Mockito.when(fixture.instanceDao.updateMigrationProjection(11L, 1L, 2L, "engine-v2")).thenReturn(1);

        BpmMigrationBatchDetailVO result = fixture.service.execute(100L).getData();

        assertThat(result.getSucceededCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        Mockito.verify(fixture.instanceDao, Mockito.never()).updateMigrationProjection(
                Mockito.eq(12L), any(), any(), any());
        assertThat(second.getFailureReason()).contains("Flowable validation failed");

        batch.setBatchStatus("PARTIAL_FAILED");
        fixture.service.execute(100L);
        Mockito.verify(fixture.gateway, Mockito.times(2)).migrate(any(), any(), any(), any());
    }

    @Test
    void lostExecutionLeaseMustStopBeforeMigratingNextItem() {
        Fixture fixture = fixture();
        BpmMigrationBatchEntity batch = new BpmMigrationBatchEntity();
        batch.setMigrationBatchId(100L); batch.setSourceVersionId(1L); batch.setTargetVersionId(2L);
        batch.setBatchStatus("PREVIEWED"); batch.setMappingJson("{}"); batch.setDataMappingJson("{}");
        BpmMigrationItemEntity item = item(101L, 11L, "ELIGIBLE");
        Mockito.when(fixture.batchDao.selectById(100L)).thenReturn(batch);
        Mockito.when(fixture.itemDao.selectByBatchId(100L)).thenReturn(List.of(item));
        Mockito.when(fixture.batchDao.claimForExecution(Mockito.eq(100L), Mockito.anyString(), Mockito.eq(9001L)))
                .thenAnswer(invocation -> {
                    batch.setExecutionOwnerKey(invocation.getArgument(1));
                    batch.setBatchStatus("EXECUTING");
                    return 1;
                });
        Mockito.when(fixture.batchDao.renewExecutionLease(Mockito.eq(100L), Mockito.anyString())).thenReturn(0);

        var response = fixture.service.execute(100L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("租约已丢失");
        Mockito.verify(fixture.gateway, Mockito.never()).migrate(any(), any(), any(), any());
        Mockito.verify(fixture.batchDao, Mockito.never()).finalizeExecution(
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

    private Fixture fixture() {
        BpmMigrationBatchDao batchDao = Mockito.mock(BpmMigrationBatchDao.class);
        BpmMigrationItemDao itemDao = Mockito.mock(BpmMigrationItemDao.class);
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmMigrationRuntimeGateway gateway = Mockito.mock(BpmMigrationRuntimeGateway.class);
        BpmCurrentActorProvider actor = Mockito.mock(BpmCurrentActorProvider.class);
        Mockito.when(actor.requireCurrentEmployeeId()).thenReturn(9001L);
        Mockito.when(versionDao.selectById(1L)).thenReturn(version(1L, "engine-v1"));
        Mockito.when(versionDao.selectById(2L)).thenReturn(version(2L, "engine-v2"));
        Mockito.when(batchDao.insert(Mockito.any(BpmMigrationBatchEntity.class))).thenAnswer(invocation -> {
            BpmMigrationBatchEntity value = invocation.getArgument(0);
            value.setMigrationBatchId(100L);
            return 1;
        });
        return new Fixture(new BpmInstanceMigrationService(batchDao, itemDao, instanceDao, versionDao, gateway, actor),
                batchDao, itemDao, instanceDao, versionDao, gateway);
    }

    private BpmMigrationPreviewForm form() {
        BpmMigrationPreviewForm form = new BpmMigrationPreviewForm();
        form.setSourceVersionId(1L);
        form.setTargetVersionId(2L);
        form.setInstanceIds(List.of(11L, 12L));
        form.setNodeMappings(Map.of("approve", "approve_v2"));
        form.setDataMappingJson("{}");
        form.setIdempotencyKey("m8-batch-1");
        form.setReason("审批链升级并已完成业务确认");
        return form;
    }

    private String requestHash(BpmInstanceMigrationService service, BpmMigrationPreviewForm form) {
        try {
            var method = BpmInstanceMigrationService.class.getDeclaredMethod("requestHash", BpmMigrationPreviewForm.class);
            method.setAccessible(true);
            return (String) method.invoke(service, form);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private GraphDefinitionVersionEntity version(Long id, String engineId) {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(id);
        version.setProcessKey("expense");
        version.setEngineProcessDefinitionId(engineId);
        version.setDefinitionVersion(id.intValue());
        version.setLifecycleState("ACTIVE");
        version.setDeploymentId("deployment-" + id);
        return version;
    }

    private BpmInstanceEntity instance(Long id) {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(id);
        instance.setGraphDefinitionVersionId(1L);
        instance.setEngineProcessDefinitionId("engine-v1");
        instance.setEngineProcessInstanceId("pi-" + id);
        instance.setRunState(1);
        instance.setActiveTaskCount(0);
        return instance;
    }

    private BpmMigrationItemEntity item(Long id, Long instanceId, String status) {
        BpmMigrationItemEntity item = new BpmMigrationItemEntity();
        item.setMigrationItemId(id);
        item.setMigrationBatchId(100L);
        item.setInstanceId(instanceId);
        item.setItemStatus(status);
        return item;
    }

    private record Fixture(BpmInstanceMigrationService service, BpmMigrationBatchDao batchDao,
                           BpmMigrationItemDao itemDao, BpmInstanceDao instanceDao,
                           GraphDefinitionVersionDao versionDao,
                           BpmMigrationRuntimeGateway gateway) {
    }
}
