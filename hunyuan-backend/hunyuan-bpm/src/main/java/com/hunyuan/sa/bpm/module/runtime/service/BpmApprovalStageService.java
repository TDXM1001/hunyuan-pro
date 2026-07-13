package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateAutomaticOutcome;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 审批阶段及其冻结成员事实的创建服务。
 *
 * 不在这里创建任务或推进流程引擎；后续投影只能消费本服务已冻结的成员状态。
 */
@Service
public class BpmApprovalStageService {

    private static final String ENGINE_EFFECT_PENDING = "PENDING";

    @Resource
    private BpmApprovalStageDao bpmApprovalStageDao;

    @Resource
    private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;

    @Transactional(rollbackFor = Exception.class)
    public BpmApprovalStageEntity open(OpenApprovalStageCommand command) {
        validate(command);

        BpmApprovalStageEntity invocationOwner =
                bpmApprovalStageDao.selectByStageInvocationId(command.stageInvocationId());
        if (invocationOwner != null) {
            verifyInvocationOwner(invocationOwner, command);
            return invocationOwner;
        }

        BpmApprovalStageEntity existing = bpmApprovalStageDao.selectByInstanceIdAndAuthoredNodeIdAndGeneration(
                command.instanceId(), command.authoredNodeId(), command.generation()
        );
        if (existing != null) {
            verifyExistingInvocation(existing, command.stageInvocationId());
            return existing;
        }

        List<ResolvedCandidateMember> members = normalizeMembers(command.candidateSnapshot().members());
        String candidateSnapshotDigest = candidateSnapshotDigest(members);
        LocalDateTime openedAt = LocalDateTime.now();
        BpmApprovalStageEntity stage = buildStage(command, members, candidateSnapshotDigest, openedAt);
        try {
            int inserted = bpmApprovalStageDao.insert(stage);
            if (inserted != 1 || stage.getApprovalStageId() == null) {
                throw new IllegalStateException("审批阶段创建失败");
            }
        } catch (DuplicateKeyException ex) {
            return loadConcurrentStage(command, ex);
        }

        for (int index = 0; index < members.size(); index++) {
            bpmApprovalStageMemberDao.insert(buildMember(
                    stage,
                    members.get(index),
                    index + 1,
                    candidateSnapshotDigest,
                    openedAt
            ));
        }
        return stage;
    }

    private BpmApprovalStageEntity buildStage(
            OpenApprovalStageCommand command,
            List<ResolvedCandidateMember> members,
            String candidateSnapshotDigest,
            LocalDateTime openedAt
    ) {
        ApprovalPolicyDocument policy = command.approvalPolicy();
        CandidateAutomaticOutcome automaticOutcome = command.candidateSnapshot().automaticOutcome();
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setInstanceId(command.instanceId());
        stage.setTenantId(command.tenantId());
        stage.setDefinitionVersionId(command.definitionVersionId());
        stage.setAuthoredNodeId(command.authoredNodeId());
        stage.setGeneration(command.generation());
        stage.setStageInvocationId(command.stageInvocationId());
        stage.setEngineProcessInstanceId(command.engineProcessInstanceId());
        stage.setEngineExecutionId(command.engineExecutionId());
        stage.setStageState(automaticStageState(automaticOutcome).name());
        stage.setTerminalReason(automaticOutcome == null ? null : automaticOutcome.name());
        stage.setEngineEffectState(ENGINE_EFFECT_PENDING);
        stage.setCompletionMode(policy.completionMode().name());
        stage.setRatioPercent(policy.ratioPercent());
        stage.setRejectionRule(policy.rejectionRule());
        stage.setEffectiveMemberCount(members.size());
        stage.setRequiredApprovalCount(automaticOutcome == null ? requiredApprovalCount(policy, members.size()) : 0);
        stage.setCandidatePolicyVersionId(command.candidatePolicyVersionId());
        stage.setCandidatePolicyDigest(command.candidatePolicyDigest());
        stage.setApprovalPolicyVersionId(command.approvalPolicyVersionId());
        stage.setApprovalPolicyDigest(command.approvalPolicyDigest());
        stage.setApprovalPolicySnapshotJson(JSON.toJSONString(policy));
        stage.setCandidateSnapshotJson(JSON.toJSONString(members));
        stage.setCandidateSnapshotDigest(candidateSnapshotDigest);
        stage.setDiagnosticsJson(JSON.toJSONString(command.candidateSnapshot().diagnostics()));
        stage.setOpenedAt(openedAt);
        stage.setClosedAt(automaticOutcome == null ? null : openedAt);
        stage.setRevision(0);
        return stage;
    }

