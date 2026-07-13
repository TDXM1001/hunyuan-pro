package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.candidate.domain.model.*;
import com.hunyuan.sa.bpm.module.candidate.service.ApprovalCompletionService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BpmApprovalStageEligibilityService {

    @Resource private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;
    @Resource private BpmApprovalStageDao bpmApprovalStageDao;
    @Resource private BpmTaskDao bpmTaskDao;
    @Resource private BpmTaskActionLogDao bpmTaskActionLogDao;
    @Resource private BpmOrgIdentityGateway bpmOrgIdentityGateway;
    @Resource private ApprovalCompletionService approvalCompletionService;
    @Resource private ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(fixedDelayString = "${hunyuan.bpm.m2.eligibility-scan-delay-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void scanOpenMembers() {
        for (Long memberId : bpmApprovalStageMemberDao.selectOpenIdsForEligibilityScan(200)) {
            reconcile(memberId);
        }
    }

    public boolean reconcile(Long memberId) {
        BpmApprovalStageMemberEntity member = bpmApprovalStageMemberDao.selectByIdForUpdate(memberId);
        if (member == null || !("PLANNED".equals(member.getMemberState()) || "ACTIVE".equals(member.getMemberState()))) {
            return false;
        }
        try {
            bpmOrgIdentityGateway.requireEmployee(member.getCurrentEmployeeId());
            return false;
        } catch (RuntimeException invalidEmployee) {
            BpmApprovalStageEntity stage = bpmApprovalStageDao.selectByIdForUpdate(member.getApprovalStageId());
            if (stage == null || !"ACTIVE".equals(stage.getStageState())) {
                return false;
            }
            List<BpmApprovalStageMemberEntity> members = bpmApprovalStageMemberDao
                    .selectByApprovalStageIdForUpdate(stage.getApprovalStageId());
            ApprovalCompletionDecision decision = approvalCompletionService.decide(
                    parsePolicy(stage),
                    new ApprovalStageFact(stage.getStageInvocationId(), stage.getTenantId(), ApprovalStageState.ACTIVE),
                    members.stream().map(this::toFact).toList(),
                    new ApprovalMemberAction(String.valueOf(memberId), "MARK_INELIGIBLE")
            );
            markIneligible(stage, members, decision, member, invalidEmployee.getMessage());
            return true;
        }
    }

    private void markIneligible(
            BpmApprovalStageEntity stage,
            List<BpmApprovalStageMemberEntity> members,
            ApprovalCompletionDecision decision,
            BpmApprovalStageMemberEntity member,
            String detail
    ) {
        LocalDateTime now = LocalDateTime.now();
        String reason = "当前处理人失效" + (detail == null ? "" : "：" + detail);
        for (MemberUpdate memberDecision : decision.memberUpdates()) {
            BpmApprovalStageMemberEntity update = new BpmApprovalStageMemberEntity();
            update.setApprovalStageMemberId(Long.valueOf(memberDecision.memberId()));
            update.setMemberState(memberDecision.state().name());
            update.setStateChangedAt(now);
            update.setChangeReason(reason);
            if (bpmApprovalStageMemberDao.updateById(update) != 1) {
                throw new IllegalStateException("M2 成员失效状态更新失败");
            }
        }
        if (decision.stageState() == ApprovalStageState.EXCEPTION_PENDING) {
            bpmApprovalStageDao.markMemberEligibilityException(stage.getApprovalStageId(), "MEMBER_INELIGIBLE");
        } else if (decision.stageState() == ApprovalStageState.REJECTED) {
            int affected = bpmApprovalStageDao.updateState(
                    stage.getApprovalStageId(), stage.getRevision(), "REJECTED",
                    "MEMBER_INELIGIBLE_UNREACHABLE", now
            );
            if (affected != 1) {
                throw new IllegalStateException("M2 成员失效终态 CAS 更新失败");
            }
            applicationEventPublisher.publishEvent(new BpmApprovalStageEngineEffectRequestedEvent(
                    stage.getStageInvocationId(), EngineEffect.CLOSE_ONCE, "REJECTED"
            ));
        }
        BpmTaskEntity task = bpmTaskDao.selectByApprovalStageMemberIdForUpdate(member.getApprovalStageMemberId());
        closeAffectedTasks(decision, now);
        if (task != null && BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            BpmTaskActionLogEntity log = new BpmTaskActionLogEntity();
            log.setInstanceId(task.getInstanceId());
            log.setTaskId(task.getTaskId());
            log.setGraphDefinitionVersionId(task.getGraphDefinitionVersionId());
            log.setDefinitionSource(task.getDefinitionSource());
            log.setActionType("M2_MEMBER_INELIGIBLE");
            log.setActorEmployeeId(0L);
            log.setActorNameSnapshot("SYSTEM");
            log.setFromAssigneeEmployeeId(member.getCurrentEmployeeId());
            log.setCommentText(reason);
            log.setActionAt(now);
            bpmTaskActionLogDao.insert(log);
        }
    }

    private void closeAffectedTasks(ApprovalCompletionDecision decision, LocalDateTime now) {
        for (MemberUpdate update : decision.memberUpdates()) {
            if (!(update.state() == ApprovalMemberState.INELIGIBLE || update.state() == ApprovalMemberState.TERMINATED)) {
                continue;
            }
            BpmTaskEntity task = bpmTaskDao.selectByApprovalStageMemberIdForUpdate(Long.valueOf(update.memberId()));
            if (task == null || !BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
                continue;
            }
            BpmTaskEntity taskUpdate = new BpmTaskEntity();
            taskUpdate.setTaskId(task.getTaskId());
            taskUpdate.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
            taskUpdate.setCancelledAt(now);
            taskUpdate.setLastActionAt(now);
            bpmTaskDao.updateById(taskUpdate);
        }
    }

    private ApprovalPolicyDocument parsePolicy(BpmApprovalStageEntity stage) {
        JSONObject policy = JSON.parseObject(stage.getApprovalPolicySnapshotJson());
        return new ApprovalPolicyDocument(
                ApprovalCompletionMode.valueOf(stage.getCompletionMode()),
                stage.getRatioPercent(),
                stage.getRejectionRule(),
                Set.copyOf(policy.getJSONArray("allowedActions").toJavaList(String.class))
        );
    }

    private ApprovalMemberFact toFact(BpmApprovalStageMemberEntity member) {
        return new ApprovalMemberFact(
                String.valueOf(member.getApprovalStageMemberId()), member.getMemberOrder(),
                member.getSourceEmployeeId(), member.getCurrentEmployeeId(),
                ApprovalMemberState.valueOf(member.getMemberState())
        );
    }
}
