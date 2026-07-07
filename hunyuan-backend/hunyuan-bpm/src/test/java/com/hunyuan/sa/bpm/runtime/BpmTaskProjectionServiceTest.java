package com.hunyuan.sa.bpm.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableActiveTaskSnapshot;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class BpmTaskProjectionServiceTest {

    private BpmTaskProjectionService service;
    private BpmInstanceDao bpmInstanceDao;
    private BpmTaskDao bpmTaskDao;
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;
    private FlowableTaskGateway flowableTaskGateway;
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @BeforeEach
    void setUp() {
        service = new BpmTaskProjectionService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmDefinitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        flowableTaskGateway = Mockito.mock(FlowableTaskGateway.class);
        bpmOrgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(service, "bpmInstanceDao", bpmInstanceDao);
        setField(service, "bpmTaskDao", bpmTaskDao);
        setField(service, "bpmDefinitionNodeDao", bpmDefinitionNodeDao);
        setField(service, "flowableTaskGateway", flowableTaskGateway);
        setField(service, "bpmOrgIdentityGateway", bpmOrgIdentityGateway);
    }

    @Test
    void syncActiveTasksShouldInsertMissingTaskProjectionAndUpdateActiveCount() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        instance.setEngineProcessInstanceId("process-1");
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeId(100L);
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCategoryIdSnapshot(7L);
        instance.setCategoryNameSnapshot("人事流程");

        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");

        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));

        int activeCount = service.syncActiveTasksForInstance(8L);

        assertThat(activeCount).isEqualTo(1);
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getEngineTaskId()).isEqualTo("task-1");
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("approve_1");
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.PENDING.getValue());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(22L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(1);
        assertThat(instanceCaptor.getValue().getCurrentNodeSummaryJson()).contains("approve_1");
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
