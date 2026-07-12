package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ActorSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberAction;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.candidate.domain.model.MemberUpdate;
import com.hunyuan.sa.bpm.module.candidate.service.ApprovalCompletionService;
import com.hunyuan.sa.bpm.module.candidate.service.ParticipantAuthorizationService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M2 成员待办的唯一命令入口。它只更新冻结成员事实，并通过阶段控制器一次性推进引擎。
 */
@Service
public class BpmApprovalStageCommandService {

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmApprovalStageDao bpmApprovalStageDao;

    @Resource
    private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private ParticipantAuthorizationService participantAuthorizationService;

    @Resource
    private ApprovalCompletionService approvalCompletionService;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

    @Resource
    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> execute(Long taskId, String action, String commentText) {
        BpmTaskEntity lookup = bpmTaskDao.selectById(taskId);
        if (lookup == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (lookup.getApprovalStageId() == null || lookup.getApprovalStageMemberId() == null) {
            return ResponseDTO.userErrorParam("当前待办不是 M2 审批阶段成员任务");
        }

        BpmInstanceEntity instance = bpmInstanceDao.selectByIdForUpdate(lookup.getInstanceId());
        if (instance == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        BpmApprovalStageEntity stage = bpmApprovalStageDao.selectByIdForUpdate(lookup.getApprovalStageId());
        if (stage == null || !lookup.getInstanceId().equals(stage.getInstanceId())) {
            return ResponseDTO.userErrorParam("审批阶段与任务实例不一致");
        }
        BpmTaskEntity task = bpmTaskDao.selectByIdForUpdate(taskId);
        if (task == null || !BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            return ResponseDTO.userErrorParam("审批任务已处理");
        }
        List<BpmApprovalStageMemberEntity> members = bpmApprovalStageMemberDao
                .selectByApprovalStageIdForUpdate(stage.getApprovalStageId());
        BpmApprovalStageMemberEntity actedMember = members.stream()
                .filter(member -> task.getApprovalStageMemberId().equals(member.getApprovalStageMemberId()))
                .findFirst()
                .orElse(null);
        if (actedMember == null) {
            return ResponseDTO.userErrorParam("审批成员不存在或不属于当前阶段");
        }

        Long actorEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actor = bpmOrgIdentityGateway.requireEmployee(actorEmployeeId);
        ApprovalPolicyDocument policy = parsePolicy(stage);
        if (!participantAuthorizationService.authorize(
                new ActorSnapshot(stage.getTenantId(), actor.employeeId(), true),
                toStageFact(stage),
                toMemberFact(actedMember),
                policy,
                action
        )) {
            return ResponseDTO.error(UserErrorCode.NO_PERMISSION);
        }

        ApprovalCompletionDecision decision = approvalCompletionService.decide(
                policy,
                toStageFact(stage),
                members.stream().map(this::toMemberFact).toList(),
                new ApprovalMemberAction(String.valueOf(actedMember.getApprovalStageMemberId()), action)
        );
        LocalDateTime now = LocalDateTime.now();
        Map<String, BpmApprovalStageMemberEntity> membersById = new HashMap<>();
        for (BpmApprovalStageMemberEntity member : members) {
            membersById.put(String.valueOf(member.getApprovalStageMemberId()), member);
        }
        for (MemberUpdate update : decision.memberUpdates()) {
            BpmApprovalStageMemberEntity member = membersById.get(update.memberId());
            if (member == null) {
                throw new IllegalStateException("审批完成决策返回了未知成员：" + update.memberId());
            }
            updateMember(member, update.state(), action, now);
            if (member.getApprovalStageMemberId().equals(task.getApprovalStageMemberId())) {
                updateActedTask(task, update.state(), now);
            } else if (update.state() == ApprovalMemberState.TERMINATED
                    || update.state() == ApprovalMemberState.CANCELLED
                    || update.state() == ApprovalMemberState.INELIGIBLE) {
                cancelMemberTask(member.getApprovalStageMemberId(), now);
            }
        }

        stage.setStageState(decision.stageState().name());
        if (decision.stageState() != ApprovalStageState.ACTIVE) {
            stage.setTerminalReason(decision.stageState().name());
            stage.setClosedAt(now);
        }
        if (bpmApprovalStageDao.updateById(stage) != 1) {
            throw new IllegalStateException("审批阶段版本已变更，请刷新后重试");
        }
        recordAction(task, actor, action, commentText, now);

        if (decision.engineEffect() == EngineEffect.CLOSE_ONCE) {
            updateClosedInstance(instance, decision.stageState(), now);
        }
        if (decision.engineEffect() == EngineEffect.COMPLETE_ONCE
                || decision.engineEffect() == EngineEffect.CLOSE_ONCE) {
            applicationEventPublisher.publishEvent(new BpmApprovalStageEngineEffectRequestedEvent(
                    stage.getStageInvocationId(),
                    decision.engineEffect(),
                    decision.stageState().name()
            ));
        } else {
            bpmTaskProjectionService.projectActiveApprovalStageMembers(stage);
        }
        return ResponseDTO.ok();
    }

    private ApprovalStageFact toStageFact(BpmApprovalStageEntity stage) {
        return new ApprovalStageFact(
                stage.getStageInvocationId(),
                stage.getTenantId(),
                ApprovalStageState.valueOf(stage.getStageState())
        );
    }

    private ApprovalMemberFact toMemberFact(BpmApprovalStageMemberEntity member) {
        return new ApprovalMemberFact(
                String.valueOf(member.getApprovalStageMemberId()),
                member.getMemberOrder(),
                member.getSourceEmployeeId(),
                member.getCurrentEmployeeId(),
                ApprovalMemberState.valueOf(member.getMemberState())
        );
    }

    private ApprovalPolicyDocument parsePolicy(BpmApprovalStageEntity stage) {
        JSONObject payload = JSON.parseObject(stage.getApprovalPolicySnapshotJson());
        if (payload == null) {
            throw new IllegalStateException("审批阶段缺少冻结审批策略");
        }
        JSONArray rawAllowedActions = payload.getJSONArray("allowedActions");
        if (rawAllowedActions == null || rawAllowedActions.isEmpty()) {
            throw new IllegalStateException("审批阶段冻结审批策略缺少 allowedActions");
        }
        java.util.Set<String> allowedActions = new java.util.HashSet<>();
        for (Object rawAction : rawAllowedActions) {
            if (!(rawAction instanceof String action)) {
                throw new IllegalStateException("审批阶段冻结审批策略 allowedActions 非法");
            }
            allowedActions.add(action);
        }
        try {
            return new ApprovalPolicyDocument(
                    ApprovalCompletionMode.valueOf(payload.getString("completionMode")),
                    payload.getInteger("ratioPercent"),
                    payload.getString("rejectionRule"),
                    allowedActions
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException("审批阶段冻结审批策略非法", ex);
        }
    }

    private void updateMember(
            BpmApprovalStageMemberEntity member,
            ApprovalMemberState state,
            String action,
            LocalDateTime now
    ) {
        BpmApprovalStageMemberEntity update = new BpmApprovalStageMemberEntity();
        update.setApprovalStageMemberId(member.getApprovalStageMemberId());
        update.setMemberState(state.name());
        update.setStateChangedAt(now);
        update.setChangeReason(action);
        if (state == ApprovalMemberState.ACTIVE) {
            update.setActivatedAt(now);
        }
        if (state == ApprovalMemberState.APPROVED
                || state == ApprovalMemberState.REJECTED
                || state == ApprovalMemberState.RETURNED) {
            update.setActionResult(state.name());
            update.setCompletedAt(now);
        }
        if (state == ApprovalMemberState.TERMINATED
                || state == ApprovalMemberState.CANCELLED
                || state == ApprovalMemberState.INELIGIBLE) {
            update.setActionResult(state.name());
            update.setCancelledAt(now);
        }
        if (bpmApprovalStageMemberDao.updateById(update) != 1) {
            throw new IllegalStateException("审批成员状态更新失败");
        }
    }

    private void updateActedTask(BpmTaskEntity task, ApprovalMemberState state, LocalDateTime now) {
        BpmTaskEntity update = new BpmTaskEntity();
        update.setTaskId(task.getTaskId());
        if (state == ApprovalMemberState.TERMINATED || state == ApprovalMemberState.CANCELLED) {
            update.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
            update.setCancelledAt(now);
        } else {
            update.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
            update.setTaskResult(taskResult(state));
            update.setCompletedAt(now);
        }
        update.setLastActionAt(now);
        bpmTaskDao.updateById(update);
    }

    private void cancelMemberTask(Long approvalStageMemberId, LocalDateTime now) {
        BpmTaskEntity task = bpmTaskDao.selectByApprovalStageMemberId(approvalStageMemberId);
        if (task == null || !BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            return;
        }
        BpmTaskEntity update = new BpmTaskEntity();
        update.setTaskId(task.getTaskId());
        update.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
        update.setCancelledAt(now);
        update.setLastActionAt(now);
        bpmTaskDao.updateById(update);
    }

    private Integer taskResult(ApprovalMemberState state) {
        return switch (state) {
            case APPROVED -> BpmTaskResultEnum.APPROVED.getValue();
            case REJECTED -> BpmTaskResultEnum.REJECTED.getValue();
            case RETURNED -> BpmTaskResultEnum.RETURNED.getValue();
            default -> null;
        };
    }

    private void updateClosedInstance(BpmInstanceEntity instance, ApprovalStageState state, LocalDateTime now) {
        BpmInstanceEntity update = new BpmInstanceEntity();
        update.setInstanceId(instance.getInstanceId());
        update.setActiveTaskCount(0);
        update.setCurrentNodeSummaryJson(null);
        update.setLastActionAt(now);
        if (state == ApprovalStageState.RETURNED) {
            update.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        } else if (state == ApprovalStageState.REJECTED) {
            update.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
            update.setResultState(BpmInstanceResultStateEnum.REJECTED.getValue());
            update.setFinishedAt(now);
        }
        bpmInstanceDao.updateById(update);
    }

    private void recordAction(
            BpmTaskEntity task,
            BpmEmployeeSnapshot actor,
            String action,
            String commentText,
            LocalDateTime now
    ) {
        BpmTaskActionLogEntity log = new BpmTaskActionLogEntity();
        log.setInstanceId(task.getInstanceId());
        log.setTaskId(task.getTaskId());
        log.setDefinitionId(task.getDefinitionId());
        log.setDefinitionNodeId(task.getDefinitionNodeId());
        log.setEngineTaskId(task.getEngineTaskId());
        log.setActionType("M2_" + action);
        log.setActorEmployeeId(actor.employeeId());
        log.setActorNameSnapshot(actor.actualName());
        log.setFromAssigneeEmployeeId(task.getAssigneeEmployeeId());
        log.setToAssigneeEmployeeId(task.getAssigneeEmployeeId());
        log.setCommentText(commentText);
        log.setActionPayloadJson(JSON.toJSONString(Map.of(
                "approvalStageId", task.getApprovalStageId(),
                "approvalStageMemberId", task.getApprovalStageMemberId()
        )));
        log.setActionAt(now);
        bpmTaskActionLogDao.insert(log);
    }
}
