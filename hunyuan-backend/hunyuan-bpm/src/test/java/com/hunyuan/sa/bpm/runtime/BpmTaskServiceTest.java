package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupSummaryVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmTaskServiceTest {

    private BpmTaskService service;
    private BpmTaskDao taskDao;
    private BpmApprovalGroupService approvalGroupService;

    @BeforeEach
    void setUp() {
        service = new BpmTaskService();
        taskDao = Mockito.mock(BpmTaskDao.class);
        approvalGroupService = Mockito.mock(BpmApprovalGroupService.class);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmApprovalGroupService", approvalGroupService);
    }

    @Test
    void queryAdminPageShouldBatchAttachApprovalGroupSummary() {
        BpmTaskVO task = new BpmTaskVO();
        task.setTaskId(11L);
        task.setApprovalGroupId(21L);
        BpmApprovalGroupSummaryVO summary = new BpmApprovalGroupSummaryVO();
        summary.setApprovalGroupId(21L);
        summary.setApprovalGroupName("财务会签");
        summary.setProcessedMemberCount(1);
        summary.setTotalMemberCount(3);
        when(approvalGroupService.mapSummariesById(List.of(21L))).thenReturn(Map.of(21L, summary));

        invokeAttachApprovalGroupSummaries(service, List.of(task));

        assertThat(task.getApprovalGroup()).isSameAs(summary);
    }

    @Test
    void addSignShouldRejectParallelAllMember() {
        BpmTaskEntity task = buildGroupMember();
        when(taskDao.selectById(11L)).thenReturn(task);
        when(approvalGroupService.isParallelAllGroup(21L)).thenReturn(true);
        BpmTaskAddSignForm form = new BpmTaskAddSignForm();
        form.setTaskId(11L);
        form.setTargetEmployeeId(102L);

        ResponseDTO<String> response = service.addSign(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("并行全员会签成员不支持加签或减签");
    }

    @Test
    void reduceSignShouldRejectParallelAllMember() {
        BpmTaskEntity task = buildGroupMember();
        when(taskDao.selectById(11L)).thenReturn(task);
        when(approvalGroupService.isParallelAllGroup(21L)).thenReturn(true);
        BpmTaskReduceSignForm form = new BpmTaskReduceSignForm();
        form.setTaskId(11L);

        ResponseDTO<String> response = service.reduceSign(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("并行全员会签成员不支持加签或减签");
    }

    private BpmTaskEntity buildGroupMember() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setApprovalGroupId(21L);
        task.setTaskState(1);
        return task;
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

    private static void invokeAttachApprovalGroupSummaries(
            BpmTaskService target,
            List<BpmTaskVO> tasks
    ) {
        try {
            var method = BpmTaskService.class.getDeclaredMethod(
                    "attachApprovalGroupSummaries",
                    List.class
            );
            method.setAccessible(true);
            method.invoke(target, tasks);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("调用审批组摘要装配方法失败", ex);
        }
    }
}