    private ApprovalStageState automaticStageState(CandidateAutomaticOutcome outcome) {
        if (outcome == null) {
            return ApprovalStageState.ACTIVE;
        }
        return outcome == CandidateAutomaticOutcome.AUTO_APPROVE
                ? ApprovalStageState.APPROVED
                : ApprovalStageState.REJECTED;
    }

    private BpmApprovalStageMemberEntity buildMember(
            BpmApprovalStageEntity stage,
            ResolvedCandidateMember candidate,
            int memberOrder,
            String candidateSnapshotDigest,
            LocalDateTime openedAt
    ) {
        boolean active = stage.getCompletionMode().equals(ApprovalCompletionMode.SEQUENTIAL.name())
                ? memberOrder == 1
                : true;
        BpmApprovalStageMemberEntity member = new BpmApprovalStageMemberEntity();
        member.setApprovalStageId(stage.getApprovalStageId());
        member.setMemberOrder(memberOrder);
        member.setSourceEmployeeId(candidate.sourceEmployeeId());
        member.setCurrentEmployeeId(candidate.employeeId());
        member.setMemberState((active ? ApprovalMemberState.ACTIVE : ApprovalMemberState.PLANNED).name());
        member.setCandidateSnapshotDigest(candidateSnapshotDigest);
        member.setMemberSnapshotJson(JSON.toJSONString(candidate));
        member.setActivatedAt(active ? openedAt : null);
        member.setStateChangedAt(openedAt);
        return member;
    }

    private BpmApprovalStageEntity loadConcurrentStage(OpenApprovalStageCommand command, DuplicateKeyException cause) {
        BpmApprovalStageEntity existing = bpmApprovalStageDao.selectByStageInvocationId(command.stageInvocationId());
        if (existing == null) {
            existing = bpmApprovalStageDao.selectByInstanceIdAndAuthoredNodeIdAndGeneration(
                    command.instanceId(), command.authoredNodeId(), command.generation()
            );
        }
        if (existing == null) {
            throw cause;
        }
        verifyInvocationOwner(existing, command);
        return existing;
    }

    private void verifyInvocationOwner(BpmApprovalStageEntity stage, OpenApprovalStageCommand command) {
        if (!Objects.equals(stage.getInstanceId(), command.instanceId())
                || !Objects.equals(stage.getAuthoredNodeId(), command.authoredNodeId())
                || !Objects.equals(stage.getGeneration(), command.generation())
                || !Objects.equals(stage.getEngineProcessInstanceId(), command.engineProcessInstanceId())
                || !Objects.equals(stage.getEngineExecutionId(), command.engineExecutionId())) {
            throw new IllegalArgumentException("stageInvocationId 已被其他审批阶段占用");
        }
    }

    private void verifyExistingInvocation(BpmApprovalStageEntity stage, String stageInvocationId) {
        if (!Objects.equals(stage.getStageInvocationId(), stageInvocationId)) {
            throw new IllegalArgumentException("审批阶段已存在且 stageInvocationId 不一致");
        }
    }

    private List<ResolvedCandidateMember> normalizeMembers(List<ResolvedCandidateMember> sourceMembers) {
        List<ResolvedCandidateMember> members = new ArrayList<>(sourceMembers);
        members.sort(Comparator.comparing(ResolvedCandidateMember::sourceEmployeeId));
        Long previousSourceEmployeeId = null;
        for (ResolvedCandidateMember member : members) {
            if (member.sourceEmployeeId() == null || member.sourceEmployeeId() <= 0
                    || member.employeeId() == null || member.employeeId() <= 0) {
                throw new IllegalArgumentException("冻结审批成员必须包含有效来源员工和当前处理人");
            }
            if (Objects.equals(previousSourceEmployeeId, member.sourceEmployeeId())) {
                throw new IllegalArgumentException("冻结审批成员存在重复来源员工");
            }
            previousSourceEmployeeId = member.sourceEmployeeId();
        }
        return members;
    }

    private int requiredApprovalCount(ApprovalPolicyDocument policy, int effectiveMemberCount) {
        return switch (policy.completionMode()) {
            case SINGLE, ANY -> 1;
            case SEQUENTIAL, ALL -> effectiveMemberCount;
            case RATIO -> (effectiveMemberCount * policy.ratioPercent() + 99) / 100;
        };
    }

    private String candidateSnapshotDigest(List<ResolvedCandidateMember> members) {
        StringBuilder canonical = new StringBuilder();
        for (ResolvedCandidateMember member : members) {
            appendCanonical(canonical, member.sourceEmployeeId());
            appendCanonical(canonical, member.employeeId());
            appendCanonical(canonical, member.displayName());
            appendCanonical(canonical, member.departmentId());
            appendCanonical(canonical, member.departmentName());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("运行环境缺少 SHA-256", ex);
        }
    }

