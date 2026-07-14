package com.hunyuan.sa.bpm.evolution;

import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationItemDao;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.model.MigrationSafetyAssessment;
import com.hunyuan.sa.bpm.module.evolution.service.BpmMigrationItemExecutor;
import com.hunyuan.sa.bpm.module.evolution.service.BpmMigrationRuntimeGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class BpmMigrationItemExecutorTest {
    @Test
    void successAuditMustBeWrittenInsideSameItemExecutionBoundary() {
        Fixture fixture = fixture();
        Mockito.when(fixture.instanceDao.updateMigrationProjection(11L, 1L, 2L, "engine-v2")).thenReturn(1);

        var evidence = fixture.executor.execute(101L, 11L, fixture.batch, fixture.target,
                Map.of(), "{}", 9002L);

        assertThat(evidence.engineCommandEvidenceJson()).contains("engine-v2", "MIGRATED");
        assertThat(fixture.item.getItemStatus()).isEqualTo("SUCCEEDED");
        assertThat(fixture.item.getExecutedByEmployeeId()).isEqualTo(9002L);
        Mockito.verify(fixture.itemDao).updateById(fixture.item);
    }

    @Test
    void projectionFailureMustThrowBeforeSuccessAuditIsWritten() {
        Fixture fixture = fixture();
        Mockito.when(fixture.instanceDao.updateMigrationProjection(11L, 1L, 2L, "engine-v2")).thenReturn(0);

        assertThatThrownBy(() -> fixture.executor.execute(101L, 11L, fixture.batch, fixture.target,
                Map.of(), "{}", 9002L)).hasMessageContaining("并发更新失败");

        Mockito.verify(fixture.itemDao, Mockito.never()).updateById(Mockito.any(BpmMigrationItemEntity.class));
    }

    @Test
    void chainedFormMappingsMustUseTheSameImmutableSourceForEngineAndPlatform() {
        Fixture fixture = fixture();
        fixture.instance.setCurrentFormDataSnapshotJson("{\"a\":\"A\",\"b\":\"B\"}");
        Mockito.when(fixture.instanceDao.updateMigrationProjection(11L, 1L, 2L, "engine-v2")).thenReturn(1);
        AtomicReference<String> engineInput = new AtomicReference<>();
        Mockito.when(fixture.gateway.migrate(any(), any(), any(), any())).thenAnswer(invocation -> {
            engineInput.set(fixture.instance.getCurrentFormDataSnapshotJson());
            return BpmMigrationRuntimeGateway.MigrationRuntimeEvidence.empty();
        });

        fixture.executor.execute(101L, 11L, fixture.batch, fixture.target, Map.of(),
                "{\"fieldMappings\":{\"a\":\"b\",\"b\":\"c\"}}", 9002L);

        assertThat(engineInput.get()).isEqualTo("{\"a\":\"A\",\"b\":\"B\"}");
        assertThat(com.alibaba.fastjson.JSON.parseObject(fixture.instance.getCurrentFormDataSnapshotJson()))
                .containsEntry("b", "A").containsEntry("c", "B").doesNotContainKey("a");
    }

    @Test
    void successAuditMustContainSanitizedVariableMigrationFacts() {
        Fixture fixture = fixture();
        Mockito.when(fixture.instanceDao.updateMigrationProjection(11L, 1L, 2L, "engine-v2")).thenReturn(1);
        Mockito.when(fixture.gateway.migrate(any(), any(), any(), any())).thenReturn(
                new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(
                        Map.of("legacySecret", "currentSecret"), "source-digest", "target-digest"));

        var evidence = fixture.executor.execute(101L, 11L, fixture.batch, fixture.target, Map.of(),
                "{\"variableMappings\":{\"legacySecret\":\"currentSecret\"}}", 9002L);

        assertThat(evidence.engineCommandEvidenceJson())
                .contains("mappedVariableNames", "legacySecret", "currentSecret",
                        "sourceVariablesDigest", "targetVariablesDigest")
                .doesNotContain("sensitive-value");
    }

    @Test
    void recoveredMigrationMustReuseSourceVariableEvidenceAndObserveTargetDigest() {
        Fixture fixture = fixture();
        fixture.instance.setGraphDefinitionVersionId(2L);
        fixture.item.setSourceSnapshotJson("{\"mappedVariableNames\":{\"legacySecret\":\"currentSecret\"},"
                + "\"sourceVariablesDigest\":\"source-digest\"}");
        Mockito.when(fixture.gateway.inspectTargetVariables(any(), any())).thenReturn(
                new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(
                        Map.of("legacySecret", "currentSecret"), null, "target-digest"));

        var evidence = fixture.executor.execute(101L, 11L, fixture.batch, fixture.target, Map.of(),
                "{\"variableMappings\":{\"legacySecret\":\"currentSecret\"}}", 9002L);

        assertThat(evidence.engineCommandEvidenceJson())
                .contains("RECOVERED_AFTER_COMMIT", "legacySecret", "currentSecret",
                        "source-digest", "target-digest");
    }

    @Test
    void recoveredMigrationWithoutCompleteVariableEvidenceMustRequireManualReconciliation() {
        Fixture fixture = fixture();
        fixture.instance.setGraphDefinitionVersionId(2L);
        fixture.item.setSourceSnapshotJson("{}");
        Mockito.when(fixture.gateway.inspectTargetVariables(any(), any())).thenReturn(
                new BpmMigrationRuntimeGateway.MigrationRuntimeEvidence(
                        Map.of("legacySecret", "currentSecret"), null, "target-digest"));

        assertThatThrownBy(() -> fixture.executor.execute(101L, 11L, fixture.batch, fixture.target, Map.of(),
                "{\"variableMappings\":{\"legacySecret\":\"currentSecret\"}}", 9002L))
                .hasMessageContaining("人工对账");

        Mockito.verify(fixture.itemDao, Mockito.never()).updateById(fixture.item);
    }

    @Test
    void recoveredMigrationWithMissingTargetVariableMustRequireManualReconciliation() {
        Fixture fixture = fixture();
        fixture.instance.setGraphDefinitionVersionId(2L);
        fixture.item.setSourceSnapshotJson("{\"mappedVariableNames\":{\"legacySecret\":\"currentSecret\"},"
                + "\"sourceVariablesDigest\":\"source-digest\"}");
        Mockito.when(fixture.gateway.inspectTargetVariables(any(), any()))
                .thenThrow(new IllegalStateException("迁移目标变量 currentSecret 缺失，需要人工对账"));

        assertThatThrownBy(() -> fixture.executor.execute(101L, 11L, fixture.batch, fixture.target, Map.of(),
                "{\"variableMappings\":{\"legacySecret\":\"currentSecret\"}}", 9002L))
                .hasMessageContaining("currentSecret", "人工对账");

        Mockito.verify(fixture.itemDao, Mockito.never()).updateById(fixture.item);
    }

    private Fixture fixture() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmMigrationRuntimeGateway gateway = Mockito.mock(BpmMigrationRuntimeGateway.class);
        BpmMigrationItemDao itemDao = Mockito.mock(BpmMigrationItemDao.class);
        BpmMigrationItemEntity item = new BpmMigrationItemEntity();
        item.setMigrationItemId(101L); item.setMigrationBatchId(100L); item.setInstanceId(11L); item.setItemStatus("ELIGIBLE");
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(11L); instance.setGraphDefinitionVersionId(1L);
        instance.setEngineProcessInstanceId("pi-11"); instance.setCurrentFormDataSnapshotJson("{}");
        GraphDefinitionVersionEntity target = new GraphDefinitionVersionEntity();
        target.setGraphDefinitionVersionId(2L); target.setEngineProcessDefinitionId("engine-v2");
        target.setLifecycleState("ACTIVE");
        BpmMigrationBatchEntity batch = new BpmMigrationBatchEntity();
        batch.setMigrationBatchId(100L); batch.setSourceVersionId(1L); batch.setTargetVersionId(2L);
        Mockito.when(itemDao.selectByIdForUpdate(101L)).thenReturn(item);
        Mockito.when(instanceDao.selectByIdForUpdate(11L)).thenReturn(instance);
        Mockito.when(gateway.assess(any(), any(), any(), any())).thenReturn(new MigrationSafetyAssessment(true, List.of()));
        Mockito.when(gateway.currentEngineDefinitionId(instance)).thenReturn("engine-v2");
        return new Fixture(new BpmMigrationItemExecutor(instanceDao, gateway, itemDao), instanceDao,
                gateway, itemDao, item, instance, batch, target);
    }

    private record Fixture(BpmMigrationItemExecutor executor, BpmInstanceDao instanceDao,
                           BpmMigrationRuntimeGateway gateway, BpmMigrationItemDao itemDao,
                           BpmMigrationItemEntity item, BpmInstanceEntity instance, BpmMigrationBatchEntity batch,
                           GraphDefinitionVersionEntity target) {
    }
}
