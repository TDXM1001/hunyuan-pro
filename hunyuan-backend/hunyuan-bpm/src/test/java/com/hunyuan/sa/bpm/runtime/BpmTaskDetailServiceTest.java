package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskDetailVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmTaskDetailServiceTest {

    @Test
    void getDetailShouldReturnTaskAndActionLogs() {
        BpmTaskService service = new BpmTaskService();
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmTaskActionLogDao", actionLogDao);

        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(18L);
        task.setInstanceId(8L);
        task.setInstanceNo("SN-2026-0002");
        task.setInstanceTitle("请假申请");
        task.setTaskName("部门审批");
        task.setAssigneeNameSnapshot("鏉庡洓");
        task.setRuntimeAssignmentSnapshotJson("{\"assigneeEmployeeId\":9}");

        BpmTaskActionLogVO log = new BpmTaskActionLogVO();
        log.setTaskId(18L);
        log.setActionType("TRANSFERRED");
        log.setActorNameSnapshot("鐜嬩簲");

        when(taskDao.selectById(18L)).thenReturn(task);
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of(log));

        ResponseDTO<BpmTaskDetailVO> response = service.getDetail(18L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getTaskId()).isEqualTo(18L);
        assertThat(response.getData().getInstanceNo()).isEqualTo("SN-2026-0002");
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getActionLogs().get(0).getActionType()).isEqualTo("TRANSFERRED");
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
