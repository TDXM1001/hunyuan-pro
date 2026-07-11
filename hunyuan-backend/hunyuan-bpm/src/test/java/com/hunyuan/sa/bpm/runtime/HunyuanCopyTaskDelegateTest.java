package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.internal.HunyuanCopyTaskDelegate;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmCopyRecipientResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunyuanCopyTaskDelegateTest {

    @Test
    void executeShouldResolveFrozenRecipientsAndCreateDesignCopies() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmCopyRecipientResolver resolver = Mockito.mock(BpmCopyRecipientResolver.class);
        BpmInstanceCopyService copyService = Mockito.mock(BpmInstanceCopyService.class);
        DelegateExecution execution = Mockito.mock(DelegateExecution.class);
        Expression copyNodeKey = Mockito.mock(Expression.class);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(81L);
        instance.setDefinitionId(18L);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(301L);
        node.setDefinitionId(18L);
        node.setNodeKey("notify_finance");
        node.setNodeType("COPY_TASK");

        when(execution.getVariable("hunyuanInstanceId")).thenReturn(81L);
        when(execution.getProcessInstanceId()).thenReturn("engine-81");
        when(copyNodeKey.getValue(execution)).thenReturn("notify_finance");
        when(instanceDao.selectById(81L)).thenReturn(instance);
        when(nodeDao.selectOne(any())).thenReturn(node);
        when(resolver.resolve(instance, node)).thenReturn(List.of(11L, 12L));
        when(copyService.createDesignCopies(instance, node, "engine-81", List.of(11L, 12L)))
                .thenReturn(ResponseDTO.ok());
        HunyuanCopyTaskDelegate delegate = new HunyuanCopyTaskDelegate();
        setField(delegate, "bpmInstanceDao", instanceDao);
        setField(delegate, "bpmDefinitionNodeDao", nodeDao);
        setField(delegate, "bpmCopyRecipientResolver", resolver);
        setField(delegate, "bpmInstanceCopyService", copyService);
        setField(delegate, "copyNodeKey", copyNodeKey);

        delegate.execute(execution);

        verify(copyService).createDesignCopies(instance, node, "engine-81", List.of(11L, 12L));
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
