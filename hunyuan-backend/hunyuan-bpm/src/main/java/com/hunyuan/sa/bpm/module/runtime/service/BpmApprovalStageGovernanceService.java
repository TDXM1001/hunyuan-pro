package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BpmApprovalStageGovernanceService {

    @Resource private BpmTaskDao bpmTaskDao;
    @Resource private BpmApprovalStageDao bpmApprovalStageDao;
    @Resource private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;
    @Resource private BpmTaskActionLogDao bpmTaskActionLogDao;
    @Resource private BpmCurrentActorProvider bpmCurrentActorProvider;
    @Resource private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> transfer(Long taskId, Long targetEmployeeId, String reason) {
        String normalizedReason = reason == null ? null : reason.trim();
        if (targetEmployeeId == null || normalizedReason == null || normalizedReason.isEmpty()
                || normalizedReason.length() > 512) {
            return ResponseDTO.userErrorParam("M2 成员转办目标和原因不能为空，原因最多 512 个字符");
        }
        BpmTaskEntity task = bpmTaskDao.selectByIdForUpdate(taskId);
        if (task == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        boolean pendingOrRecoverable = BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())
                || BpmTaskStateEnum.CANCELLED.equalsValue(task.getTaskState());
        if (task.getApprovalStageId() == null || task.getApprovalStageMemberId() == null || !pendingOrRecoverable) {
            return ResponseDTO.userErrorParam("当前任务不是可转办的 M2 待办");
        }
        BpmApprovalStageEntity stage = bpmApprovalStageDao.selectByIdForUpdate(task.getApprovalStageId());
        if (stage == null || !task.getInstanceId().equals(stage.getInstanceId())
                || !("ACTIVE".equals(stage.getStageState()) || "EXCEPTION_PENDING".equals(stage.getStageState()))) {
            return ResponseDTO.userErrorParam("审批阶段不是可治理的活动状态");
        }
        List<BpmApprovalStageMemberEntity> members = bpmApprovalStageMemberDao
                .selectByApprovalStageIdForUpdate(stage.getApprovalStageId());
        BpmApprovalStageMemberEntity member = members.stream()
                .filter(item -> task.getApprovalStageMemberId().equals(item.getApprovalStageMemberId()))
                .findFirst().orElse(null);
        if (member == null || !Set.of("PLANNED", "ACTIVE", "INELIGIBLE").contains(member.getMemberState())) {
            return ResponseDTO.userErrorParam("审批成员不是可治理状态");
        }
        Long fromEmployeeId = member.getCurrentEmployeeId();
        if (targetEmployeeId.equals(fromEmployeeId)) {
            return ResponseDTO.userErrorParam("目标处理人不能与当前处理人相同");
        }
        Long actorEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actor = bpmOrgIdentityGateway.requireEmployee(actorEmployeeId);
        BpmEmployeeSnapshot target = bpmOrgIdentityGateway.requireEmployee(targetEmployeeId);
        LocalDateTime now = LocalDateTime.now();

        BpmApprovalStageMemberEntity memberUpdate = new BpmApprovalStageMemberEntity();
        memberUpdate.setApprovalStageMemberId(member.getApprovalStageMemberId());
        memberUpdate.setCurrentEmployeeId(targetEmployeeId);
        memberUpdate.setMemberState("INELIGIBLE".equals(member.getMemberState()) ? "ACTIVE" : member.getMemberState());
        memberUpdate.setStateChangedAt(now);
        memberUpdate.setChangeReason(normalizedReason);
        if (bpmApprovalStageMemberDao.updateById(memberUpdate) != 1) {
            throw new IllegalStateException("M2 审批成员转办更新失败");
        }

        BpmTaskEntity taskUpdate = new BpmTaskEntity();
        taskUpdate.setTaskId(taskId);
        taskUpdate.setAssigneeEmployeeId(targetEmployeeId);
        taskUpdate.setAssigneeNameSnapshot(target.actualName());
        taskUpdate.setAssigneeDepartmentIdSnapshot(target.departmentId());
        taskUpdate.setAssigneeDepartmentNameSnapshot(target.departmentName());
        JSONObject assignment = new JSONObject();
        assignment.put("assigneeEmployeeId", targetEmployeeId);
        assignment.put("governanceReason", normalizedReason);
        taskUpdate.setRuntimeAssignmentSnapshotJson(assignment.toJSONString());
        taskUpdate.setAssignedAt(now);
        taskUpdate.setLastActionAt(now);
        int taskAffected = "INELIGIBLE".equals(member.getMemberState())
                ? bpmTaskDao.restoreM2Task(
                        taskId, targetEmployeeId, target.actualName(), target.departmentId(),
                        target.departmentName(), assignment.toJSONString()
                )
                : bpmTaskDao.updateById(taskUpdate);
        if (taskAffected != 1) {
            throw new IllegalStateException("M2 审批任务转办投影更新失败");
        }
        if ("INELIGIBLE".equals(member.getMemberState())) {
            bpmApprovalStageDao.restoreFromMemberEligibilityException(stage.getApprovalStageId());
        }

        BpmTaskActionLogEntity log = new BpmTaskActionLogEntity();
        log.setInstanceId(task.getInstanceId());
        log.setTaskId(taskId);
        log.setDefinitionId(task.getDefinitionId());
        log.setGraphDefinitionVersionId(task.getGraphDefinitionVersionId());
        log.setDefinitionSource(task.getDefinitionSource());
        log.setDefinitionNodeId(task.getDefinitionNodeId());
        log.setEngineTaskId(task.getEngineTaskId());
        log.setActionType("M2_MEMBER_TRANSFERRED");
        log.setActorEmployeeId(actorEmployeeId);
        log.setActorNameSnapshot(actor.actualName());
        log.setFromAssigneeEmployeeId(fromEmployeeId);
        log.setToAssigneeEmployeeId(targetEmployeeId);
        log.setCommentText(normalizedReason);
        log.setActionAt(now);
        bpmTaskActionLogDao.insert(log);
        return ResponseDTO.ok();
    }
}