    private void appendCanonical(StringBuilder target, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        target.append(text.length()).append(':').append(text).append(';');
    }

    private void validate(OpenApprovalStageCommand command) {
        if (command == null
                || command.instanceId() == null || command.instanceId() <= 0
                || command.tenantId() == null || command.tenantId() <= 0
                || command.definitionVersionId() == null || command.definitionVersionId() <= 0
                || command.authoredNodeId() == null || command.authoredNodeId().isBlank()
                || command.generation() == null || command.generation() < 0
                || command.stageInvocationId() == null || command.stageInvocationId().isBlank()
                || isBlank(command.engineProcessInstanceId())
                || isBlank(command.engineExecutionId())
                || command.candidatePolicyVersionId() == null || command.candidatePolicyVersionId() <= 0
                || isBlank(command.candidatePolicyDigest())
                || command.approvalPolicyVersionId() == null || command.approvalPolicyVersionId() <= 0
                || isBlank(command.approvalPolicyDigest())
                || command.approvalPolicy() == null
                || command.candidateSnapshot() == null
                || command.candidateSnapshot().members() == null) {
            throw new IllegalArgumentException("审批阶段冻结参数不完整");
        }
        boolean automatic = command.candidateSnapshot().automaticOutcome() != null;
        if (automatic == command.candidateSnapshot().members().isEmpty()) {
            // 自动终态必须无成员；普通阶段必须至少冻结一个成员。
        } else {
            throw new IllegalArgumentException("审批阶段候选成员与自动终态不一致");
        }
        if (command.approvalPolicy().completionMode() == ApprovalCompletionMode.SINGLE
                && !automatic
                && command.candidateSnapshot().members().size() != 1) {
            throw new IllegalArgumentException("SINGLE 审批阶段必须冻结一个成员");
        }
    }

    /**
     * 先以数据库状态领取一次引擎副作用，再由控制器触发 receive task。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public EngineEffectClaim claimEngineEffect(String stageInvocationId, String terminalReason) {
        if (isBlank(stageInvocationId) || isBlank(terminalReason)) {
            throw new IllegalArgumentException("审批阶段引擎推进参数不完整");
        }
        BpmApprovalStageEntity stage = bpmApprovalStageDao.selectByStageInvocationId(stageInvocationId);
        if (stage == null) {
            throw new IllegalArgumentException("审批阶段不存在：" + stageInvocationId);
        }
        if (!ENGINE_EFFECT_PENDING.equals(stage.getEngineEffectState())) {
            return null;
        }
        int claimed = bpmApprovalStageDao.claimEngineEffect(stage.getApprovalStageId(), terminalReason);
        if (claimed != 1) {
            return null;
        }
        return new EngineEffectClaim(
                stage.getApprovalStageId(),
                stage.getInstanceId(),
                stage.getStageInvocationId(),
                stage.getEngineProcessInstanceId(),
                stage.getEngineExecutionId(),
                terminalReason
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markEngineEffectCompleted(Long approvalStageId) {
        if (approvalStageId == null || bpmApprovalStageDao.markEngineEffectCompleted(approvalStageId) != 1) {
            throw new IllegalStateException("审批阶段引擎推进完成状态更新失败");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markEngineEffectFailed(Long approvalStageId, String error) {
        if (approvalStageId == null || bpmApprovalStageDao.markEngineEffectFailed(
                approvalStageId,
                limitError(error)
        ) != 1) {
            throw new IllegalStateException("审批阶段引擎推进失败状态更新失败");
        }
    }

    private String limitError(String error) {
        if (error == null) {
            return "未知引擎错误";
        }
        return error.length() <= 512 ? error : error.substring(0, 512);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record OpenApprovalStageCommand(
            Long instanceId,
            Long tenantId,
            Long definitionVersionId,
            String authoredNodeId,
            Integer generation,
            String stageInvocationId,
            Long candidatePolicyVersionId,
            String candidatePolicyDigest,
            Long approvalPolicyVersionId,
            String approvalPolicyDigest,
            ApprovalPolicyDocument approvalPolicy,
            ResolvedCandidateSnapshot candidateSnapshot,
            String engineProcessInstanceId,
            String engineExecutionId
    ) {
    }

    public record EngineEffectClaim(
            Long approvalStageId,
            Long instanceId,
            String stageInvocationId,
            String engineProcessInstanceId,
            String engineExecutionId,
            String terminalReason
    ) {
    }
}
