package com.hunyuan.sa.bpm.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyReadStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmInstanceCopyServiceTest {

    private BpmInstanceCopyService service;

    private BpmInstanceCopyDao copyDao;

    private BpmCurrentActorProvider actorProvider;

    private BpmOrgIdentityGateway orgIdentityGateway;

    @BeforeEach
    void setUp() {
        service = new BpmInstanceCopyService();
        copyDao = Mockito.mock(BpmInstanceCopyDao.class);
        actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        orgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(service, "bpmInstanceCopyDao", copyDao);
        setField(service, "bpmCurrentActorProvider", actorProvider);
        setField(service, "bpmOrgIdentityGateway", orgIdentityGateway);
    }

    @Test
    void createManualCopiesShouldDeduplicateTargetsAndSkipCurrentActor() {
        BpmTaskEntity task = buildTask();
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(10L);
        when(orgIdentityGateway.requireEmployee(22L))
                .thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));
        when(orgIdentityGateway.requireEmployee(23L))
                .thenReturn(new BpmEmployeeSnapshot(23L, "王五", 8L, "行政部", null, null));

        ResponseDTO<String> response = service.createManualCopies(
                task,
                List.of(22L, 22L, 10L, 23L),
                "同意，知会财务和行政",
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmInstanceCopyEntity> captor = ArgumentCaptor.forClass(BpmInstanceCopyEntity.class);
        verify(copyDao, Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(BpmInstanceCopyEntity::getTargetEmployeeId)
                .containsExactly(22L, 23L);
        assertThat(captor.getAllValues()).allSatisfy(entity -> {
            assertThat(entity.getInstanceId()).isEqualTo(8L);
            assertThat(entity.getDefinitionId()).isEqualTo(2L);
            assertThat(entity.getDefinitionNodeId()).isEqualTo(5L);
            assertThat(entity.getEngineProcessInstanceId()).isEqualTo("process-8");
            assertThat(entity.getSourceNodeKey()).isEqualTo("manager_approve");
            assertThat(entity.getSourceNodeName()).isEqualTo("经理审批");
            assertThat(entity.getCopyType()).isEqualTo("MANUAL_APPROVE_COPY");
            assertThat(entity.getReadState()).isEqualTo(BpmCopyReadStateEnum.UNREAD.getValue());
            assertThat(entity.getReasonSnapshot()).isEqualTo("同意，知会财务和行政");
            assertThat(entity.getSentAt()).isNotNull();
        });
    }

    @Test
    void createManualCopiesShouldReturnOkWithoutInsertWhenTargetsAreEmpty() {
        ResponseDTO<String> response = service.createManualCopies(
                buildTask(),
                List.of(),
                "同意",
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );

        assertThat(response.getOk()).isTrue();
        verify(copyDao, never()).insert(any(BpmInstanceCopyEntity.class));
    }

    @Test
    void createDesignCopiesShouldWriteStableSourceEventKey() {
        when(orgIdentityGateway.requireEmployee(22L))
                .thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("notify_finance");
        node.setNodeNameSnapshot("抄送财务");

        ResponseDTO<String> response = service.createDesignCopies(
                instance,
                node,
                "process-8",
                List.of(22L)
        );

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmInstanceCopyEntity> captor = ArgumentCaptor.forClass(BpmInstanceCopyEntity.class);
        verify(copyDao).insert(captor.capture());
        assertThat(captor.getValue().getSourceEventKey())
                .isEqualTo("COPY:process-8:notify_finance");
        assertThat(captor.getValue().getCopyType()).isEqualTo("DESIGN_NODE_COPY");
    }

    @Test
    void queryMyCopyPageShouldScopeToCurrentEmployee() {
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(22L);
        BpmInstanceCopyQueryForm form = new BpmInstanceCopyQueryForm();
        form.setPageNum(1L);
        form.setPageSize(10L);
        form.setInstanceNo("SN-2026");
        form.setTitle("请假");
        form.setReadState(BpmCopyReadStateEnum.UNREAD.getValue());

        ResponseDTO<?> response = service.queryMyCopyPage(form);

        assertThat(response.getOk()).isTrue();
        assertThat(form.getTargetEmployeeId()).isEqualTo(22L);
        verify(copyDao).queryMyCopyPage(any(), Mockito.same(form));
    }

    @Test
    void markReadShouldOnlyUpdateCurrentEmployeeCopy() {
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(22L);

        ResponseDTO<String> response = service.markRead(100L);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmInstanceCopyEntity> entityCaptor = ArgumentCaptor.forClass(BpmInstanceCopyEntity.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<BpmInstanceCopyEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(copyDao).update(entityCaptor.capture(), wrapperCaptor.capture());
        assertThat(entityCaptor.getValue().getReadState()).isEqualTo(BpmCopyReadStateEnum.READ.getValue());
        assertThat(entityCaptor.getValue().getReadAt()).isNotNull();
    }

    private BpmTaskEntity buildTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(1L);
        task.setInstanceId(8L);
        task.setDefinitionId(2L);
        task.setDefinitionNodeId(5L);
        task.setEngineProcessInstanceId("process-8");
        task.setTaskKey("manager_approve");
        task.setTaskName("经理审批");
        return task;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
