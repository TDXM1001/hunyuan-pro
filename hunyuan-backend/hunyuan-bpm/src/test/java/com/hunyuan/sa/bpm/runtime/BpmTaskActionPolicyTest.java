package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.common.enumeration.BpmTaskAction;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskKind;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskActionPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class BpmTaskActionPolicyTest {

    @Test
    void describeShouldExposeHandleActionsWithoutApprovalActions() {
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setNodeType("HANDLE_TASK");
        when(nodeDao.selectById(31L)).thenReturn(node);
        BpmTaskActionPolicy policy = new BpmTaskActionPolicy(nodeDao);
        BpmTaskEntity task = pendingTask();

        BpmTaskActionPolicy.TaskActions actions = policy.describe(task);

        assertThat(actions.taskKind()).isEqualTo(BpmTaskKind.HANDLE);
        assertThat(actions.availableActions()).containsExactly(
                BpmTaskAction.COMPLETE,
                BpmTaskAction.RETURN,
                BpmTaskAction.TRANSFER,
                BpmTaskAction.DELEGATE
        );
        assertThatThrownBy(() -> policy.requireAllowed(task, BpmTaskAction.APPROVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("办理任务");
    }

    @Test
    void describeShouldReturnNoActionsForCompletedTask() {
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmTaskActionPolicy policy = new BpmTaskActionPolicy(nodeDao);
        BpmTaskEntity task = pendingTask();
        task.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());

        BpmTaskActionPolicy.TaskActions actions = policy.describe(task);

        assertThat(actions.taskKind()).isEqualTo(BpmTaskKind.APPROVAL);
        assertThat(actions.availableActions()).isEmpty();
    }

    @Test
    void describeShouldHideGenericAdvancedActionsForFrozenApprovalStageMember() {
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        BpmTaskActionPolicy policy = new BpmTaskActionPolicy(nodeDao, stageDao, null);
        BpmTaskEntity task = pendingTask();
        task.setApprovalStageId(71L);
        task.setApprovalStageMemberId(701L);
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalPolicySnapshotJson("{\"allowedActions\":[\"APPROVE\",\"RETURN\"]}");
        when(stageDao.selectById(71L)).thenReturn(stage);

        BpmTaskActionPolicy.TaskActions actions = policy.describe(task);

        assertThat(actions.taskKind()).isEqualTo(BpmTaskKind.APPROVAL);
        assertThat(actions.availableActions()).containsExactly(
                BpmTaskAction.APPROVE,
                BpmTaskAction.RETURN
        );
    }

    private BpmTaskEntity pendingTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setDefinitionNodeId(31L);
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        return task;
    }
}
