package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.common.enumeration.BpmTaskAction;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskKind;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一描述并校验任务类型允许的动作集合。
 */
@Service
public class BpmTaskActionPolicy {

    private final BpmDefinitionNodeDao bpmDefinitionNodeDao;

    public BpmTaskActionPolicy(BpmDefinitionNodeDao bpmDefinitionNodeDao) {
        this.bpmDefinitionNodeDao = bpmDefinitionNodeDao;
    }

    public TaskActions describe(BpmTaskEntity task) {
        BpmTaskKind taskKind = resolveTaskKind(task);
        if (!BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            return new TaskActions(taskKind, List.of());
        }
        if (BpmTaskKind.HANDLE.equals(taskKind)) {
            return new TaskActions(taskKind, List.of(
                    BpmTaskAction.COMPLETE,
                    BpmTaskAction.RETURN,
                    BpmTaskAction.TRANSFER,
                    BpmTaskAction.DELEGATE
            ));
        }
        List<BpmTaskAction> actions = new ArrayList<>(List.of(
                BpmTaskAction.APPROVE,
                BpmTaskAction.REJECT,
                BpmTaskAction.RETURN,
                BpmTaskAction.TRANSFER,
                BpmTaskAction.DELEGATE,
                BpmTaskAction.ADD_SIGN
        ));
        if (task.getRuntimeAssignmentSnapshotJson() != null
                && task.getRuntimeAssignmentSnapshotJson().contains("\"addSign\":true")) {
            actions.add(BpmTaskAction.REDUCE_SIGN);
        }
        return new TaskActions(taskKind, List.copyOf(actions));
    }

    public void requireAllowed(BpmTaskEntity task, BpmTaskAction action) {
        TaskActions taskActions = describe(task);
        if (taskActions.availableActions().contains(action)) {
            return;
        }
        String prefix = BpmTaskKind.HANDLE.equals(taskActions.taskKind()) ? "办理任务" : "当前任务";
        throw new IllegalArgumentException(prefix + "不支持动作：" + action.name());
    }

    private BpmTaskKind resolveTaskKind(BpmTaskEntity task) {
        if (task.getDefinitionNodeId() == null) {
            return BpmTaskKind.APPROVAL;
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectById(task.getDefinitionNodeId());
        return node != null && "HANDLE_TASK".equals(node.getNodeType())
                ? BpmTaskKind.HANDLE
                : BpmTaskKind.APPROVAL;
    }

    public record TaskActions(BpmTaskKind taskKind, List<BpmTaskAction> availableActions) {
    }
}
