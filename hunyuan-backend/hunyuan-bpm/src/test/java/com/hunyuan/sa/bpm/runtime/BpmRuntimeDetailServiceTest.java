package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmRuntimeDetailServiceTest {

    @Test
    void getDetailShouldReturnInstanceAndActionLogs() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskActionLogDao", actionLogDao);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCurrentFormDataSnapshotJson("{\"days\":1}");

        BpmTaskActionLogVO log = new BpmTaskActionLogVO();
        log.setActionType("APPROVED");
        log.setActorNameSnapshot("李四");

        when(instanceDao.selectById(8L)).thenReturn(instance);
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of(log));

        ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstanceNo()).isEqualTo("SN-2026-0001");
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getActionLogs().get(0).getActionType()).isEqualTo("APPROVED");
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
