package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.common.enumeration.BpmTaskAction;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskKind;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BpmApprovalStageDao bpmApprovalStageDao;
    private final BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;

    public BpmTaskActionPolicy(BpmDefinitionNodeDao bpmDefinitionNodeDao) {
        this(bpmDefinitionNodeDao, null, null);
    }

    @Autowired
    public BpmTaskActionPolicy(
            BpmDefinitionNodeDao bpmDefinitionNodeDao,
            BpmApprovalStageDao bpmApprovalStageDao,
            BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService
    ) {
        this.bpmDefinitionNodeDao = bpmDefinitionNodeDao;
        this.bpmApprovalStageDao = bpmApprovalStageDao;
        this.bpmGraphRuntimeMetadataService = bpmGraphRuntimeMetadataService;
    }

    public TaskActions describe(BpmTaskEntity task) {
        BpmTaskKind taskKind = resolveTaskKind(task);
        if (!BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            return new TaskActions(taskKind, List.of());
        }
        if (task.getApprovalStageId() != null || task.getApprovalStageMemberId() != null) {
            return new TaskActions(BpmTaskKind.APPROVAL, frozenStageActions(task));
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
        if ("GRAPH".equals(task.getDefinitionSource())
                && task.getGraphDefinitionVersionId() != null
                && bpmGraphRuntimeMetadataService != null) {
            return bpmGraphRuntimeMetadataService
                    .requireNode(task.getGraphDefinitionVersionId(), task.getTaskKey())
                    .nodeType() == com.hunyuan.sa.bpm.engine.graph.GraphNodeType.HANDLE
                    ? BpmTaskKind.HANDLE : BpmTaskKind.APPROVAL;
        }
        if (task.getDefinitionNodeId() == null) {
            return BpmTaskKind.APPROVAL;
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectById(task.getDefinitionNodeId());
        return node != null && "HANDLE_TASK".equals(node.getNodeType())
                ? BpmTaskKind.HANDLE
                : BpmTaskKind.APPROVAL;
    }

    private List<BpmTaskAction> frozenStageActions(BpmTaskEntity task) {
        if (bpmApprovalStageDao == null || task.getApprovalStageId() == null) {
            return List.of(BpmTaskAction.APPROVE, BpmTaskAction.REJECT, BpmTaskAction.RETURN);
        }
        BpmApprovalStageEntity stage = bpmApprovalStageDao.selectById(task.getApprovalStageId());
        if (stage == null || stage.getApprovalPolicySnapshotJson() == null) {
            return List.of();
        }
        JSONArray raw = JSON.parseObject(stage.getApprovalPolicySnapshotJson()).getJSONArray("allowedActions");
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(String::valueOf)
                .map(BpmTaskAction::valueOf)
                .toList();
    }

    public record TaskActions(BpmTaskKind taskKind, List<BpmTaskAction> availableActions) {
    }
}
