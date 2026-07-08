package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmRuntimeDetailServiceTest {

    @Test
    void getDetailShouldReturnInstanceAndActionLogs() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskDao", taskDao);
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
        when(taskDao.queryCurrentTasksByInstanceId(8L)).thenReturn(List.of());
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of(log));

        ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstanceNo()).isEqualTo("SN-2026-0001");
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getActionLogs().get(0).getActionType()).isEqualTo("APPROVED");
    }

    @Test
    void getDetailShouldReturnCurrentPendingTasksInStableOrder() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmTaskActionLogDao", actionLogDao);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCurrentFormDataSnapshotJson("{\"days\":1}");

        BpmTaskVO firstTask = new BpmTaskVO();
        firstTask.setTaskId(18L);
        firstTask.setInstanceId(8L);
        firstTask.setInstanceNo("SN-2026-0001");
        firstTask.setInstanceTitle("请假申请");
        firstTask.setTaskName("部门审批");
        firstTask.setAssigneeNameSnapshot("李四");
        firstTask.setAssignedAt(LocalDateTime.of(2026, 7, 8, 9, 0));

        BpmTaskVO secondTask = new BpmTaskVO();
        secondTask.setTaskId(19L);
        secondTask.setInstanceId(8L);
        secondTask.setInstanceNo("SN-2026-0001");
        secondTask.setInstanceTitle("请假申请");
        secondTask.setTaskName("人事审批");
        secondTask.setAssigneeNameSnapshot("王五");
        secondTask.setAssignedAt(LocalDateTime.of(2026, 7, 8, 9, 5));

        when(instanceDao.selectById(8L)).thenReturn(instance);
        when(taskDao.queryCurrentTasksByInstanceId(8L)).thenReturn(List.of(firstTask, secondTask));
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of());

        ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getCurrentTasks()).extracting(BpmTaskVO::getTaskId)
                .containsExactly(18L, 19L);
        assertThat(response.getData().getCurrentTasks().get(0).getTaskName()).isEqualTo("部门审批");
        assertThat(response.getData().getActionLogs()).isEmpty();
    }

    @Test
    void getDetailShouldReturnDataNotExistWhenInstanceMissing() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        setField(service, "bpmInstanceDao", instanceDao);

        when(instanceDao.selectById(404L)).thenReturn(null);

        ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(404L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getCode()).isNotNull();
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
