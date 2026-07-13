package com.hunyuan.sa.bpm.runtime;

import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSubProcessServiceTest {

    @Test
    void prepareShouldFreezeVersionMappingsAndBeIdempotent() {
        Fixture fixture = fixture();
        when(fixture.linkDao.selectOne(any())).thenReturn(null);
        when(fixture.linkDao.insert(any(BpmSubProcessLinkEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BpmSubProcessLinkEntity.class).setSubProcessLinkId(71L);
            return 1;
        });

        BpmSubProcessService.PreparedSubProcess prepared = fixture.service.prepareChild(
                31L, "parent-execution-1", "archive", Map.of("expenseId", "E-1"));

        assertThat(prepared.linkId()).isEqualTo(71L);
        assertThat(prepared.childInstanceId()).isEqualTo(81L);
        ArgumentCaptor<BpmSubProcessLinkEntity> captor = ArgumentCaptor.forClass(BpmSubProcessLinkEntity.class);
        verify(fixture.linkDao).insert(captor.capture());
        assertThat(captor.getValue().getEventKey()).isEqualTo("SUB:31:1:archive");
        assertThat(captor.getValue().getCalledDefinitionVersionId()).isEqualTo(42L);
        assertThat(captor.getValue().getInputSnapshotJson()).contains("E-1");
        assertThat(captor.getValue().getLinkStatus()).isEqualTo("WAITING");
        assertThat(captor.getValue().getChildInstanceId()).isNotNull();
        verify(fixture.instanceDao).insert(Mockito.argThat((BpmInstanceEntity child) ->
                "GRAPH".equals(child.getDefinitionSource())
                        && child.getGraphDefinitionVersionId().equals(42L)
                        && child.getCurrentFormDataSnapshotJson().contains("E-1")
                        && child.getInstanceId().equals(captor.getValue().getChildInstanceId())
        ));
    }

    @Test
    void completeShouldClaimWaitingLinkOnlyOnceAndFreezeMappedOutput() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity link = link("WAITING", "child-engine-1");
        when(fixture.linkDao.selectOne(any())).thenReturn(link);
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        boolean completed = fixture.service.complete(31L, "archive", Map.of("archiveNo", "AR-9"));

        assertThat(completed).isTrue();
        verify(fixture.linkDao).update(Mockito.argThat((BpmSubProcessLinkEntity update) ->
                "COMPLETED".equals(update.getLinkStatus())
                        && update.getOutputSnapshotJson().contains("AR-9")
                        && update.getCompletedAt() != null
        ), any());
        verify(fixture.instanceDao).updateById(Mockito.argThat((BpmInstanceEntity child) ->
                child.getInstanceId().equals(81L)
                        && child.getRunState().equals(3)
                        && child.getFinishedAt() != null
        ));
    }

    @Test
    void bindChildEngineInstanceShouldPersistOnceForRecovery() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity link = link("WAITING", null);
        when(fixture.linkDao.selectOne(any())).thenReturn(link);
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        boolean bound = fixture.service.bindChildEngineInstance(31L, 81L, "child-engine-9");

        assertThat(bound).isTrue();
        verify(fixture.linkDao).update(Mockito.argThat((BpmSubProcessLinkEntity update) ->
                "child-engine-9".equals(update.getChildEngineProcessInstanceId())), any());
        verify(fixture.instanceDao).updateById(Mockito.argThat((BpmInstanceEntity child) ->
                child.getInstanceId().equals(81L)
                        && "child-engine-9".equals(child.getEngineProcessInstanceId())));
    }

    @Test
    void childRejectionShouldRejectParentWhenFrozenPolicyRequiresIt() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity link = link("WAITING", "child-engine-1");
        link.setFailurePolicy("REJECT_PARENT");
        when(fixture.linkDao.selectOne(any())).thenReturn(link);
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        assertThat(fixture.service.propagateChildRejection(81L, "子流程拒绝")).isTrue();

        verify(fixture.linkDao).update(Mockito.argThat((BpmSubProcessLinkEntity update) ->
                "REJECTED_PARENT".equals(update.getLinkStatus()) && update.getLastError().contains("拒绝")
        ), any());
        verify(fixture.gateway).cancel("parent-engine-1", "子流程拒绝");
        verify(fixture.instanceDao).updateById(Mockito.argThat((BpmInstanceEntity parent) ->
                Long.valueOf(31L).equals(parent.getInstanceId())
                        && Integer.valueOf(3).equals(parent.getRunState())
                        && Integer.valueOf(2).equals(parent.getResultState())
        ));
    }

    @Test
    void recordFailureShouldMoveToManualOrRejectedAccordingToFrozenPolicy() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity link = link("WAITING", "child-engine-1");
        link.setFailurePolicy("MANUAL_INTERVENTION");
        when(fixture.linkDao.selectOne(any())).thenReturn(link);
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        boolean recorded = fixture.service.recordFailure(31L, "archive", "child failed");

        assertThat(recorded).isTrue();
        verify(fixture.linkDao).update(Mockito.argThat((BpmSubProcessLinkEntity update) ->
                "FAILED_MANUAL".equals(update.getLinkStatus())
                        && "child failed".equals(update.getLastError())
        ), any());
    }

    @Test
    void technicalFailureShouldUseChildEngineBindingAndRejectParent() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity link = link("WAITING", "child-engine-1");
        link.setFailurePolicy("REJECT_PARENT");
        when(fixture.linkDao.selectOne(any())).thenReturn(link);
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        assertThat(fixture.service.recordTechnicalFailure("child-engine-1", "delegate failed")).isTrue();

        verify(fixture.gateway).cancel("parent-engine-1", "delegate failed");
        verify(fixture.linkDao).update(Mockito.argThat((BpmSubProcessLinkEntity update) ->
                "REJECTED_PARENT".equals(update.getLinkStatus())
                        && update.getLastError().contains("delegate failed")
        ), any());
    }

    @Test
    void cancelChildrenShouldRespectFrozenPropagationAndCancelOnce() {
        Fixture fixture = fixture();
        BpmSubProcessLinkEntity cancel = link("WAITING", "child-engine-1");
        cancel.setSubProcessLinkId(71L);
        cancel.setCancelPropagation("CANCEL_CHILD");
        BpmSubProcessLinkEntity keep = link("WAITING", "child-engine-2");
        keep.setSubProcessLinkId(72L);
        keep.setCancelPropagation("KEEP_CHILD");
        when(fixture.linkDao.selectList(any())).thenReturn(java.util.List.of(cancel, keep));
        when(fixture.linkDao.update(any(), any())).thenReturn(1);

        int cancelled = fixture.service.cancelChildren(31L, "父流程取消");

        assertThat(cancelled).isEqualTo(1);
        verify(fixture.gateway).cancel("child-engine-1", "父流程取消");
        verify(fixture.gateway, never()).cancel("child-engine-2", "父流程取消");
        verify(fixture.instanceDao).updateById(Mockito.argThat((BpmInstanceEntity child) ->
                Long.valueOf(81L).equals(child.getInstanceId())
                        && Integer.valueOf(4).equals(child.getRunState())
                        && child.getCancelledAt() != null
        ));
    }

    private Fixture fixture() {
        BpmSubProcessService service = new BpmSubProcessService();
        BpmSubProcessLinkDao linkDao = Mockito.mock(BpmSubProcessLinkDao.class);
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmGraphRuntimeMetadataService metadata = Mockito.mock(BpmGraphRuntimeMetadataService.class);
        GraphDefinitionVersionDao graphVersionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        SerialNumberService serialNumberService = Mockito.mock(SerialNumberService.class);
        FlowableProcessInstanceGateway gateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        setField(service, "bpmSubProcessLinkDao", linkDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmGraphRuntimeMetadataService", metadata);
        setField(service, "graphDefinitionVersionDao", graphVersionDao);
        setField(service, "serialNumberService", serialNumberService);
        when(serialNumberService.generate(any())).thenReturn("SUB-20260713-1");
        setField(service, "flowableProcessInstanceGateway", gateway);
        BpmInstanceEntity parent = new BpmInstanceEntity();
        parent.setInstanceId(31L);
        parent.setCurrentGeneration(1);
        parent.setDefinitionSource("GRAPH");
        parent.setGraphDefinitionVersionId(91L);
        parent.setCurrentFormDataSnapshotJson("{\"expenseId\":\"E-1\"}");
        parent.setEngineProcessInstanceId("parent-engine-1");
        when(instanceDao.selectById(31L)).thenReturn(parent);
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BpmInstanceEntity.class).setInstanceId(81L);
            return 1;
        });
        GraphDefinitionVersionEntity childVersion = new GraphDefinitionVersionEntity();
        childVersion.setGraphDefinitionVersionId(42L);
        childVersion.setProcessKey("expense_archive");
        childVersion.setProcessNameSnapshot("归档子流程");
        childVersion.setDefinitionVersion(3);
        childVersion.setLifecycleState("ACTIVE");
        childVersion.setEngineProcessDefinitionId("child-definition-42");
        childVersion.setCategoryIdSnapshot(5L);
        childVersion.setCategoryNameSnapshot("费用");
        when(graphVersionDao.selectById(42L)).thenReturn(childVersion);
        when(metadata.requireNode(91L, "archive")).thenReturn(
                new BpmGraphRuntimeMetadataService.GraphNodeMetadata(
                        "archive", "scope_root", "归档子流程", GraphNodeType.SUB_PROCESS,
                        JSONObject.parseObject("""
                                {
                                  "calledProcessKey":"expense_archive",
                                  "calledDefinitionVersionId":42,
                                  "inputMapping":{"expenseId":"expenseId"},
                                  "outputMapping":{"archiveNo":"archiveNo"},
                                  "failurePolicy":"PAUSE_PARENT",
                                  "cancelPropagation":"CANCEL_CHILD"
                                }
                                """)
                )
        );
        return new Fixture(service, linkDao, instanceDao, gateway);
    }

    private BpmSubProcessLinkEntity link(String status, String childEngineId) {
        BpmSubProcessLinkEntity link = new BpmSubProcessLinkEntity();
        link.setSubProcessLinkId(71L);
        link.setParentInstanceId(31L);
        link.setParentNodeId("archive");
        link.setChildInstanceId(81L);
        link.setLinkStatus(status);
        link.setChildEngineProcessInstanceId(childEngineId);
        link.setFailurePolicy("PAUSE_PARENT");
        link.setCancelPropagation("CANCEL_CHILD");
        return link;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private record Fixture(
            BpmSubProcessService service,
            BpmSubProcessLinkDao linkDao,
            BpmInstanceDao instanceDao,
            FlowableProcessInstanceGateway gateway
    ) {
    }
}
